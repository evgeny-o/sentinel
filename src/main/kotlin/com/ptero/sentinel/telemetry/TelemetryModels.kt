package com.ptero.sentinel.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TelemetryEnvelope(
    @SerialName("schema_version")
    val schemaVersion: Int,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("node_id")
    val nodeId: String,
    @SerialName("sequence_id")
    val sequenceId: Long,
    @SerialName("captured_at_ms")
    val capturedAtMs: Long,
    @SerialName("captured_elapsed_realtime_ms")
    val capturedElapsedRealtimeMs: Long,
    @SerialName("message_type")
    val messageType: MessageType,
    val payload: JsonElement,
)

@Serializable
enum class MessageType {
    @SerialName("target_track")
    TARGET_TRACK,

    @SerialName("node_health")
    NODE_HEALTH,
}

@Serializable
enum class TargetClass {
    @SerialName("drone")
    DRONE,

    @SerialName("vehicle")
    VEHICLE,

    @SerialName("human")
    HUMAN,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
enum class CameraSource {
    @SerialName("rear")
    REAR,

    @SerialName("front")
    FRONT,

    @SerialName("fused")
    FUSED,
}

@Serializable
data class TargetTrackTelemetry(
    @SerialName("track_id")
    val trackId: String,
    @SerialName("target_class")
    val targetClass: TargetClass,
    val confidence: Double,
    @SerialName("camera_source")
    val cameraSource: CameraSource,
    @SerialName("azimuth_deg")
    val azimuthDeg: Double,
    @SerialName("estimated_distance_m")
    val estimatedDistanceM: Double?,
    @SerialName("speed_m_s")
    val speedMetersPerSecond: Double?,
    @SerialName("audio_spl_db")
    val audioSplDb: Double?,
    @SerialName("quality_flags")
    val qualityFlags: Set<String>,
)

@Serializable
data class NodeHealthTelemetry(
    @SerialName("service_state")
    val serviceState: String,
    @SerialName("active_transport")
    val activeTransport: String?,
    @SerialName("queue_depth")
    val queueDepth: Int,
    @SerialName("battery_percent")
    val batteryPercent: Int?,
    @SerialName("thermal_status")
    val thermalStatus: Int?,
    @SerialName("last_error")
    val lastError: String?,
)

@Serializable
data class DeliveryAcknowledgement(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("ack_sequence_id")
    val acknowledgedSequenceId: Long,
)
