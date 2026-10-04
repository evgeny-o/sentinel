package com.ptero.sentinel.transport

import com.ptero.sentinel.delivery.DeliveryMessage
import com.ptero.sentinel.telemetry.DeliveryAcknowledgement
import com.ptero.sentinel.telemetry.TelemetryCodec
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

class TcpLineTransport(
    override val id: String,
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 1_000,
    private val readTimeoutMs: Int = 2_000,
) : TelemetryTransport {
    override suspend fun send(message: DeliveryMessage): SendResult = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.soTimeout = readTimeoutMs
                socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                socket.getOutputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.appendLine(message.jsonLine)
                    writer.flush()

                    val acknowledgementLine = socket.getInputStream()
                        .bufferedReader(Charsets.UTF_8)
                        .readLine()
                        ?: return@withContext SendResult.Failed("Bridge closed without acknowledgement")
                    val acknowledgement = TelemetryCodec.defaultJson
                        .decodeFromString<DeliveryAcknowledgement>(acknowledgementLine)
                    if (
                        acknowledgement.sessionId == message.sessionId &&
                        acknowledgement.acknowledgedSequenceId >= message.sequenceId
                    ) {
                        SendResult.Acknowledged(acknowledgement.acknowledgedSequenceId)
                    } else {
                        SendResult.Failed("Bridge acknowledgement did not match the current session")
                    }
                }
            }
        } catch (error: IOException) {
            SendResult.Failed(error.message ?: error.javaClass.simpleName)
        } catch (error: SerializationException) {
            SendResult.Failed("Invalid acknowledgement: ${error.message}")
        }
    }
}
