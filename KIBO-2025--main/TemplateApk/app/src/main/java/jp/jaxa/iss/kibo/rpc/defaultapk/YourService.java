package jp.jaxa.iss.kibo.rpc.defaultapk;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.Detection;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

public class YourService extends KiboRpcService {
    private final String TAG = this.getClass().getSimpleName();
    private YOLOv8Detector detector;

    // Class labels in your model
    private static final List<String> CLASS_LABELS = Arrays.asList(
            "coin", "compass", "coral", "crystal", "diamond", "emerald",
            "fossil", "key", "letter", "shell", "treasure_box"
    );

    @Override
    protected void runPlan1() {
        try {
            detector = new YOLOv8Detector(getApplicationContext());
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize YOLOv8 detector: " + e.getMessage());
            return;
        }

        api.startMission();

        // Move to area 1
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);

        if (move(point, quaternion, 1)) {
            // Only capture and process image after successful movement
            captureAndAnalyze(1);
        } else {
            Log.e(TAG, "Failed to move to area 1");
        }

        // Move to astronaut
        point = new Point(11.143d, -6.7607d, 4.9654d);
        quaternion = new Quaternion(0f, 0f, 0.707f, 0.707f);

        Result result = api.moveTo(point, quaternion, false);
        if (result.hasSucceeded()) {
            wait(2000); // Wait for robot to stabilize
            api.reportRoundingCompletion();
            api.notifyRecognitionItem();
            api.takeTargetItemSnapshot();
        } else {
            Log.e(TAG, "Failed to move to astronaut position");
        }
    }

    public boolean move(Point point, Quaternion quaternion, int areaNumber) {
        Log.i(TAG, "Attempting to move to Area " + areaNumber + ", Point:" + point + "; Quaternion: " + quaternion);

        if (point == null || quaternion == null) {
            Log.e(TAG, "Cannot move, params are null");
            return false;
        }

        int retryCount = 0;
        Result result;

        do {
            result = api.moveTo(point, quaternion, true);

            if (result.hasSucceeded()) {
                Log.i(TAG, "Successfully moved to Area " + areaNumber);
                wait(2000); // Wait longer for robot to stabilize
                return true;
            }

            retryCount++;
            Log.w(TAG, "Move attempt " + retryCount + " failed for Area " + areaNumber);
            wait(1000); // Wait longer between retries

        } while (!result.hasSucceeded() && retryCount < 3);

        Log.e(TAG, "Move to Area " + areaNumber + " failed after " + retryCount + " attempts");
        return false;
    }

    private void captureAndAnalyze(int areaNumber) {
        Log.i(TAG, "Capturing and analyzing image for Area " + areaNumber);

        // Wait a bit more to ensure camera has updated
        wait(1000);

        // Capture multiple images to ensure we get a fresh one
        Mat image1 = api.getMatNavCam();
        wait(500);
        Mat image2 = api.getMatNavCam();
        wait(500);
        Mat image3 = api.getMatNavCam();

        // Use the latest image
        Mat finalImage = image3;

        // Save the raw image for debugging
        api.saveMatImage(finalImage, "area" + areaNumber + "_raw.jpg");

        // Process image
        Bitmap originalBitmap = processImage(finalImage);
        if (originalBitmap == null) {
            Log.e(TAG, "Failed to process image for Area " + areaNumber);
            return;
        }

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 640, 640, true);

        // Save processed image for debugging
        api.saveBitmapImage(resizedBitmap, "area" + areaNumber + "_processed.jpg");

        // Detect objects
        TensorImage input = TensorImage.fromBitmap(resizedBitmap);
        List<Detection> detections = detector.detect(input);

        Log.i(TAG, "Found " + detections.size() + " detections in Area " + areaNumber);

        // Analyze detections
        analyzeDetections(detections, areaNumber);
    }

    private Bitmap processImage(Mat grayMat) {
        try {
            if (grayMat == null || grayMat.empty()) {
                Log.e(TAG, "Input Mat is null or empty");
                return null;
            }

            // Step 1: Convert grayscale to RGB
            Mat rgbMat = new Mat();
            Imgproc.cvtColor(grayMat, rgbMat, Imgproc.COLOR_GRAY2RGB);

            // Step 2: Create a Bitmap from the RGB Mat
            Bitmap bitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgbMat, bitmap);

            // Clean up
            rgbMat.release();

            return bitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            return null;
        }
    }

    private void analyzeDetections(List<Detection> detections, int areaNumber) {
        int itemCount = 0;
        List<String> detectedItems = new ArrayList<>();

        for (Detection detection : detections) {
            if (detection.getCategories() != null && !detection.getCategories().isEmpty()) {
                String label = detection.getCategories().get(0).getLabel();
                float score = detection.getCategories().get(0).getScore();

                Log.i(TAG, "Detection: " + label + " (confidence: " + score + ")");

                if (CLASS_LABELS.contains(label)) {
                    itemCount++;
                    detectedItems.add(label);
                    Log.i(TAG, "Valid item detected in Area " + areaNumber + ": " + label + " (confidence: " + score + ")");
                }
            }
        }

        if (itemCount > 0) {
            api.setAreaInfo(areaNumber, "items", itemCount);
            Log.i(TAG, "Area " + areaNumber + " total items: " + itemCount + " - " + detectedItems.toString());
        } else {
            Log.i(TAG, "No valid items detected in Area " + areaNumber);
        }
    }

    // Inner YOLOv8Detector class
    private static class YOLOv8Detector {
        private final ObjectDetector detector;

        public YOLOv8Detector(Context context) throws IOException {
            ObjectDetector.ObjectDetectorOptions options =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setMaxResults(10) // Increased to detect more objects
                            .setScoreThreshold(0.3f) // Lowered threshold to catch more detections
                            .build();

            detector = ObjectDetector.createFromFileAndOptions(context, "model.tflite", options);
        }

        public List<Detection> detect(TensorImage image) {
            try {
                return detector.detect(image);
            } catch (Exception e) {
                Log.e("YOLOv8Detector", "Error during detection: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    public void wait(int milliseconds) {
        Log.i(TAG, "Pausing for: " + milliseconds + "ms");

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error pausing for: " + milliseconds + "ms");
            e.printStackTrace();
        }
    }
}