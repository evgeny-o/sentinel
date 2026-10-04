package com.ptero.sentinel.telemetry

import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class TelemetryCodec(
    private val nodeId: String,
    val sessionId: String = UUID.randomUUID().toString(),
    private val json: Json = defaultJson,
) {
    fun encodeTarget(
        sequenceId: Long,
        capturedAtMs: Long,
        capturedElapsedRealtimeMs: Long,
        payload: TargetTrackTelemetry,
    ): String = encode(
        sequenceId = sequenceId,
        capturedAtMs = capturedAtMs,
        capturedElapsedRealtimeMs = capturedElapsedRealtimeMs,
        messageType = MessageType.TARGET_TRACK,
        payload = json.encodeToJsonElement(payload),
    )

    fun encodeHealth(
        sequenceId: Long,
        capturedAtMs: Long,
        capturedElapsedRealtimeMs: Long,
        payload: NodeHealthTelemetry,
    ): String = encode(
        sequenceId = sequenceId,
        capturedAtMs = capturedAtMs,
        capturedElapsedRealtimeMs = capturedElapsedRealtimeMs,
        messageType = MessageType.NODE_HEALTH,
        payload = json.encodeToJsonElement(payload),
    )

    private fun encode(
        sequenceId: Long,
        capturedAtMs: Long,
        capturedElapsedRealtimeMs: Long,
        messageType: MessageType,
        payload: kotlinx.serialization.json.JsonElement,
    ): String {
        val envelope = TelemetryEnvelope(
            schemaVersion = SCHEMA_VERSION,
            messageId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            nodeId = nodeId,
            sequenceId = sequenceId,
            capturedAtMs = capturedAtMs,
            capturedElapsedRealtimeMs = capturedElapsedRealtimeMs,
            messageType = messageType,
            payload = payload,
        )
        return json.encodeToString(envelope)
    }

    companion object {
        const val SCHEMA_VERSION = 1

        val defaultJson = Json {
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = false
        }
    }
}
