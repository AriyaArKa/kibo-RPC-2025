package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

//import java.util.Dictionary;
import java.util.ArrayList;
import java.util.List;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    private final String TAG = this.getClass().getSimpleName();

    private final double[][] place={
            {10.95,-9.85,5.195},
            {10.925,-8.875,4.52},
            {10.942,-7.75,4.51},
            {10.6,-6.852,4.94}
    };

    private final float[][] angle={
            {0,0,-0.707f,0.707f},
            //{0,0.707f,0,0.707f},
            {-0.0923f,0.7002f,-0.0923f,0.7002f},
            //{0,0.707f,0,0.707f},
            {0.1651f,0.6876f,0.1651f,0.6876f},
            {0,1f,0,0}
    };

    @Override
    protected void runPlan1(){
        Log.i(TAG,"Start mission ! ");

        // The mission starts.
        api.startMission();


        for(int i=0;i<4;i++){
            double x=place[i][0],y=place[i][1],z=place[i][2];
            float a=angle[i][0],b=angle[i][1],c=angle[i][2],d=angle[i][3];

            Point point=new Point(x,y,z);
            Quaternion q=new Quaternion(a,b,c,d);
            boolean ans=move(point,q,i);
            Log.i(TAG," moved to: "+point+" -> "+ans);
        }


            api.setAreaInfo(1, "something",1);



        /* **************************************************** */
        /* Let's move to each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
         Point point1 = new Point(11.143d, -6.7607d, 4.9654d);
        Quaternion quaternion1 = new Quaternion(0f, 0f, 0.707f, 0.707f);
        boolean ans=move(point1,quaternion1,5);
        //api.moveTo(point1, quaternion1, false);
        api.reportRoundingCompletion();

        /* ********************************************************** */
        /* Write your code to recognize which target item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();
    }

    @Override
    protected void runPlan2(){
       // write your plan 2 here.
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here.
    }

    // You can add your method.
    private String yourMethod(){
        return "your method";
    }


    public boolean move(Point point, Quaternion quaternion,int i){
        Log.i(TAG, "Attempting to move to, Point:" + point + "; Quaternion: "+quaternion);

        if(point == null || quaternion == null){
            Log.i(TAG, "Count not move, params are null");
            return false;
        }

        int retryCount = 0;
        Result result;
        do {
            result = api.moveTo(point, quaternion, true);

            if(result.hasSucceeded()){
                wait(500);
                Mat img=api.getMatNavCam();
                wait(10);
                api.saveMatImage(img,"img@"+i+".jpg");
                //ar_read_crop(img,i);

                return true;
            }
            retryCount++;

            Log.i(TAG,"Move attempt :" + retryCount);

            wait(500);
        } while(!result.hasSucceeded() && retryCount < 3);

        Log.i(TAG, "Move to target failed");
        return false;
    }

    public void wait(int milliseconds){
        Log.i(TAG, "Pausing for: "+milliseconds+"ms");

        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            Log.i(TAG, "Error Pausing for: "+milliseconds+"ms");
            e.printStackTrace();
        }

        return;
    }

    //Aruco tag detection
    public void ar_read_crop(Mat mat,int i){
        //Get camera matrix
        Mat cameraMatrix=new Mat(3,3, CvType.CV_64F);
        cameraMatrix.put(0,0,api.getNavCamIntrinsics()[0]);

        //Get lens distortion parameter
        Mat cameraCoefficient=new Mat(1,5,CvType.CV_64F);
        cameraCoefficient.put(0,0,api.getNavCamIntrinsics()[1]);
        cameraCoefficient.convertTo(cameraCoefficient,CvType.CV_64F);

        //Undistort image
        Mat undistorting=new Mat();
        Calib3d.undistort(mat,undistorting,cameraMatrix,cameraCoefficient);

        Dictionary dictionary= Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        List<Mat>corners=new ArrayList<>();
        Mat markerids=new Mat();
        Aruco.detectMarkers(mat,dictionary,corners,markerids);
        if (!markerids.empty()) {
            crop(mat, corners, markerids, i);
        } else {
            Log.i(TAG, "No ArUco markers detected in image " + i);
        }
    }

    public void crop(Mat img, List<Mat> corners, Mat Markerids, int i) {
        // I want to crop like a box which will have 32 centimeter height and width,
        // which is parallel to corners of the aruco tag
        // and the center of aruco tag is center of the box
        double x = 0,y =0;
        if (!corners.isEmpty()) {
            Mat cornerMat = corners.get(0); // Get the first marker's corner matrix (4x1x2)

            double[] tl = cornerMat.get(0, 0); // [x, y] // Get the top-left corner: index 0

            if (tl != null && tl.length >= 2) {
                 x =  tl[0];
                 y =  tl[1];

                Log.i(TAG, "Top-left corner: x = " + x + ", y = " + y);
            } else {
                Log.i(TAG, "Top-left corner not found or malformed.");
            }
        } else {
            Log.i(TAG, "No ArUco markers detected.");
            return;
        }
        x-=23;
        y-=23;
           Log.i(TAG,"top left of croped image : "+x+" "+y);
        // Define the crop rectangle: x, y, width, height
        Rect roi = new Rect((int)x, (int)y, 200, 150);

        // Crop the region
        Mat cropped = new Mat(img, roi);
        api.saveMatImage(cropped,"cropped_"+i+".png");
        return;

    }

}
