package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import org.opencv.core.Mat;

import java.util.HashMap;
import java.util.Map;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.Constants;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.QuaternionPoint;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.AreaEnum;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class AreaProcessor {
    private final KiboRpcApi api;
    private final MovementService movementService;
    private final VisionService visionService;

    public AreaProcessor(KiboRpcApi api, MovementService movementService, VisionService visionService) {
        this.api = api;
        this.movementService = movementService;
        this.visionService = visionService;
    }
    /**
     * Rotates the Astrobee in place to face -X, +X, -Z, and +Z directions sequentially,
     * captures a NavCam image at each orientation, and returns a map with the orientation labels and images.
     *
     * @return A Map where keys are orientation labels (e.g., "negX", "posX") and values are the corresponding Mat images.
     */
    private Map<String, Mat> rotateAndCaptureNavCamImages() {
        // Map to store the orientation labels and the captured images
        Map<String, Mat> capturedImages = new HashMap<>();

        // Get current position to rotate in place
        Point currentPosition = api.getRobotKinematics().getPosition();

        if (currentPosition == null) {
            Log.e("ROTATE_CAPTURE", "Failed to get current position");
            return capturedImages; // Return empty map if position is unavailable
        }

        // Define orientations for -X, +X, -Z, +Z
        Quaternion facingNegX = new Quaternion(0f, 0f, 0f, 1f);
        Quaternion facingPosX = new Quaternion(0f, 0f, -0.707f, 0.707f);
        Quaternion facingNegZ = new Quaternion(0f, 0.707f, 0f, 0.707f);
        Quaternion facingPosZ = new Quaternion(0f, -0.707f, 0f, 0.707f);

        // Store orientations with labels
        String[] labels = {"negX", "posX", "negZ", "posZ"};
        Quaternion[] quaternions = {facingNegX, facingPosX, facingNegZ, facingPosZ};

        for (int i = 0; i < quaternions.length; i++) {
            Log.d("ROTATE_CAPTURE", "Rotating to face " + labels[i]);

            // Move to the target position and rotate to the correct orientation
            boolean success = movementService.moveToTargetPosition(currentPosition, quaternions[i]);

            if (!success) {
                Log.e("ROTATE_CAPTURE", "Failed to rotate to " + labels[i]);
                continue;
            }

            // Capture NavCam image
            Mat navImage = visionService.getMatNavCamImage();
            if (navImage != null) {
                capturedImages.put(labels[i], navImage);

                if(Constants.DEBUG_MODE){
                    api.saveMatImage(navImage, "ROTATE_CAPTURE_NAV_"+labels[i]+ "_" + System.currentTimeMillis() + ".jpg");
                }

                Log.d("ROTATE_CAPTURE", "Captured and stored image for orientation " + labels[i]);
            } else {
                Log.e("ROTATE_CAPTURE", "Failed to capture image for orientation " + labels[i]);
            }
        }

        return capturedImages;
    }

    /**
     * Moves Astrobee to the pre-defined search area for the given area,
     * waits for the NavCam to stabilize, and returns the captured image.
     *
     * @param area the search area enum
     * @return the captured NavCam image, or an empty Mat if any step fails
     */
    public Mat getLostItemPlaneNavImage(AreaEnum area) {
        try {
            QuaternionPoint plane = Constants.LOST_ITEM_SEARCH_PLANE_PER_AREA.get(area);
            if (plane == null) {
                Log.e("LOST_ITEM_SEARCH", "No plane for " + area);
                return new Mat();
            }

            Point currentPosition = api.getRobotKinematics().getPosition();

            if (currentPosition == null) {
                Log.e("LOST_ITEM_SEARCH", "Failed to get current position");
                return new Mat();
            }

            boolean moveSuccess = movementService.moveToTargetPosition(
                    currentPosition,
                    plane.getQuaternion()
            );

            if (!moveSuccess) {
                Log.e("LOST_ITEM_SEARCH", "MoveTo failed for area " + area);
                return new Mat();
            }

            Mat navImage = visionService.getMatNavCamImage();
            if (navImage == null || navImage.empty()) {
                Log.e("LOST_ITEM_SEARCH", "Captured empty NavCam frame for " + area);
                return new Mat();
            }

            if (Constants.DEBUG_MODE) {
                api.saveMatImage(navImage,
                        "SEARCH_CAPTURE_NAV_" + area + "_" + System.currentTimeMillis() + ".jpg");
            }
            return navImage;
        }
        catch (Exception e) {
            Log.e("LOST_ITEM_SEARCH", "Exception getting NavCam for " + area, e);
            return new Mat();
        }
    }

    /**
     * Attempts to move the robot to a given search area and capture its navigation camera image.
     *
     * @param area        The logical search area being processed (used for logging and image lookup).
     * @param point       The 3D coordinates of the target position to move to.
     * @param orientation The desired orientation (as a quaternion) for the robot at the target position.
     * @return            The captured NavCam image for the lost-item search plane in this area,
     *                    or an empty Mat if the move fails, no image is available, or an unexpected error occurs.
     */
    public Mat processSearchArea(AreaEnum area, Point point, Quaternion orientation) {
        try {
            boolean moved = movementService.moveToTargetPosition(point, orientation);
            if (!moved) {
                Log.e("PROCESS_SEARCH_AREA", "MoveTo failed for " + area + "; continuing anyway");
            }

            Mat nav = getLostItemPlaneNavImage(area);
            return nav != null ? nav : new Mat();
        }
        catch (Exception e) {
            Log.e("PROCESS_SEARCH_AREA", "Unexpected error in " + area, e);
            return new Mat();
        }
    }


}
