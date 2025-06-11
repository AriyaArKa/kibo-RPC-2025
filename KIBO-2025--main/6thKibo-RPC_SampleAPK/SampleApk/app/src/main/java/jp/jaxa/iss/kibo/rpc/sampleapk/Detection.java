// File: app/src/main/java/jp/jaxa/iss/kibo/rpc/sampleapk/Detection.java
package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.graphics.RectF;

/** Simple POJO representing one detected box + class + score. */
public class Detection {
    public final RectF box;
    public final int    classIdx;
    public final float  score;

    public Detection(RectF box, int classIdx, float score) {
        this.box      = box;
        this.classIdx = classIdx;
        this.score    = score;
    }
}
