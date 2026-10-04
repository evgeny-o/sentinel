package com.ptero.sentinel.transport

import com.ptero.sentinel.delivery.DeliveryMessage

interface TelemetryTransport {
    val id: String

    suspend fun send(message: DeliveryMessage): SendResult
}

sealed interface SendResult {
    data class Acknowledged(
        val sequenceId: Long,
    ) : SendResult

    data class Failed(
        val reason: String,
    ) : SendResult
}
