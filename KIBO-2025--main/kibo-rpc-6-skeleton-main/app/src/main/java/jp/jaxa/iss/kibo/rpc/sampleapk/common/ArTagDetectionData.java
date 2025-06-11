package jp.jaxa.iss.kibo.rpc.sampleapk.common;

import org.opencv.core.Mat;

import java.util.List;

/**
 * @author Hashib Islam (#)
 * @since 2025-04-24
 */

public class ArTagDetectionData {
    private final List<Mat> corners;
    private final Mat ids;

    public ArTagDetectionData(List<Mat> corners, Mat ids) {
        this.corners = corners;
        this.ids     = ids;
    }

    public List<Mat> getCorners() {
        return corners;
    }

    public Mat getIds() {
        return ids;
    }
}
