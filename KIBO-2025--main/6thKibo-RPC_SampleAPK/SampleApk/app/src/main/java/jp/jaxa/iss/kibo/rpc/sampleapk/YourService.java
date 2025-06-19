package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.Map;

public class YourService extends KiboRpcService {
    private static final String TAG = "YourService";
    //    private Detector detector;
    private DetectorMerged detectorMerged;

    private final double[][] place = {
            {10.95,-9.85,5.195},
            {10.925,-8.875,4.52},
            {10.942,-7.75,4.51},
            {10.6,-6.852,4.94}
    };

    private final float[][] angle = {
            {0,0,-0.707f,0.707f},
            {-0.0923f,0.7002f,-0.0923f,0.7002f},
            {0.1651f,0.6876f,0.1651f,0.6876f},
            {0,1f,0,0}
    };


    @Override
    protected void runPlan1() {
        Log.d(TAG, "RUN1 start");


//        try {
//            detector = new Detector(getApplicationContext());
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to init Detector", e);
//        }

        detectorMerged = new DetectorMerged(this, "best_float32.tflite", "labelmap.txt");

        api.startMission();

        // Move through each area, capture and detect
        for (int i = 0; i < place.length; i++) {
            Log.d(TAG, "Move through each area, capture and detect start");

            Point p = new Point(place[i][0], place[i][1], place[i][2]);
            Quaternion q = new Quaternion(
                    angle[i][0], angle[i][1], angle[i][2], angle[i][3]
            );
            moveAndDetect(p, q, i);
            Log.d(TAG, "Move through each area, capture and detect success");
        }

        // Final position in front of astronaut
        Point p1 = new Point(11.143, -6.7607, 4.9654);
        Quaternion q1 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        moveAndDetect(p1, q1, 4);
//=============================
        api.reportRoundingCompletion();
        //=========================
        api.notifyRecognitionItem();

        // Take snapshot of target item held by astronaut
        api.takeTargetItemSnapshot();
        Log.d(TAG, "RUN1 shesh");

    }

    /**
     * Moves, captures, runs detection, and reports via setAreaInfo.
     * @param idx used both in filename and as areaId for reporting
     */
    private void moveAndDetect(Point point, Quaternion quat, int idx) {

        Log.i(TAG, "Moving to " + point);
        Result r = api.moveTo(point, quat, true);
        if (!r.hasSucceeded()) {
            Log.w(TAG, "Move failed idx=" + idx);
            return;
        }
        sleep(500);

        // 1) Capture NavCam Mat
        Mat mat = api.getMatNavCam();
        Log.i(TAG, "Captured Mat size: " + mat.rows() + "x" + mat.cols());
        sleep(10);

        // 2) Save raw image for debugging
        String fname = "img@" + idx + ".jpg";
        api.saveMatImage(mat, fname);
        Log.i(TAG, "Saved " + fname);

        // 3) Convert to Bitmap and detect
        Bitmap bmp = matToBitmap(mat);
        Log.i(TAG, "Converted Bitmap size: " + bmp.getWidth() + "x" + bmp.getHeight());
        DetectorMerged.Result det = detectorMerged.detect(bmp);

        // 4) Log detections
        for (BoundingBox d : det.detections) {
            String lbl = detectorMerged.labels.get(d.cls);
            Log.i(TAG, String.format(
                    "→ %s @ [%.1f,%.1f→%.1f,%.1f] score=%.2f",
                    lbl, d.x1, d.y1, d.x2, d.y2, d.cnf
            ));
        }

        // 5) Report counts via setAreaInfo(areaId, itemName, number)
        for (Map.Entry<String,Integer> e : det.counts.entrySet()) {
            String itemName = e.getKey();
            int count       = e.getValue();
            api.setAreaInfo(idx, itemName, count);
            Log.i(TAG, "Reported area " + idx + ": " + itemName + " × " + count);
        }


    }

    private void sleep(int ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException ignored) {}
    }

    @Override
    public void onDestroy() {
        if (detectorMerged != null) detectorMerged.close();
        super.onDestroy();
    }

    /** Convert BGR Mat → ARGB_8888 Bitmap */
    private Bitmap matToBitmap(Mat mat) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
        Bitmap bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(rgb, bmp);
        return bmp;
    }

    @Override protected void runPlan2() { }
    @Override protected void runPlan3() { }
}
