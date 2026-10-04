import json
import unittest

from sentinel_bridge import InvalidTelemetry, TelemetryBridge, validate_envelope


def envelope(sequence_id: int = 1) -> dict[str, object]:
    return {
        "schema_version": 1,
        "message_id": f"message-{sequence_id}",
        "session_id": "session-1",
        "node_id": "node-1",
        "sequence_id": sequence_id,
        "captured_at_ms": 1_700_000_000_000,
        "captured_elapsed_realtime_ms": 123_456,
        "message_type": "node_health",
        "payload": {"service_state": "running"},
    }


class TelemetryBridgeTest(unittest.TestCase):
    def test_process_returns_session_aware_acknowledgement(self) -> None:
        bridge = TelemetryBridge()

        acknowledgement = bridge.process(json.dumps(envelope(7)).encode("utf-8"))

        self.assertEqual(
            {"session_id": "session-1", "ack_sequence_id": 7},
            json.loads(acknowledgement),
        )

    def test_duplicate_returns_highest_acknowledged_sequence(self) -> None:
        bridge = TelemetryBridge()
        bridge.process(json.dumps(envelope(7)).encode("utf-8"))

        acknowledgement = bridge.process(json.dumps(envelope(6)).encode("utf-8"))

        self.assertEqual(7, json.loads(acknowledgement)["ack_sequence_id"])

    def test_rejects_unsupported_schema(self) -> None:
        message = envelope()
        message["schema_version"] = 2

        with self.assertRaisesRegex(InvalidTelemetry, "Unsupported schema"):
            validate_envelope(message)

    def test_rejects_missing_identity(self) -> None:
        message = envelope()
        message["session_id"] = ""

        with self.assertRaisesRegex(InvalidTelemetry, "Identity fields"):
            validate_envelope(message)


if __name__ == "__main__":
    unittest.main()
