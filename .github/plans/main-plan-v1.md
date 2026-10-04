# SYSTEM PROMPT: Single-Device Multimodal Target Detection & Tracking System (Android / Edge AI)

## 1. Project Goal
Develop an Android application for **Samsung S25 FE** that acts as an autonomous, stationary multimodal sensor node (Audio + Dual Camera) for detecting, classifying, and tracking targets (Humans, Vehicles, Drones) in real-time.

## 2. Hardware Specs & Capabilities (Target Platform: Samsung S25 FE)
- **SoC/NPU:** Exynos / Snapdragon with high-performance NPU (Optimized for TFLite / ONNX Runtime with NNAPI / GPU delegates).
- **Cameras:** Concurrent Dual-Camera capture (Rear Primary + Front Wide-angle) via Android Camera2 API / CameraX concurrent streaming to double FOV coverage (~160° total).
- **Microphones:** Multi-microphone array (Top/Bottom) recording raw PCM audio (48kHz, 16-bit) for spectral analysis and sound pressure level (SPL) tracking.

---

## 3. Core System Architecture

### Phase A: Acoustic Trigger & Classifier (Always-On / Low-Power)
1. **Audio Capture:** Continuously stream raw audio buffer via `AudioRecord`.
2. **Feature Extraction:** Compute Short-Time Fourier Transform (STFT) / Mel-spectrograms every 100ms.
3. **Audio CNN Classifier:** Run lightweight TFLite model to detect target spectral signatures:
    - Target Classes: `Drone (BLDC motor noise)`, `Vehicle (Engine/Tire noise)`, `Human (Voice/Steps)`, `Background Noise`.
4. **Behavior:** When target confidence > 70% OR sound pressure level spike (SPL > threshold), raise `Acoustic_Trigger_Flag` and output estimated sound intensity.

### Phase B: Dual-Camera Optical Detection & Fusion
1. **Dual Video Stream:** Capture frames concurrently from Rear (Primary) and Front cameras at 1080p @ 15-30 FPS.
2. **NPU Inference (YOLOv8-nano / MobileNet-SSDLite):**
    - Run object detection on incoming frames from both cameras upon `Acoustic_Trigger_Flag` (or continuous low-FPS mode).
    - Detect Bounding Boxes: `[x_min, y_min, x_max, y_max, class, confidence]`.
3. **Monocular Distance & Azimuth Estimation:**
    - **Azimuth ($\theta$):** Calculate angle relative to camera optical axis based on horizontal pixel position $X_c$ and camera horizontal FOV ($\approx 80^\circ$).
    - **Distance ($D$):** Estimate distance using known physical dimensions ($H_{real}$) vs bounding box pixel height ($h_{pixel}$) using pinhole camera model: $D = (F_{mm} \times H_{real} \times Img_{height}) / (h_{pixel} \times Sensor_{height})$.
    - Adjust distance estimation using Acoustic SPL correlation (increasing dB = approaching target).

### Phase C: Tracking & Sensor Fusion (ByteTrack + Kalman Filter)
1. **State Vector:** Maintain target state $X = [x, y, v_x, v_y, \text{Azimuth}, \text{Distance}, \text{Camera_ID}]$.
2. **Kalman Filter:** Predict and smooth trajectory across consecutive frame detections.
3. **Output Telemetry Packet (JSON over MQTT/UDP Socket):**
```json
{
  "node_id": "s25fe_node_01",
  "timestamp_ms": 1718000000000,
  "target_detected": true,
  "class": "Drone",
  "confidence": 0.89,
  "camera_source": "REAR", // REAR or FRONT
  "azimuth_deg": 14.5,     // Angle relative to phone facing
  "estimated_dist_m": 35.2,
  "trajectory_vector": {"speed_m_s": 8.5, "direction": "approaching"},
  "audio_spl_db": 68.4
}