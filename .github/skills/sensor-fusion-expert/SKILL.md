---
name: sensor-fusion-expert
description: 'Provides multimodal sensor-fusion, coordinate transformation, state-estimation, and telemetry guidance for edge tracking systems.'
---
# Sensor Fusion Expert

Use this skill when designing, implementing, debugging, or reviewing systems that combine camera detections, acoustic observations, and Android orientation sensors into stable tracks and transport-ready telemetry.

## Workflow

1. Define all sensor frames, units, timestamp sources, update rates, uncertainty models, and the required output coordinate frame.
2. Inspect camera calibration, device mounting orientation, Android sensor availability, upstream detection contracts, and downstream telemetry requirements.
3. Normalize measurements into explicit typed records before fusion.
4. Align measurements by monotonic timestamp and reject data outside configured age or skew limits.
5. Transform observations into a shared coordinate frame with calibrated transforms.
6. Associate measurements with tracks, update the state estimator, and publish state with confidence and freshness metadata.
7. Validate against recorded fixtures and surveyed ground truth before field deployment.

## Coordinate Frames and Conventions

- Name every frame explicitly, such as camera optical, Android device, magnetic north, true north, local east-north-up, or tracker world frame.
- Document axis direction, handedness, angle sign, angle range, and units at every boundary.
- Use radians internally for trigonometry and convert to degrees only at defined interfaces.
- Normalize bearings consistently, such as `[0, 360)` degrees or `[-pi, pi)` radians.
- Represent frame transforms as tested rotation matrices or quaternions rather than scattered sign changes.
- Apply camera-to-device extrinsic calibration and phone mounting offsets before computing an absolute bearing.
- Do not combine values expressed in different frames or reference north definitions.

## Camera-to-Bearing Conversion

- Prefer calibrated focal lengths and principal points over approximate field-of-view formulas.
- For an undistorted image point `(u, v)`, form a normalized camera ray using the intrinsic matrix inverse.
- Account for crop, rotation, mirroring, letterboxing, digital zoom, and lens distortion before ray construction.
- Rotate the camera ray into the device frame and then into the selected world frame.
- Derive azimuth and elevation from the transformed ray using the documented axis convention.
- Attach uncertainty based on box-center error, calibration quality, object size, truncation, and device-pose accuracy.

## Android Orientation Sensors

- Prefer rotation-vector sensors for device attitude; use raw accelerometer, gyroscope, and magnetometer fusion only when requirements justify maintaining a custom orientation filter.
- Use monotonic sensor event timestamps for alignment, not wall-clock time.
- Remap coordinates for display or mounting orientation using Android sensor APIs or an equivalent tested transform.
- Distinguish magnetic bearing from true bearing and apply magnetic declination only when location and timestamp are available.
- Detect magnetometer interference and lower confidence rather than publishing unstable absolute headings as valid.
- Use gyroscope data for short-term smoothness and an absolute reference for long-term drift correction.

## Multimodal Association

- Keep acoustic detections and camera detections separate until their timestamps, classes, directions, and uncertainties support association.
- Define association gates for time difference, angular difference, class compatibility, and track prediction error.
- Treat audio intensity as supporting evidence unless the microphones are calibrated for reliable ranging.
- Prevent one broad acoustic event from incorrectly updating multiple visual tracks without an explicit allocation rule.
- Record which sensor measurements contributed to each state update for diagnostics.

## Kalman Filtering

- Define the state vector, transition model, measurement models, process covariance, and measurement covariance explicitly.
- Use elapsed monotonic time in the transition matrix when update intervals vary.
- Keep angle residuals wrapped to the selected angular range.
- Use an extended or unscented Kalman filter when measurement equations are materially nonlinear; do not force nonlinear bearing geometry into an invalid linear model.
- Gate updates using innovation covariance or a documented Mahalanobis-distance threshold.
- Handle missing measurements through prediction rather than fabricated observations.
- Reset or reinitialize tracks when covariance, innovation, or elapsed time exceeds configured safety bounds.
- Publish covariance or an interpretable uncertainty derived from it alongside the filtered state.

## Track Lifecycle

- Separate tentative, confirmed, coasting, and deleted track states.
- Require configurable evidence before confirming a new track.
- Preserve stable external track IDs while internal filters remain valid.
- Expire tracks based on elapsed time and uncertainty, not only a fixed frame count.
- Emit lifecycle events so downstream consumers can distinguish updates from track creation or termination.

## Telemetry Contracts

- Prefer Protocol Buffers when compact binary compatibility and schema evolution are priorities; use JSON when inspectability and integration simplicity dominate.
- Wrap payloads in a versioned envelope containing node ID, message ID, sequence number, monotonic capture time, wall-clock time, schema version, and payload type.
- Include coordinate frame, units, source sensors, calibration version, state freshness, confidence, and uncertainty.
- Keep transport concerns separate from the fusion model so MQTT and UDP can carry the same logical message.
- For MQTT, define topic structure, quality of service, retained-message policy, session behavior, and backpressure limits.
- For UDP, define framing, maximum datagram size, ordering, duplicate handling, integrity checks, acknowledgements if required, and fragmentation avoidance.
- Do not serialize non-finite numeric values. Reject or explicitly encode unavailable measurements.

## Reliability and Observability

- Bound all queues and define whether overload drops oldest measurements, newest measurements, or low-priority telemetry.
- Detect stale, duplicate, reordered, and clock-discontinuous data.
- Expose metrics for input rate, measurement age, association rejects, filter innovation, covariance, output rate, queue depth, and transport loss.
- Log calibration and schema versions with each session.
- Fail explicitly on incompatible frames, units, schema versions, or missing required calibration.

## Validation

- Unit-test angle wrapping, quaternion and matrix transforms, coordinate remapping, serialization, and filter prediction/update equations.
- Use synthetic trajectories with controlled noise, dropout, outliers, and variable time steps.
- Replay timestamped camera, audio, and orientation recordings deterministically.
- Compare absolute bearing and range against surveyed ground truth at multiple device orientations.
- Test magnetic interference, orientation changes, camera handoffs, delayed packets, reconnect replay, and prolonged measurement loss.
- Report numerical accuracy, track stability, latency, and uncertainty calibration separately.
