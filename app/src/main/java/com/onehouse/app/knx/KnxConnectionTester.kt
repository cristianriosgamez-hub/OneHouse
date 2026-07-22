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
 * Realiza una comprobación no intrusiva de un interfaz KNXnet/IP.
 *
 * Envía un KNXnet/IP Description Request por UDP y espera la respuesta del
 * dispositivo. No abre un túnel KNX ni transmite telegramas al bus.
 */
object KnxConnectionTester {
    private const val TIMEOUT_MILLIS = 3_000
    private const val KNXNET_IP_DESCRIPTION_RESPONSE = 0x0204

    sealed interface Result {
        data class Success(val deviceAddress: String) : Result
        data object Timeout : Result
        data class NetworkError(val detail: String) : Result
        data object InvalidResponse : Result
    }

    fun test(
        host: String,
        port: Int,
        onResult: (Result) -> Unit
    ): Closeable {
        val cancelled = AtomicBoolean(false)
        var socket: DatagramSocket? = null
        val mainHandler = Handler(Looper.getMainLooper())

        val worker = Thread {
            val result = try {
                val targetAddress = InetAddress.getByName(host)
                if (targetAddress !is Inet4Address) {
                    Result.NetworkError("La dirección debe ser IPv4")
                } else {
                    DatagramSocket().use { udpSocket ->
                        socket = udpSocket
                        udpSocket.soTimeout = TIMEOUT_MILLIS
                        udpSocket.connect(InetSocketAddress(targetAddress, port))

                        val request = buildDescriptionRequest(
                            localAddress = udpSocket.localAddress,
                            localPort = udpSocket.localPort
                        )
                        udpSocket.send(DatagramPacket(request, request.size))

                        val responseBuffer = ByteArray(512)
                        val response = DatagramPacket(responseBuffer, responseBuffer.size)
                        udpSocket.receive(response)

                        if (isDescriptionResponse(response.data, response.length)) {
                            Result.Success(response.address.hostAddress ?: host)
                        } else {
                            Result.InvalidResponse
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

    private fun buildDescriptionRequest(localAddress: InetAddress, localPort: Int): ByteArray {
        val ip = (localAddress as? Inet4Address)?.address ?: byteArrayOf(0, 0, 0, 0)

        return byteArrayOf(
            0x06, 0x10,             // KNXnet/IP header length and protocol version
            0x02, 0x03,             // Description Request (0x0203)
            0x00, 0x0E,             // Total length: 14 bytes
            0x08, 0x01,             // HPAI length and UDP protocol code
            ip[0], ip[1], ip[2], ip[3],
            ((localPort ushr 8) and 0xFF).toByte(),
            (localPort and 0xFF).toByte()
        )
    }

    private fun isDescriptionResponse(data: ByteArray, length: Int): Boolean {
        if (length < 6) return false
        val headerLength = data[0].toInt() and 0xFF
        val protocolVersion = data[1].toInt() and 0xFF
        val serviceType = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val totalLength = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)

        return headerLength == 6 &&
            protocolVersion == 0x10 &&
            serviceType == KNXNET_IP_DESCRIPTION_RESPONSE &&
            totalLength in 6..length
    }
}
