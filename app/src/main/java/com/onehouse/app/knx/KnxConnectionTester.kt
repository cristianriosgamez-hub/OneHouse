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
 * Punto de conexión KNXnet/IP reutilizable por las futuras órdenes del bus.
 */
data class KnxEndpoint(
    val host: String,
    val port: Int
)

/**
 * Comprueba que un interfaz KNXnet/IP permite abrir un túnel real.
 *
 * La prueba envía un Connect Request de tipo Tunnelling y, cuando recibe una
 * respuesta correcta, cierra inmediatamente el canal mediante Disconnect
 * Request. No transmite telegramas al bus KNX.
 */
object KnxConnectionTester {
    private const val DEFAULT_TIMEOUT_MILLIS = 4_000
    private const val KNXNET_IP_CONNECT_RESPONSE = 0x0206
    private const val KNXNET_IP_DISCONNECT_REQUEST = 0x0209

    sealed interface Result {
        data class Success(
            val deviceAddress: String,
            val channelId: Int
        ) : Result

        data object Timeout : Result
        data class Rejected(val status: Int) : Result
        data class NetworkError(val detail: String) : Result
        data object InvalidResponse : Result
    }

    fun test(
        endpoint: KnxEndpoint,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
        onResult: (Result) -> Unit
    ): Closeable {
        require(endpoint.port in 1..65535) { "Puerto KNX/IP fuera de rango" }
        require(timeoutMillis > 0) { "El tiempo de espera debe ser mayor que cero" }

        val cancelled = AtomicBoolean(false)
        var socket: DatagramSocket? = null
        val mainHandler = Handler(Looper.getMainLooper())

        val worker = Thread {
            val result = try {
                val targetAddress = InetAddress.getByName(endpoint.host)
                if (targetAddress !is Inet4Address) {
                    Result.NetworkError("La dirección debe ser IPv4")
                } else {
                    DatagramSocket().use { udpSocket ->
                        socket = udpSocket
                        udpSocket.soTimeout = timeoutMillis
                        udpSocket.connect(InetSocketAddress(targetAddress, endpoint.port))

                        val localAddress = udpSocket.localAddress as? Inet4Address
                            ?: return@use Result.NetworkError("No se pudo obtener la dirección IPv4 local")

                        val request = buildConnectRequest(localAddress, udpSocket.localPort)
                        udpSocket.send(DatagramPacket(request, request.size))

                        val responseBuffer = ByteArray(512)
                        val response = DatagramPacket(responseBuffer, responseBuffer.size)
                        udpSocket.receive(response)

                        when (val parsed = parseConnectResponse(response.data, response.length)) {
                            is ConnectResponse.Accepted -> {
                                runCatching {
                                    val disconnect = buildDisconnectRequest(
                                        channelId = parsed.channelId,
                                        localAddress = localAddress,
                                        localPort = udpSocket.localPort
                                    )
                                    udpSocket.send(DatagramPacket(disconnect, disconnect.size))
                                }
                                Result.Success(
                                    deviceAddress = response.address.hostAddress ?: endpoint.host,
                                    channelId = parsed.channelId
                                )
                            }

                            is ConnectResponse.Rejected -> Result.Rejected(parsed.status)
                            ConnectResponse.Invalid -> Result.InvalidResponse
                        }
                    }
                }
            } catch (_: SocketTimeoutException) {
                Result.Timeout
            } catch (error: Exception) {
                if (cancelled.get()) return@Thread
                Result.NetworkError(error.localizedMessage ?: error.javaClass.simpleName)
            }

            if (!cancelled.get()) {
                mainHandler.post {
                    if (!cancelled.get()) onResult(result)
                }
            }
        }.apply {
            name = "KnxConnectionTester"
            isDaemon = true
            start()
        }

        return Closeable {
            cancelled.set(true)
            socket?.close()
            worker.interrupt()
        }
    }

    fun test(
        host: String,
        port: Int,
        onResult: (Result) -> Unit
    ): Closeable = test(KnxEndpoint(host.trim(), port), onResult = onResult)

    private fun buildConnectRequest(localAddress: Inet4Address, localPort: Int): ByteArray {
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

    private fun buildDisconnectRequest(
        channelId: Int,
        localAddress: Inet4Address,
        localPort: Int
    ): ByteArray = byteArrayOf(
        0x06, 0x10,
        ((KNXNET_IP_DISCONNECT_REQUEST ushr 8) and 0xFF).toByte(),
        (KNXNET_IP_DISCONNECT_REQUEST and 0xFF).toByte(),
        0x00, 0x10,
        (channelId and 0xFF).toByte(),
        0x00,
        *buildHpai(localAddress, localPort)
    )

    private fun buildHpai(localAddress: Inet4Address, localPort: Int): ByteArray {
        val ip = localAddress.address
        return byteArrayOf(
            0x08, 0x01,
            ip[0], ip[1], ip[2], ip[3],
            ((localPort ushr 8) and 0xFF).toByte(),
            (localPort and 0xFF).toByte()
        )
    }

    private fun parseConnectResponse(data: ByteArray, length: Int): ConnectResponse {
        if (length < 8) return ConnectResponse.Invalid

        val headerLength = data[0].toInt() and 0xFF
        val protocolVersion = data[1].toInt() and 0xFF
        val serviceType = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val totalLength = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)

        if (
            headerLength != 6 ||
            protocolVersion != 0x10 ||
            serviceType != KNXNET_IP_CONNECT_RESPONSE ||
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

    private sealed interface ConnectResponse {
        data class Accepted(val channelId: Int) : ConnectResponse
        data class Rejected(val status: Int) : ConnectResponse
        data object Invalid : ConnectResponse
    }
}
