package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.Constants;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.AreaEnum;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class VisionService {
    private final KiboRpcApi api;
    private final MovementService movementService;

    public VisionService(KiboRpcApi api, MovementService movementService) {
        this.api = api;
        this.movementService = movementService;
    }

    /**
     * Attempts to retrieve a Mat image from the NavCam using getMatNavCam().
     * Retries up to MAX_RETRIES times with a delay between attempts.
     *
     * @return Mat image of NavCam (1280x960, CV_8UC1), or null if all attempts fail.
     */
    public Mat getMatNavCamImage() {
        Log.d("NAV_CAM", "Attempting to take Nav cam image");

        for (int attempt = 1; attempt <= Constants.MAX_RETRIES; attempt++) {
            movementService.wait(Constants.CAM_STABILIZATION_WAIT_MS);

            Mat mat = api.getMatNavCam();

            if (mat != null && !mat.empty()) {
                return mat;
            }

            Log.d("NAV_CAM","NavCam image fetch failed (attempt " + attempt + "). Retrying...");

            movementService.wait(Constants.RETRY_WAIT_MS);
        }

        Log.d("NAV_CAM","Failed to take NavCam image");

        return null;
    }

    /**
     * Detects ArUco markers in the given image using a predefined dictionary.
     * The image is undistorted before marker detection using camera intrinsics.
     * The detected marker corners and IDs are returned via the provided references.
     *
     * @param sourceImage the input image from which markers will be detected (Mat)
     * @param corners an output parameter that will hold corner positions of detected markers (List<Mat>)
     * @param ids an output Mat that will contain the IDs of detected markers
     */
    public void readArTag(Mat sourceImage,
                          List<Mat> corners,
                          Mat ids,
                          AreaEnum area)
    {
        if (sourceImage == null || sourceImage.empty()) {
            Log.w("AR_TAG", "Empty sourceImage for " + area + "; skipping detect");
            return;
        }

        Dictionary dict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        Mat undistorted = null;
        try {
            undistorted = undistortImage(sourceImage, area);
            if (undistorted == null || undistorted.empty()) {
                Log.w("AR_TAG", "Undistorted image empty for " + area);
                return;
            }

            Aruco.detectMarkers(undistorted, dict, corners, ids);
        }
        catch (Exception e) {
            Log.e("AR_TAG", "detectMarkers failed for " + area, e);
        }
        finally {
            if (undistorted != null) undistorted.release();
        }

        if (Constants.DEBUG_MODE) {
            try {
                Mat debug = undistorted.clone();
                if (ids.total() > 0 && !corners.isEmpty()) {
                    Aruco.drawDetectedMarkers(debug, corners, ids, new Scalar(0, 255, 0));
                }
                api.saveMatImage(debug,
                        "AR_IMAGE_" + area + "_" + System.currentTimeMillis() + ".jpg");
                debug.release();
            }
            catch (Exception e) {
                Log.e("AR_TAG", "drawDetectedMarkers or save failed for " + area, e);
            }

            if (ids.total() > 0) {
                Log.d("AR_TAG", "Detected ArUco IDs: " + ids.dump());
            } else {
                Log.d("AR_TAG", "No ArUco markers detected in " + area);
            }
        }
    }

    /**
     * Undistorts the given source image using navigation camera intrinsics.
     *
     * @param sourceImage the distorted input image (Mat)
     * @return undistorted output image (Mat), or the original image on failure
     */
    private Mat undistortImage(final Mat sourceImage, AreaEnum area) {
        Log.d("UNDISTORT", "Starting image undistortion");

        // Validate input image
        if (sourceImage == null || sourceImage.empty()) {
            Log.e("UNDISTORT", "Source image is null or empty");
            return sourceImage;
        }

        // Retrieve camera intrinsics: [0] = camera matrix elements, [1] = distortion coefficients
        double[][] navCamIntrinsics = api.getNavCamIntrinsics();
        if (navCamIntrinsics == null || navCamIntrinsics.length < 2) {
            Log.e("UNDISTORT", "Invalid intrinsics data received");
            return sourceImage;
        }

        // Flattened camera matrix (row-major): fx, 0, cx, 0, fy, cy, 0, 0, 1
        double[] cameraData = navCamIntrinsics[0];
        if (cameraData == null || cameraData.length < 9) {
            Log.e("UNDISTORT", "Camera matrix must have 9 elements");
            return sourceImage;
        }

        // Distortion coefficients: k1, k2, p1, p2, k3 (or more)
        double[] distCoeffsArr = navCamIntrinsics[1];
        if (distCoeffsArr == null || distCoeffsArr.length < 5) {
            Log.w("UNDISTORT", "Distortion array has fewer than 5 coefficients");
        }

        // Construct OpenCV Mats for intrinsics and distortion
        Mat cameraMatrix = Mat.eye(3, 3, CvType.CV_64F);
        cameraMatrix.put(0, 0, cameraData);

        MatOfDouble distCoeffs = new MatOfDouble(distCoeffsArr);

        Mat undistorted = new Mat();

        try {
            // Undistort the image using Calib3d
            Calib3d.undistort(sourceImage, undistorted, cameraMatrix, distCoeffs);
            Log.d("UNDISTORT", "Image undistorted successfully");

            if(Constants.DEBUG_MODE) {
                api.saveMatImage(undistorted, "UNDISTORATED_IMG_" + area + "_" + System.currentTimeMillis() + ".jpg");
            }
        } catch (Exception e) {
            Log.e("UNDISTORT", "Error during undistortion, returning original image", e);
            return sourceImage;
        }

        return undistorted;
    }

    /**
     * Crops a region of interest (ROI) from the given source image, with an extra padding margin.
     *
     * @param sourceImage The original OpenCV Mat image to crop from.
     * @param x1Y1        A two-element array containing the top-left coordinates [x1, y1] of the desired crop.
     *                     These values will be adjusted by subtracting padding.
     * @param x2Y2        A two-element array containing the bottom-right coordinates [x2, y2] of the desired crop.
     *                     These values will be adjusted by adding padding.
     * @return            A new Mat representing the cropped region (including padding). If the coordinates
     *                    go out of bounds you may get an exception—consider clamping to image size if needed.
     */
    private Mat cropMatImage(Mat sourceImage, double [] x1Y1, double [] x2Y2){
        // some slack room
        x1Y1[0] -= 20;
        x1Y1[1] -= 20;
        x2Y2[0] += 20;
        x2Y2[1] += 20;

        Log.d("CROP_MAT_IMAGE", "x1: "+x1Y1[0]+" y1: "+ x1Y1[1]+" x2: "+x2Y2[0]+" y2: "+x2Y2[1]);

        int cropWidth = (int)x2Y2[0] - (int)x1Y1[0]+1;
        int cropHeight = (int)x2Y2[1] - (int)x1Y1[1]+1;

        Rect roi = new Rect((int)x1Y1[0], (int)x1Y1[1] , cropWidth, cropHeight);

        return new Mat(sourceImage, roi);
    }
}
