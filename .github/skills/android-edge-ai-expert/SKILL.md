---
name: android-edge-ai-expert
description: 'Provides Android edge-AI architecture and implementation guidance for camera capture, accelerated on-device inference, and reliable long-running processing.'
---
# Android Edge AI Expert

Use this skill when designing, implementing, debugging, or reviewing native Android applications that capture camera data, run machine-learning models locally, or perform sustained background processing.

## Workflow

1. Establish the target device, Android API level, model format, latency and accuracy targets, camera requirements, and power or thermal constraints.
2. Inspect existing project conventions, dependencies, lifecycle ownership, threading, and hardware-capability checks before proposing changes.
3. Design the processing pipeline with explicit stages for capture, preprocessing, inference, postprocessing, tracking, and output.
4. Keep capture and inference bounded with backpressure. Drop stale frames rather than allowing an unbounded queue to increase latency.
5. Measure latency, frame rate, memory, power, and thermal behavior on representative physical hardware.
6. Provide CPU-compatible fallbacks when requested hardware acceleration or concurrent-camera capability is unavailable.

## Camera Architecture

- Prefer CameraX for lifecycle integration and portability; use Camera2 when low-level controls or device-specific concurrent-camera behavior require it.
- Query camera characteristics and concurrent-camera capability at runtime. Never assume front and rear streams can run concurrently at the requested resolutions.
- Select compatible stream sizes and frame rates based on the device's supported stream combinations.
- Isolate camera callbacks from preprocessing and inference using bounded executors or coroutine dispatchers.
- Close `ImageProxy`, `Image`, camera sessions, surfaces, and related resources deterministically.
- Preserve frame timestamps, camera identity, rotation, crop region, and calibration metadata through the pipeline.

## On-Device Inference

- Support TensorFlow Lite or ONNX Runtime according to project requirements and model compatibility.
- Evaluate NNAPI, GPU, vendor acceleration such as Qualcomm QNN, and CPU execution on the target hardware rather than assuming accelerator availability.
- Initialize model sessions once, reuse input and output buffers, minimize image copies, and avoid allocations in per-frame hot paths.
- Validate tensor shape, layout, quantization, normalization, and color-space requirements against the exported model.
- Make delegate selection observable and fail explicitly or fall back according to the application's configured policy.
- Benchmark warm inference separately from model loading and first-run compilation.

## Long-Running Processing

- Use an appropriately typed foreground service with a visible notification and required manifest permissions.
- Start foreground execution within Android's required deadline and respect background-start restrictions.
- Keep service ownership, camera lifecycle, inference resources, and shutdown behavior explicit.
- Persist only the state needed to recover after process recreation; do not rely on a service being immortal.
- Monitor thermal status, battery state, memory pressure, and processing latency. Reduce frame rate, resolution, or inference frequency through defined degradation levels.
- Do not attempt to bypass Android power-management or thermal protections. Design graceful throttling and recovery instead.

## Reliability and Validation

- Surface camera, model, delegate, permission, and service failures using the project's established error reporting.
- Test single-camera fallback, accelerator fallback, permission denial, service restart, screen-off behavior, transport loss, and sustained thermal load.
- Validate concurrency and performance on the actual target phone because emulator behavior is not representative for cameras, NPUs, GPUs, or thermals.
- Keep device-specific workarounds isolated behind capability or device adapters and document why they are required.
