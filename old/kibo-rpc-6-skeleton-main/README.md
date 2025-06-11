# Kibo RPC 6 Skeleton Code by STEMX365

> **Author:** Hashib Islam (ihashib)

This is a reference implementation of an Astrobee mission using the JAXA Kibo-RPC SDK and OpenCV ArUco marker detection. It demonstrates how to
- Move Astrobee to predefined search areas
- Capture and undistort NavCam images
- Detect ArUco markers and report findings
- Integrate movement, vision, and area-processing services

---

## 📂 Project Structure

```
src/
├─ common/
│  ├─ Constants.java          # Global retry, wait, area bounds & orientations
│  ├─ QuaternionPoint.java    # Pairing of Point + Quaternion + direction
│  ├─ ArTagDetectionData.java # Encapsulates detected corners & IDs
│  └─ enumeration/
│     ├─ AreaEnum.java
│     └─ CartesianDirection.java
│
├─ MovementService.java       # moveToTargetPosition() with retries & logging
├─ VisionService.java         # NavCam capture, undistort, ArUco detect/draw
├─ AreaProcessor.java         # Rotate, capture NavCam at –X/+X/–Z/+Z
└─ YourService.java           # KiboRpcService entrypoint with runPlan1()
```

---

## 🔧 Key Classes

### `MovementService`
- **`moveToTargetPosition(Point, Quaternion)`**
  Moves Astrobee with retry logic and waits for completion.

### `VisionService`
- **`getMatNavCamImage()`**
  Grabs a NavCam frame, with retry and stabilization delays.
- **`readArTag(Mat, List<Mat>, Mat, AreaEnum)`**
  Undistorts, detects ArUco markers, and optionally draws/debug-saves images.

### `AreaProcessor`
- **`rotateAndCaptureNavCamImages()`**
  Faces Astrobee in four cardinal orientations and captures images.
- **`processSearchArea(AreaEnum, Point, Quaternion)`**
  Moves into an area and returns a NavCam frame.

### `YourService` (`KiboRpcService`)
- **`runPlan1()`**
  Sequence: start mission → entry move → per-area processing → astronaut rendezvous → reporting.

---

## 🧪 Debugging & Testing

- Set `Constants.DEBUG_MODE = true` to save intermediate images via `api.saveMatImage(...)`.

---

## 📖 References

- **Astrobee Kibo-RPC SDK**
  https://github.com/nasa/astrobee/tree/master/kibo_rpc
- **OpenCV ArUco**
  https://docs.opencv.org/4.5.3/d5/dae/tutorial_aruco_detection.html

---

## 📜 License

This sample is provided under the MIT License. See [LICENSE](./LICENSE) for details.
