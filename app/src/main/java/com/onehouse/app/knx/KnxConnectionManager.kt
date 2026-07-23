package com.onehouse.app.knx

import android.os.Handler
import android.os.Looper
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Motor reutilizable de conexión KNXnet/IP Tunnelling.
 *
 * En v1.4.2 gestiona el ciclo de vida del túnel (connect/disconnect) y deja
 * definida la API que utilizará v1.5.0 para telegramas y lecturas de grupo.
 * Los métodos de bus devuelven [OperationResult.NotAvailable] hasta que se
 * incorpore la codificación cEMI/DPT en la siguiente versión.
 */
class KnxConnectionManager(
    private val timeoutMillis: Int = KnxProtocol.DEFAULT_TIMEOUT_MILLIS
) : Closeable {

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    sealed interface ConnectResult {
        data class Success(
            val deviceAddress: String,
            val channelId: Int
        ) : ConnectResult

        data object Timeout : ConnectResult
        data class Rejected(val status: Int) : ConnectResult
        data class NetworkError(val detail: String) : ConnectResult
        data object InvalidResponse : ConnectResult
        data object Cancelled : ConnectResult
    }

    sealed interface OperationResult {
        data object Success : OperationResult
        data class Failure(val detail: String) : OperationResult
        data class NotAvailable(val detail: String) : OperationResult
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var worker: Thread? = null
    private var socket: DatagramSocket? = null
    private var channelId: Int? = null
    private var localAddress: Inet4Address? = null
    private var endpoint: KnxEndpoint? = null
    private var cancelled = AtomicBoolean(false)

    @Volatile
    var state: State = State.DISCONNECTED
        private set

    val isConnected: Boolean
        get() = state == State.CONNECTED && socket?.isClosed == false && channelId != null

    fun connect(
        endpoint: KnxEndpoint,
        onResult: (ConnectResult) -> Unit
    ): Closeable {
        require(endpoint.port in 1..65535) { "Puerto KNX/IP fuera de rango" }
        require(timeoutMillis > 0) { "El tiempo de espera debe ser mayor que cero" }

        disconnectInternal(sendRequest = true)
        cancelled = AtomicBoolean(false)
        state = State.CONNECTING

        val currentCancellation = cancelled
        val thread = Thread {
            val result = openTunnel(endpoint, currentCancellation)

            if (!currentCancellation.get()) {
                mainHandler.post {
                    if (!currentCancellation.get()) onResult(result)
                }
            }
        }.apply {
            name = "KnxConnectionManager"
            isDaemon = true
            start()
        }

        synchronized(lock) {
            worker = thread
        }

        return Closeable {
            currentCancellation.set(true)
            synchronized(lock) {
                socket?.close()
                worker?.interrupt()
            }
            if (state == State.CONNECTING) state = State.DISCONNECTED
        }
    }

    fun disconnect() {
        disconnectInternal(sendRequest = true)
    }

    /**
     * Punto de entrada reservado para v1.5.0.
     */
    fun sendTelegram(
        groupAddress: String,
        payload: ByteArray,
        onResult: (OperationResult) -> Unit
    ) {
        val result = if (!isConnected) {
            OperationResult.Failure("No existe un túnel KNX/IP conectado")
        } else {
            OperationResult.NotAvailable(
                "El envío cEMI/DPT se habilitará en OneHouse v1.5.0"
            )
        }
        mainHandler.post { onResult(result) }
    }

    /**
     * Punto de entrada reservado para v1.5.0.
     */
    fun readGroupValue(
        groupAddress: String,
        onResult: (OperationResult) -> Unit
    ) {
        val result = if (!isConnected) {
            OperationResult.Failure("No existe un túnel KNX/IP conectado")
        } else {
            OperationResult.NotAvailable(
                "La lectura de direcciones de grupo se habilitará en OneHouse v1.5.0"
            )
        }
        mainHandler.post { onResult(result) }
    }

    override fun close() {
        disconnectInternal(sendRequest = true)
    }

    private fun openTunnel(
        target: KnxEndpoint,
        cancellation: AtomicBoolean
    ): ConnectResult {
        return try {
            val targetAddress = InetAddress.getByName(target.host)
            if (targetAddress !is Inet4Address) {
                state = State.DISCONNECTED
                ConnectResult.NetworkError("La dirección debe ser IPv4")
            } else {
                val udpSocket = DatagramSocket()
                synchronized(lock) {
                    socket = udpSocket
                    endpoint = target
                }

                udpSocket.soTimeout = timeoutMillis
                udpSocket.connect(InetSocketAddress(targetAddress, target.port))

                val ownAddress = udpSocket.localAddress as? Inet4Address
                    ?: run {
                        udpSocket.close()
                        state = State.DISCONNECTED
                        return ConnectResult.NetworkError(
                            "No se pudo obtener la dirección IPv4 local"
                        )
                    }

                localAddress = ownAddress
                val request = KnxProtocol.buildConnectRequest(ownAddress, udpSocket.localPort)
                udpSocket.send(DatagramPacket(request, request.size))

                val responseBuffer = ByteArray(KnxProtocol.MAX_PACKET_SIZE)
                val response = DatagramPacket(responseBuffer, responseBuffer.size)
                udpSocket.receive(response)

                when (val parsed = KnxProtocol.parseConnectResponse(response.data, response.length)) {
                    is KnxProtocol.ConnectResponse.Accepted -> {
                        if (cancellation.get()) {
                            udpSocket.close()
                            state = State.DISCONNECTED
                            ConnectResult.Cancelled
                        } else {
                            channelId = parsed.channelId
                            state = State.CONNECTED
                            ConnectResult.Success(
                                deviceAddress = response.address.hostAddress ?: target.host,
                                channelId = parsed.channelId
                            )
                        }
                    }

                    is KnxProtocol.ConnectResponse.Rejected -> {
                        udpSocket.close()
                        clearSession()
                        ConnectResult.Rejected(parsed.status)
                    }

                    KnxProtocol.ConnectResponse.Invalid -> {
                        udpSocket.close()
                        clearSession()
                        ConnectResult.InvalidResponse
                    }
                }
            }
        } catch (_: SocketTimeoutException) {
            synchronized(lock) { socket?.close() }
            clearSession()
            ConnectResult.Timeout
        } catch (error: Exception) {
            synchronized(lock) { socket?.close() }
            clearSession()
            if (cancellation.get()) {
                ConnectResult.Cancelled
            } else {
                ConnectResult.NetworkError(
                    error.localizedMessage ?: error.javaClass.simpleName
                )
            }
        }
    }

    private fun disconnectInternal(sendRequest: Boolean) {
        val udpSocket: DatagramSocket?
        val currentChannel: Int?
        val ownAddress: Inet4Address?

        synchronized(lock) {
            cancelled.set(true)
            worker?.interrupt()
            worker = null
            udpSocket = socket
            currentChannel = channelId
            ownAddress = localAddress
            state = if (udpSocket != null && !udpSocket.isClosed) {
                State.DISCONNECTING
            } else {
                State.DISCONNECTED
            }
        }

        if (
            sendRequest &&
            udpSocket != null &&
            !udpSocket.isClosed &&
            currentChannel != null &&
            ownAddress != null
        ) {
            runCatching {
                val packet = KnxProtocol.buildDisconnectRequest(
                    channelId = currentChannel,
                    localAddress = ownAddress,
                    localPort = udpSocket.localPort
                )
                udpSocket.send(DatagramPacket(packet, packet.size))
            }
        }

        udpSocket?.close()
        clearSession()
    }

    private fun clearSession() {
        synchronized(lock) {
            socket = null
            channelId = null
            localAddress = null
            endpoint = null
            worker = null
            state = State.DISCONNECTED
        }
    }
}

/**
 * Parámetros comunes del transporte KNXnet/IP.
 */
internal object KnxProtocol {
    const val DEFAULT_TIMEOUT_MILLIS = 4_000
    const val MAX_PACKET_SIZE = 512

    private const val CONNECT_RESPONSE = 0x0206
    private const val DISCONNECT_REQUEST = 0x0209

    fun buildConnectRequest(localAddress: Inet4Address, localPort: Int): ByteArray {
        val hpai = buildHpai(localAddress, localPort)
        return byteArrayOf(
            0x06, 0x10,
            0x02, 0x05,
            0x00, 0x1A,
            *hpai,
            *hpai,
            0x04, 0x04,
            0x02, 0x00
        )
    }

    fun buildDisconnectRequest(
        channelId: Int,
        localAddress: Inet4Address,
        localPort: Int
    ): ByteArray = byteArrayOf(
        0x06, 0x10,
        ((DISCONNECT_REQUEST ushr 8) and 0xFF).toByte(),
        (DISCONNECT_REQUEST and 0xFF).toByte(),
        0x00, 0x10,
        (channelId and 0xFF).toByte(),
        0x00,
        *buildHpai(localAddress, localPort)
    )

    fun parseConnectResponse(data: ByteArray, length: Int): ConnectResponse {
        if (length < 8) return ConnectResponse.Invalid

        val headerLength = data[0].toInt() and 0xFF
        val protocolVersion = data[1].toInt() and 0xFF
        val serviceType = ((data[2].toInt() and 0xFF) shl 8) or
            (data[3].toInt() and 0xFF)
        val totalLength = ((data[4].toInt() and 0xFF) shl 8) or
            (data[5].toInt() and 0xFF)

        if (
            headerLength != 6 ||
            protocolVersion != 0x10 ||
            serviceType != CONNECT_RESPONSE ||
            totalLength !in 8..length
        ) {
            return ConnectResponse.Invalid
        }

        val channelId = data[6].toInt() and 0xFF
        val status = data[7].toInt() and 0xFF
        return if (status == 0) {
            ConnectResponse.Accepted(channelId)
        } else {
            ConnectResponse.Rejected(status)
        }
    }

    private fun buildHpai(localAddress: Inet4Address, localPort: Int): ByteArray {
        val ip = localAddress.address
        return byteArrayOf(
            0x08, 0x01,
            ip[0], ip[1], ip[2], ip[3],
            ((localPort ushr 8) and 0xFF).toByte(),
            (localPort and 0xFF).toByte()
        )
    }

    sealed interface ConnectResponse {
        data class Accepted(val channelId: Int) : ConnectResponse
        data class Rejected(val status: Int) : ConnectResponse
        data object Invalid : ConnectResponse
    }
}
