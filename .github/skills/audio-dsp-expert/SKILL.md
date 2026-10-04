---
name: audio-dsp-expert
description: 'Provides real-time Android audio DSP and edge-CNN guidance for low-latency acoustic detection and always-on trigger pipelines.'
---
# Audio DSP Expert

Use this skill when designing, implementing, debugging, or reviewing Android pipelines that continuously capture PCM audio, extract spectral features, classify acoustic events on-device, and trigger more expensive processing.

## Workflow

1. Define target sounds, sample rate, channel count, latency budget, detection range, false-positive tolerance, and device power limits.
2. Inspect available microphones, supported `AudioRecord` formats, Android API level, model runtime, and existing threading or service architecture.
3. Build a bounded pipeline: capture, frame buffering, conditioning, feature extraction, inference, temporal smoothing, and trigger emission.
4. Validate DSP and model preprocessing against the training pipeline using identical windowing, FFT, Mel filter-bank, scaling, and tensor layout.
5. Measure latency, recall, false-positive rate, CPU load, battery use, memory, and thermal behavior on the target physical device.
6. Tune thresholds with representative field recordings rather than clean laboratory samples alone.

## Real-Time PCM Capture

- Use `AudioRecord` with a format supported by the target device; verify the selected sample rate, encoding, channel mask, audio source, and minimum buffer size at runtime.
- Prefer 16-bit PCM unless the model or measurement requirements justify float capture.
- Read audio on a dedicated high-priority worker without blocking the main thread.
- Reuse buffers and keep queues bounded. Detect and report overruns instead of silently processing stale audio.
- Preserve frame timestamps and sample counts so downstream windows remain continuous and latency can be measured.
- Handle permission denial, recorder initialization failure, route changes, interruptions, and service shutdown explicitly.

## DSP and Feature Extraction

- Remove DC offset and apply normalization or pre-emphasis only when it matches model training.
- Divide PCM into overlapping frames using a documented window length and hop length.
- Apply a Hann or other training-compatible window before FFT.
- Compute STFT power spectra, apply the configured Mel filter bank, and convert to log-Mel features with a stable numerical floor.
- Avoid allocations in the per-hop path; reuse FFT plans, arrays, tensors, and model buffers.
- Confirm frequency-bin mapping, Nyquist handling, FFT normalization, Mel scale, log base, and feature orientation with known test vectors.
- Do not treat device microphone amplitude as calibrated SPL unless a calibration procedure and reference level are available.

## Audio CNN Design

- Use a compact CNN architecture appropriate for spectrogram patches and the target latency budget.
- Include representative drone propeller, engine, footsteps, speech, wind, handling noise, and ambient recordings in the dataset.
- Split data by recording session, location, and source device to prevent leakage between training and evaluation.
- Address class imbalance and evaluate per-class precision, recall, confusion matrices, and performance at relevant signal-to-noise ratios.
- Use augmentation that reflects deployment conditions, including gain variation, background mixing, time shifts, band limiting, and moderate spectral masking.
- Export preprocessing constants with the model and version them as one artifact.

## Quantization and Edge Deployment

- Prefer full integer quantization when the target delegate supports all required operations and accuracy remains acceptable.
- Build the representative calibration dataset from realistic deployment audio spanning expected devices and environments.
- Compare float, dynamic-range, float16, and int8 models using the same evaluation set.
- Verify tensor data types, scales, zero points, shapes, and output interpretation at runtime.
- Benchmark accelerated and CPU execution on target hardware; use explicit, observable fallback behavior when an accelerator is unavailable.

## Trigger Logic

- Smooth frame-level probabilities over a defined temporal window to avoid rapid trigger oscillation.
- Use separate activation and release thresholds or a cooldown period where appropriate.
- Combine classifier confidence with signal-quality indicators; do not use raw amplitude as the sole classifier.
- Emit trigger events with class probabilities, timestamp, confidence, signal level, model version, and diagnostic quality flags.
- Keep camera activation asynchronous so audio capture continues without gaps.
- Apply rate limits and bounded retention to repeated events.

## Reliability and Validation

- Run continuous processing in an appropriately typed foreground service and respect Android background-execution restrictions.
- Define degraded modes for thermal pressure or low battery, such as larger hop sizes or reduced inference frequency.
- Test silence, clipping, impulse noise, wind, speech, music, overlapping targets, route changes, screen-off operation, and long-duration execution.
- Maintain a replayable corpus of labeled PCM fixtures for deterministic DSP and inference regression tests.
- Validate feature tensors against a reference implementation within documented numerical tolerances.
- Record operational metrics for capture overruns, end-to-end latency, inference duration, queue depth, trigger count, and thermal state.
