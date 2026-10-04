package com.ptero.sentinel.telemetry

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryCodecTest {
    @Test
    fun `health envelope contains versioned delivery metadata`() {
        val codec = TelemetryCodec(
            nodeId = "test-node",
            sessionId = "test-session",
        )
        val encoded = codec.encodeHealth(
            sequenceId = 42,
            capturedAtMs = 1_700_000_000_000,
            capturedElapsedRealtimeMs = 123_456,
            payload = NodeHealthTelemetry(
                serviceState = "running",
                activeTransport = "wifi",
                queueDepth = 3,
                batteryPercent = 80,
                thermalStatus = 0,
                lastError = null,
            ),
        )

        val envelope = TelemetryCodec.defaultJson.parseToJsonElement(encoded).jsonObject

        assertEquals(1, envelope.getValue("schema_version").jsonPrimitive.content.toInt())
        assertEquals("test-node", envelope.getValue("node_id").jsonPrimitive.content)
        assertEquals("test-session", envelope.getValue("session_id").jsonPrimitive.content)
        assertEquals(42, envelope.getValue("sequence_id").jsonPrimitive.content.toLong())
        assertEquals("node_health", envelope.getValue("message_type").jsonPrimitive.content)
    }
}
