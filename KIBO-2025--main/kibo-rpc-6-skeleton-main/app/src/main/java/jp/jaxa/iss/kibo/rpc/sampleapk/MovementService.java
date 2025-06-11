package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.sampleapk.common.Constants;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class MovementService {
    private final KiboRpcApi api;

    public MovementService(KiboRpcApi api) {
        this.api = api;
    }

    /**
     * Attempts to move Astrobee to the specified position and orientation.
     * Retries the movement up to MAX_RETRIES times if it fails.
     *
     * @param point the target position as a Point
     * @param quaternion the target orientation as a Quaternion
     * @return true if the movement succeeds, false otherwise
     */
    public boolean moveToTargetPosition(Point point, Quaternion quaternion){
        Log.d("MOVE", "Attempting to move to, Point:" + point + "; Quaternion: "+quaternion);

        if(point == null || quaternion == null){
            Log.d("MOVE", "Count not move, params are null");
            return false;
        }

        int retryCount = 0;
        Result result;
        do {
            result = api.moveTo(point, quaternion, true);

            if(result.hasSucceeded()){
                wait(Constants.MOVE_TO_COMPLETE_WAIT_MS);

                return true;
            }
            retryCount++;

            Log.d("MOVE","Move attempt :" + retryCount);

            wait(Constants.RETRY_WAIT_MS);
        } while(!result.hasSucceeded() && retryCount < Constants.MAX_RETRIES);

        Log.d("MOVE", "Move to target failed");
        return false;
    }

    /**
     * Pauses the current thread for a specified number of milliseconds.
     * This is typically used to wait between hardware polling attempts or retries.
     *
     * @param milliseconds the amount of time to pause execution, in milliseconds
     */
    public void wait(int milliseconds){
        Log.d("WAIT", "Pausing for: "+milliseconds+"ms");

        try{
            Thread.sleep(milliseconds);
        }catch (InterruptedException e){
            Log.d("WAIT", "Error Pausing for: "+milliseconds+"ms");
            e.printStackTrace();
        }

        return;
    }
}
