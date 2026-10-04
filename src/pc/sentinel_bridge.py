"""Sentinel phone-to-PC telemetry bridge."""

from __future__ import annotations

import argparse
import json
import logging
import socket
import socketserver
import threading
from dataclasses import dataclass
from typing import Any

LOG = logging.getLogger("sentinel.bridge")
MAX_MESSAGE_BYTES = 1_048_576
SUPPORTED_SCHEMA_VERSION = 1
SUPPORTED_MESSAGE_TYPES = frozenset({"node_health", "target_track"})


class InvalidTelemetry(ValueError):
    """Raised when a phone message does not satisfy the bridge contract."""


@dataclass(frozen=True)
class TrackerEndpoint:
    host: str
    port: int


def validate_envelope(value: Any) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise InvalidTelemetry("Envelope must be a JSON object")

    required_types: dict[str, type] = {
        "schema_version": int,
        "message_id": str,
        "session_id": str,
        "node_id": str,
        "sequence_id": int,
        "captured_at_ms": int,
        "captured_elapsed_realtime_ms": int,
        "message_type": str,
        "payload": dict,
    }
    for field, expected_type in required_types.items():
        if field not in value or type(value[field]) is not expected_type:
            raise InvalidTelemetry(f"Field '{field}' must be {expected_type.__name__}")

    if value["schema_version"] != SUPPORTED_SCHEMA_VERSION:
        raise InvalidTelemetry("Unsupported schema version")
    if value["message_type"] not in SUPPORTED_MESSAGE_TYPES:
        raise InvalidTelemetry("Unsupported message type")
    if value["sequence_id"] < 1:
        raise InvalidTelemetry("Sequence ID must be positive")
    if not value["session_id"] or not value["node_id"] or not value["message_id"]:
        raise InvalidTelemetry("Identity fields must not be empty")
    return value


class SequenceRegistry:
    def __init__(self) -> None:
        self._highest_by_session: dict[str, int] = {}
        self._lock = threading.Lock()

    def record(self, session_id: str, sequence_id: int) -> tuple[int, bool]:
        with self._lock:
            previous = self._highest_by_session.get(session_id, 0)
            if sequence_id <= previous:
                return previous, True

            self._highest_by_session[session_id] = sequence_id
            return sequence_id, False


class TelemetryBridge:
    def __init__(self, tracker_endpoint: TrackerEndpoint | None = None) -> None:
        self._tracker_endpoint = tracker_endpoint
        self._sequences = SequenceRegistry()

    def process(self, raw_line: bytes) -> bytes:
        if len(raw_line) > MAX_MESSAGE_BYTES:
            raise InvalidTelemetry("Message exceeds size limit")

        try:
            decoded = json.loads(raw_line)
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            raise InvalidTelemetry("Message is not valid UTF-8 JSON") from error

        envelope = validate_envelope(decoded)
        acknowledged_sequence, duplicate = self._sequences.record(
            envelope["session_id"],
            envelope["sequence_id"],
        )
        if not duplicate:
            self._forward(envelope)

        acknowledgement = {
            "session_id": envelope["session_id"],
            "ack_sequence_id": acknowledged_sequence,
        }
        return json.dumps(acknowledgement, separators=(",", ":")).encode("utf-8") + b"\n"

    def _forward(self, envelope: dict[str, Any]) -> None:
        normalized = json.dumps(envelope, separators=(",", ":"), ensure_ascii=True)
        LOG.info(
            "Telemetry node=%s type=%s sequence=%s",
            envelope["node_id"],
            envelope["message_type"],
            envelope["sequence_id"],
        )
        if self._tracker_endpoint is None:
            return

        payload = normalized.encode("utf-8")
        if len(payload) > 65_507:
            raise InvalidTelemetry("Normalized telemetry exceeds one UDP datagram")
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp_socket:
            udp_socket.sendto(
                payload,
                (self._tracker_endpoint.host, self._tracker_endpoint.port),
            )


class TelemetryRequestHandler(socketserver.StreamRequestHandler):
    def handle(self) -> None:
        raw_line = self.rfile.readline(MAX_MESSAGE_BYTES + 1)
        try:
            acknowledgement = self.server.bridge.process(raw_line)
        except InvalidTelemetry as error:
            LOG.warning("Rejected telemetry from %s: %s", self.client_address[0], error)
            return
        self.wfile.write(acknowledgement)
        self.wfile.flush()


class TelemetryServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, address: tuple[str, int], bridge: TelemetryBridge) -> None:
        self.bridge = bridge
        super().__init__(address, TelemetryRequestHandler)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--listen-host", default="0.0.0.0")
    parser.add_argument("--listen-port", type=int, default=47_500)
    parser.add_argument("--tracker-host")
    parser.add_argument("--tracker-port", type=int)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if bool(args.tracker_host) != bool(args.tracker_port):
        raise SystemExit("--tracker-host and --tracker-port must be provided together")

    tracker_endpoint = (
        TrackerEndpoint(args.tracker_host, args.tracker_port)
        if args.tracker_host and args.tracker_port
        else None
    )
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    bridge = TelemetryBridge(tracker_endpoint)
    with TelemetryServer((args.listen_host, args.listen_port), bridge) as server:
        LOG.info("Listening on %s:%s", args.listen_host, args.listen_port)
        server.serve_forever()


if __name__ == "__main__":
    main()
