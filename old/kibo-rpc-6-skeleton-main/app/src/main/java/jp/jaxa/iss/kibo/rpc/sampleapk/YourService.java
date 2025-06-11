package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.ArTagDetectionData;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.Constants;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.QuaternionPoint;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.enumeration.AreaEnum;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee.
 */

public class YourService extends KiboRpcService {
    private MovementService movementService;
    private VisionService visionService;
    private AreaProcessor areaProcessor;

    @Override
    protected void runPlan1(){
        movementService = new MovementService(api);
        visionService = new VisionService(api, movementService);
        areaProcessor = new AreaProcessor(api, movementService, visionService);

        // The mission starts
        api.startMission();

        /* **************************************************** */
        /* Let's move to each area and recognize the items. */
        /* **************************************************** */

        // Move Astrobee from KIZ2 into KIZ1
        Log.d("MISSION_START", "Moving out of KIZ2");
        Point kiz1EntryPoint = new Point(10.292d, -10d, 4.6d);
        Quaternion kiz1EntryQuaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        movementService.moveToTargetPosition(kiz1EntryPoint, kiz1EntryQuaternion);

        Point kiz1Point = new Point(10.7d, -10d, 4.6d);
        Quaternion kiz1Quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        movementService.moveToTargetPosition(kiz1Point, kiz1Quaternion);


        // ArTag data per area
        Map<AreaEnum, ArTagDetectionData> detections = new HashMap<>();

        // move to the areas and collect ar tag data
        for (AreaEnum area : Constants.AREA_LIST) {
            try {
                // look up the coordinates
                QuaternionPoint areaCoordinates = Constants.AREA_COORDINATES_MAP.get(area);

                // process search areas (move, capture, undistort)
                Mat searchImage = areaProcessor.processSearchArea(
                        area,
                        areaCoordinates.getPoint(),
                        areaCoordinates.getQuaternion()
                );

                // read ar tags
                List<Mat> corners = new ArrayList<>();
                Mat ids = new Mat();
                visionService.readArTag(searchImage, corners, ids, area);

                // store ar tag data
                if (ids.total() > 0) {
                    detections.put(area, new ArTagDetectionData(corners, ids));
                }
            }
            catch (Exception e) {
                Log.e("RUN_PLAN1", "Error processing " + area + ", skipping to next", e);
            }
        }

        /* ******************************************************************************** */
        /* Write your code to recognize the type and number of landmark items in each area! */
        /* If there is a treasure item, remember it.                                        */
        /* ******************************************************************************** */

        // When you recognize landmark items, let’s set the type and number.
//        api.setAreaInfo(1, "item_name", 1);


        // When you move to the front of the astronaut, report the rounding completion
        Point astronautPoint = new Point(9.866984d, -6.7d, 5d);
        Quaternion astronautQuaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        movementService.moveToTargetPosition(astronautPoint, astronautQuaternion);

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
}
