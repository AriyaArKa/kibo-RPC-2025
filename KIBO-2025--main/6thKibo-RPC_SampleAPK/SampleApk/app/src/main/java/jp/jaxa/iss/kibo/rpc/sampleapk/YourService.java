// package jp.jaxa.iss.kibo.rpc.sampleapk;

// import android.graphics.Bitmap;
// import android.graphics.Bitmap.Config;
// import android.util.Log;

// import gov.nasa.arc.astrobee.Result;
// import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

// import gov.nasa.arc.astrobee.types.Point;
// import gov.nasa.arc.astrobee.types.Quaternion;

// import org.opencv.core.Mat;
// import org.opencv.imgproc.Imgproc;

// import java.io.IOException;
// import java.util.Map;

// public class YourService extends KiboRpcService {
//     private static final String TAG = "YourService";
//     //    private Detector detector;
//     private DetectorMerged detectorMerged;

//     private final double[][] place = {
//             {10.95,-9.85,5.195},
//             {10.925,-8.875,4.52},
//             {10.942,-7.75,4.51},
//             {10.6,-6.852,4.94}
//     };

//     private final float[][] angle = {
//             {0,0,-0.707f,0.707f},
//             {-0.0923f,0.7002f,-0.0923f,0.7002f},
//             {0.1651f,0.6876f,0.1651f,0.6876f},
//             {0,1f,0,0}
//     };


//     @Override
//     protected void runPlan1() {
//         Log.d(TAG, "RUN1 start");


// //        try {
// //            detector = new Detector(getApplicationContext());
// //        } catch (IOException e) {
// //            Log.e(TAG, "Failed to init Detector", e);
// //        }

//         detectorMerged = new DetectorMerged(this, "best_float32.tflite", "labelmap.txt");

//         api.startMission();

//         // Move through each area, capture and detect
//         for (int i = 0; i < place.length; i++) {
//             Log.d(TAG, "Move through each area, capture and detect start");

//             Point p = new Point(place[i][0], place[i][1], place[i][2]);
//             Quaternion q = new Quaternion(
//                     angle[i][0], angle[i][1], angle[i][2], angle[i][3]
//             );
//             moveAndDetect(p, q, i+1);
//             Log.d(TAG, "Move through each area, capture and detect success");
//         }

//         // Final position in front of astronaut
//         Point p1 = new Point(11.143, -6.7607, 4.9654);
//         Quaternion q1 = new Quaternion(0f, 0f, 0.707f, 0.707f);
//         moveAndDetect(p1, q1, 5);
// //=============================
//         api.reportRoundingCompletion();
//         //=========================
//         api.notifyRecognitionItem();

//         // Take snapshot of target item held by astronaut
//         api.takeTargetItemSnapshot();
//         Log.d(TAG, "RUN1 shesh");

//     }

//     /**
//      * Moves, captures, runs detection, and reports via setAreaInfo.
//      * @param idx used both in filename and as areaId for reporting
//      */
//     private void moveAndDetect(Point point, Quaternion quat, int idx) {

//         Log.i(TAG, "Moving to " + point);
//         Result r = api.moveTo(point, quat, true);
//         if (!r.hasSucceeded()) {
//             Log.w(TAG, "Move failed idx=" + idx);
//             return;
//         }
//         sleep(500);

//         // 1) Capture NavCam Mat
//         Mat mat = api.getMatNavCam();
//         Log.i(TAG, "Captured Mat size: " + mat.rows() + "x" + mat.cols());
//         sleep(10);

//         // 2) Save raw image for debugging
//         String fname = "img@" + idx + ".jpg";
//         api.saveMatImage(mat, fname);
//         Log.i(TAG, "Saved " + fname);

//         // 3) Convert to Bitmap and detect
//         Bitmap bmp = matToBitmap(mat);
//         Log.i(TAG, "Converted Bitmap size: " + bmp.getWidth() + "x" + bmp.getHeight());
//         DetectorMerged.Result det = detectorMerged.detect(bmp);

//         // 4) Log detections
//         for (BoundingBox d : det.detections) {
//             String lbl = detectorMerged.labels.get(d.cls);
//             Log.i(TAG, String.format(
//                     "→ %s @ [%.1f,%.1f→%.1f,%.1f] score=%.2f",
//                     lbl, d.x1, d.y1, d.x2, d.y2, d.cnf
//             ));
//         }

//         // 5) Report counts via setAreaInfo(areaId, itemName, number)
//         for (Map.Entry<String,Integer> e : det.counts.entrySet()) {
//             String itemName = e.getKey();
//             int count       = e.getValue();
//             Log.i(TAG, "Reported area " + idx + ": " + itemName + " × " + count);
//             if (itemName.equals("emerald") || itemName.equals("diamond") || itemName.equals("crystal")) continue;
//             api.setAreaInfo(idx, itemName, count);
//         }

//     }

//     private void sleep(int ms) {
//         try { Thread.sleep(ms); }
//         catch (InterruptedException ignored) {}
//     }

//     @Override
//     public void onDestroy() {
//         if (detectorMerged != null) detectorMerged.close();
//         super.onDestroy();
//     }

//     /** Convert BGR Mat → ARGB_8888 Bitmap */
//     private Bitmap matToBitmap(Mat mat) {
//         Mat rgb = new Mat();
//         Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
//         Bitmap bmp = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Config.ARGB_8888);
//         org.opencv.android.Utils.matToBitmap(rgb, bmp);
//         return bmp;
//     }

//     @Override protected void runPlan2() { }
//     @Override protected void runPlan3() { }
// }


package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.nfc.Tag;
import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YourService extends KiboRpcService {
    private static final String TAG = "YourService__HAHA";
    private DetectorMerged detector;
    Map<String, HashMap<Object, Object>> gemMap = new HashMap<>();
    private String Treasure;
    Map<Integer,String>Landmark=new HashMap<>();

    private final double[][] place = {
            {10.95,-9.85,5.195},
            {10.925,-8.875,4.52},
            {10.942,-7.85,4.51},
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
        Log.i(TAG, "RUN1 start");



        detector = new DetectorMerged(getApplicationContext(), "best_float32.tflite", "labelmap.txt");


        api.startMission();

        // Move through each area, capture and detect


        /* **************************************************** */
        /* Let's move to 1 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 1st area, capture and detect start");

        Point p = new Point(place[0][0], place[0][1], place[0][2]);
        Quaternion q = new Quaternion(
                angle[0][0], angle[0][1], angle[0][2], angle[0][3]
        );
        move(p, q, 1);
        Bitmap image=getImg(1);
        //sleep(50);
        detect(image,1);
        Log.i(TAG, "================Move through 1st area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 2 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 2nd area, capture and detect start");

        Point p1 = new Point(place[1][0], place[1][1], place[1][2]);
        Quaternion q1 = new Quaternion(
                angle[1][0], angle[1][1], angle[1][2], angle[1][3]
        );
        move(p1, q1, 2);
        Bitmap image1=getImg(2);
        //sleep(50);
        detect(image1,2);
        Log.i(TAG, "================Move through 2nd area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 3 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 3rd area, capture and detect start");

        Point p2 = new Point(place[2][0], place[2][1], place[2][2]);
        Quaternion q2 = new Quaternion(
                angle[2][0], angle[2][1], angle[2][2], angle[2][3]
        );
        move(p2, q2, 3);
        Bitmap image2=getImg(3);
        //sleep(50);
        detect(image2,3);
        Log.i(TAG, "================Move through 3rd area, capture and detect success======");


        /* **************************************************** */
        /* Let's move to 4 area and recognize the items. */
        /* **************************************************** */
        Log.i(TAG, "Move through 4th area, capture and detect start");

        Point p3 = new Point(place[3][0], place[3][1], place[3][2]);
        Quaternion q3 = new Quaternion(
                angle[3][0], angle[3][1], angle[3][2], angle[3][3]
        );
        move(p3, q3, 4);
        Bitmap image3=getImg(4);
        //sleep(50);
        detect(image3,4);
        Log.i(TAG, "================Move through 4th area, capture and detect success======");


        // Final position in front of astronaut
        Point p5 = new Point(11.143, -6.7607, 4.9654);
        Quaternion q5 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        move(p5, q5, 5);

        //=============================
        api.reportRoundingCompletion();
        //===============================
        sleep(2500);
        Bitmap image4=getImg(5);
        sleep(50);
        detect(image4,5);
        int i=3;

        while(!isTreasureItem(Treasure)&&i!=0) {
            i--;
                sleep(1000);
                image4=getImg(5);
                detect(image4,5);
        }

        Integer Area = null; // Use Integer instead of int to allow null checking
        HashMap<Object, Object> treasureData = gemMap.get(Treasure);

        if (treasureData != null) {
            // Iterate over the entries of the inner map
            for (Map.Entry<Object, Object> entry : treasureData.entrySet()) {
                int currentIdx = (Integer)entry.getKey();  // This is the idx (key)
                Object currentElement = entry.getValue();  // This is the element (value)
                Log.i(TAG,"ID: " + currentIdx + ", Element: " + currentElement);
                if(Landmark.get(currentIdx)==currentElement){
                    Area=currentIdx;
                    break;
                }
            }
        } else {
            Log.i(TAG,"No data found for treasure.");
        }


        /* ********************************************************** */
        /* Write your code to recognize which target item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        if(Area!=null) {
            Point last = new Point(place[Area - 1][0], place[Area - 1][1], place[Area - 1][2]);
            Quaternion lastAngle = new Quaternion(
                    angle[Area - 1][0], angle[Area - 1][1], angle[Area - 1][2], angle[Area - 1][3]
            );
            move(last, lastAngle , 6);
        }


        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();


        Log.i(TAG, "RUN1 shesh");

    }

    /**
     * Moves, captures, runs detection, and reports via setAreaInfo.
     * @param idx used both in filename and as areaId for reporting
     */
    private void move(Point point, Quaternion quaternion, int idx) {
        Log.i("MOVE", "Attempting to move to, Point:" + point + "; Quaternion: "+quaternion);

        if(point == null || quaternion == null){
            Log.d("MOVE", "Count not move, params are null");
            return;
        }

        int retryCount = 0;
        Result result;
        do {
            result = api.moveTo(point, quaternion, true);

            if(result.hasSucceeded()){
                sleep(1000);

                return;
            }
            retryCount++;

            Log.d("MOVE","Move attempt :" + retryCount);

            sleep(1000);
        } while(!result.hasSucceeded() && retryCount < 3);

        Log.d("MOVE", "Move to target failed");
        return;
    }


    private void detect(Bitmap image,int idx){
        Log.i(TAG,"starting detection , idx "+idx+" =========>>>>>>>>>>");
        // 3) Convert to Bitmap and detect
        api.saveBitmapImage(image,"bitmap_"+idx+".jpg");
        //=============================
        DetectorMerged.Result det = detector.detect(image);

        // 4) Log detections
        for (BoundingBox d : det.detections) {
            String lbl = detector.labels.get(d.cls);
            Log.i(TAG, String.format(
                    "→ %s @ [%.1f,%.1f→%.1f,%.1f] score=%.2f",
                    lbl, d.x1, d.y1, d.x2, d.y2, d.cnf
            ));
        }

        boolean treasure=false;
        boolean found=false;
        String treasure_name = null,element = null;
        int item_count=0;

        // 5) Report counts via setAreaInfo(areaId, itemName, number)
        for (Map.Entry<String,Integer> e : det.counts.entrySet()) {
            String itemName = e.getKey();
            int count       = e.getValue();
            Log.i(TAG,"calling setAreaInfo ()^^^^^^^^^^^^^^^^^^");
            treasure=isTreasureItem(itemName);
            if(treasure){
                treasure_name=itemName;
                found=true;
            }else{
                element=itemName;
                item_count=count;
            }
            Log.i(TAG, "Reported area " + idx + ": " + itemName + " × " + count);
        }
        api.setAreaInfo(idx, element, item_count);
        if(found&&treasure_name!=null&&idx!=5&&element != null){
            // Initialize the inner map if it doesn't exist for the given gem name (e.g., "diamond")
            gemMap.putIfAbsent(treasure_name, new HashMap<>()); // This will only put the inner map if it doesn't exist

            // Now put the ID and element into the inner map
            gemMap.get(treasure_name).put(idx, element);
        }
        if(idx!=5){
            Landmark.put(idx,element);
        }
        Log.i(TAG,"the map for id "+idx +" is=>=>=>=>=>=> " + String.valueOf(gemMap));
        Log.i(TAG,"====>>==>>>the LandMark element is "+String.valueOf(Landmark));
        if(idx==5){
            Treasure=treasure_name;
            Log.i(TAG,"treasure of astronaut is : "+Treasure);
        }

    }

    public boolean isTreasureItem(String itemName) {
        return itemName.equals("crystal") || itemName.equals("diamond") || itemName.equals("emerald");
    }

    public Bitmap getImg(int i){
        Log.i(TAG,"starting image processing , idx "+i+" =========>>>>>>>>>>");
        Mat image=api.getMatNavCam();


        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat> corners=new ArrayList<>();
        Mat markerId=new Mat();
        Aruco.detectMarkers(image,dictionary,corners,markerId);
//        markerIds[i]=new Mat();
//        markerIds[i] = markerId.clone();
        Log.i(TAG,i+" no Marker ID is : "+markerId);
        Log.i(TAG,"marker id is >>>>>>>>>>>>>>>>> "+markerId.dump());
        api.saveMatImage(image,"captured_raw_"+i+".jpg");

        //Get camera matrix
        Mat cameraMatrix=new Mat(3,3, CvType.CV_64F);
        cameraMatrix.put(0,0,api.getNavCamIntrinsics()[0]);

        //Get Lens distortion parameters
        Mat cameraCoefficient=new Mat(1,5,CvType.CV_64F);
        cameraCoefficient.put(0,0,api.getNavCamIntrinsics()[1]);
        cameraCoefficient.convertTo(cameraCoefficient,CvType.CV_64F);

        //Undistort image
        Mat undistorting=new Mat();
        Calib3d.undistort(image,undistorting,cameraMatrix,cameraCoefficient);
        api.saveMatImage(image,"undistort_"+i+".jpg");


        // 6. Estimate pose (only if markers were found)
//        if (!markerId.empty()) {
//            Mat rvecs = new Mat();  // rotation vectors
//            Mat tvecs = new Mat();  // translation vectors
//
//            float markerLength = 0.05f; // meters (adjust based on your real marker size)
//
//            Aruco.estimatePoseSingleMarkers(corners, markerLength, cameraMatrix, cameraCoefficient, rvecs, tvecs);
//
//            for (int idx = 0; idx < markerId.rows(); idx++) {
//                // 7. Get position
//                double[] tvec = new double[3];
//                tvecs.get(idx, 0, tvec); // tx, ty, tz
//
//                // >>> ADDED: Calculate Euclidean distance
//                double distance = Math.sqrt(tvec[0]*tvec[0] + tvec[1]*tvec[1] + tvec[2]*tvec[2]);
//                Log.i(TAG, String.format("Marker real Tag %d -> Distance from camera: %.4f meters",
//                        (int) markerId.get(idx, 0)[0], distance));
//
//
//                // 8. Convert rotation vector to rotation matrix
//                Mat rvec = new Mat(1, 3, CvType.CV_64F);
//                rvecs.row(idx).copyTo(rvec);
//                Mat rotationMatrix = new Mat();
//                Calib3d.Rodrigues(rvec, rotationMatrix);
//
//                // >>> ADDED: Calculate angle of rotation (in degrees)
//                double[] rotVec = new double[3];
//                rvec.get(0, 0, rotVec);
//                double angleRad = Math.sqrt(rotVec[0]*rotVec[0] + rotVec[1]*rotVec[1] + rotVec[2]*rotVec[2]);
//                double angleDeg = Math.toDegrees(angleRad);
//                Log.i(TAG, String.format("Marker real Tag %d -> Rotation angle: %.2f degrees",
//                        (int) markerId.get(idx, 0)[0], angleDeg));
//
//                // 9. Convert rotation matrix to quaternion
//                double[] R = new double[9];
//                rotationMatrix.get(0, 0, R);
//                double r00 = R[0], r01 = R[1], r02 = R[2];
//                double r10 = R[3], r11 = R[4], r12 = R[5];
//                double r20 = R[6], r21 = R[7], r22 = R[8];
//
//                double qw = Math.sqrt(1.0 + r00 + r11 + r22) / 2.0;
//                double qx = (r21 - r12) / (4.0 * qw);
//                double qy = (r02 - r20) / (4.0 * qw);
//                double qz = (r10 - r01) / (4.0 * qw);
//
//                // 10. Log position and orientation (quaternion)
//                Log.i(TAG, String.format("Marker real Tag %d -> Position (x,y,z): [%.4f, %.4f, %.4f]",
//                        (int) markerId.get(idx, 0)[0], tvec[0], tvec[1], tvec[2]));
//                Log.i(TAG, String.format("Marker real Tag %d -> Quaternion (x,y,z,w): [%.4f, %.4f, %.4f, %.4f]",
//                        (int) markerId.get(idx, 0)[0], qx, qy, qz, qw));
//            }
//        } else {
//            Log.i(TAG, "No marker detected.");
//        }


        Mat rgb = new Mat();
        Imgproc.cvtColor(image, rgb, Imgproc.COLOR_BGR2RGB);
        Bitmap bitmap = Bitmap.createBitmap(rgb.cols(), rgb.rows(), Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(rgb,bitmap);

        Log.i(TAG,"Returning bitmap image for , idx "+i+" =========>>>>>>>>>>");

        return bitmap;
    }

    private void sleep(int milliseconds) {
        Log.d("WAIT", "Pausing for: "+milliseconds+"ms");
        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            Log.d("WAIT", "Error Pausing for: "+milliseconds+"ms");
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void onDestroy() {
        if (detector != null) detector.close();
        super.onDestroy();
    }

    @Override protected void runPlan2() { }
    @Override protected void runPlan3() { }
}
