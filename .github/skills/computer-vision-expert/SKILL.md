---
name: computer-vision-expert
description: 'Provides edge object-detection, multi-object tracking, and monocular geometry guidance for real-time Android camera pipelines.'
---
# Computer Vision Expert

Use this skill when designing, implementing, training, debugging, or reviewing real-time object detection, multi-object tracking, or monocular target-geometry pipelines for Android edge devices.

## Workflow

1. Define target classes, operating distances, camera geometry, frame-rate and latency budgets, accuracy requirements, and hardware constraints.
2. Inspect camera metadata, model input contract, runtime delegates, tracking requirements, and the format expected by downstream telemetry consumers.
3. Build a bounded pipeline: capture, preprocessing, detection, coordinate conversion, tracking, geometry estimation, and output.
4. Preserve camera ID, frame timestamp, rotation, crop, scale, and calibration metadata through every stage.
5. Evaluate detection, tracking, geometry accuracy, throughput, memory, power, and thermal behavior on representative physical devices.
6. Tune and validate using field data that reflects expected range, lighting, weather, motion blur, occlusion, and target scale.

## Dataset and Detector Training

- Define class labels and annotation rules before collecting data, including how to annotate partial, occluded, truncated, or ambiguous targets.
- Split data by recording sequence, location, and source device to prevent near-duplicate frame leakage.
- Include representative negatives and hard examples such as birds, shadows, reflections, distant lights, and visually similar machinery.
- Track class balance and object-size distribution; report metrics separately for small, medium, and large targets.
- Use deployment-realistic augmentation without creating physically implausible samples.
- Evaluate precision, recall, per-class average precision, confusion matrices, and false positives per unit time.
- Version labels, preprocessing parameters, model weights, export settings, and evaluation results together.

## Edge Detector Deployment

- Select YOLO nano variants, MobileNet SSD, or another compact detector based on measured accuracy and latency rather than model size alone.
- Match color order, resize method, letterboxing, normalization, tensor layout, quantization, and output decoding to the exported model.
- Reuse image and tensor buffers and avoid per-frame allocations or unnecessary bitmap conversions.
- Benchmark CPU, GPU, NNAPI, and vendor delegates on the target phone, including warm-up and sustained-load behavior.
- Apply non-maximum suppression with documented confidence and overlap thresholds, preferably outside hot loops when the runtime does not fuse it.
- Convert model-space boxes back to source-frame coordinates while accounting for crop, padding, rotation, mirroring, and scaling.

## Multi-Object Tracking

- Use SORT for simple motion-dominated scenes or ByteTrack when low-confidence detections improve continuity through occlusion.
- Define tracker state, association metric, confidence thresholds, track confirmation age, maximum lost age, and removal policy explicitly.
- Use frame timestamps in motion prediction when capture intervals are variable.
- Keep tentative tracks separate from confirmed tracks and do not publish unstable IDs prematurely.
- Report track lifecycle events such as created, updated, temporarily lost, and removed.
- Measure ID switches, fragmentation, track recall, and tracking latency in addition to detector metrics.
- Do not assume track IDs transfer between front and rear cameras. Cross-camera identity requires overlapping geometry, calibrated handoff rules, or a validated re-identification model.

## Dual-Camera Handling

- Maintain independent calibration, preprocessing transforms, and tracker state for each camera unless a validated fusion method is implemented.
- Include `camera_id` in every detection, track, and geometry estimate.
- Synchronize streams only to the degree required by fusion, and define the maximum acceptable timestamp skew.
- Account for front-camera mirroring and differences in focal length, field of view, resolution, distortion, and sensor orientation.
- Use calibrated camera-to-device transforms before expressing azimuth in a shared phone coordinate frame.
- Query and validate supported concurrent-camera stream combinations on the actual device.

## Monocular Geometry

- Derive focal length in pixels from calibration or camera intrinsics; do not rely on nominal field of view when calibrated intrinsics are available.
- Estimate azimuth from the undistorted box center or another validated target reference point.
- Estimate range from a known target dimension only when the class and visible dimension are sufficiently reliable.
- Correct for image resize, crop, digital zoom, lens distortion, and device mounting orientation.
- Treat bounding-box range as uncertain: propagate class-size assumptions, pixel error, truncation, pose, and occlusion into a quality or uncertainty field.
- Smooth range and bearing over time using a Kalman filter or another state estimator without hiding abrupt real target motion.
- Prefer sensor fusion, stereo, time-of-flight, or external ranging when available; monocular box geometry is an approximation.

## Performance and Reliability

- Bound every queue and drop stale frames when inference falls behind capture.
- Separate camera callbacks, preprocessing, inference, tracking, and publishing onto appropriate workers without excessive context switching.
- Define thermal degradation levels such as reduced resolution, lower detector frequency, or alternating camera inference.
- Keep tracker prediction operational across intentionally skipped detector frames.
- Surface camera, model, delegate, decoding, calibration, and tracker failures through established logging and health telemetry.

## Validation

- Maintain replayable labeled video sequences for deterministic detector, tracker, and geometry regression tests.
- Verify coordinate transforms with synthetic boxes and known image points.
- Test occlusion, crossings, entry and exit, missed detections, false detections, variable frame intervals, camera switching, and concurrent streams.
- Compare estimated azimuth and range against surveyed ground truth at multiple positions and orientations.
- Report confidence and uncertainty honestly; never present monocular estimates as precise measurements without calibration evidence.
