package com.ptero.sentinel.transport

import com.ptero.sentinel.delivery.DeliveryMessage

data class FailoverResult(
    val transportId: String?,
    val sendResult: SendResult,
)

class FailoverTransport(
    private val transports: List<TelemetryTransport>,
) {
    init {
        require(transports.isNotEmpty()) { "At least one transport is required" }
    }

    suspend fun send(message: DeliveryMessage): FailoverResult {
        var lastFailure: SendResult.Failed? = null
        for (transport in transports) {
            when (val result = transport.send(message)) {
                is SendResult.Acknowledged -> {
                    return FailoverResult(transport.id, result)
                }

                is SendResult.Failed -> {
                    lastFailure = result
                }
            }
        }

        return FailoverResult(
            transportId = null,
            sendResult = lastFailure ?: SendResult.Failed("No transport attempted"),
        )
    }
}
