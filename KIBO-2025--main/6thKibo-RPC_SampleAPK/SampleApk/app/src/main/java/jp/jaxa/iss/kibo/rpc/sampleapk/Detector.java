package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector class: loads a YOLOv8 TFLite model, runs inference with top-K pre-NMS,
 * per-class NMS, and returns both detections and per-class counts.
 */
public class Detector {
    private static final String TAG = "Detector";

    // Asset file names
    private static final String MODEL_FILENAME = "best_float32.tflite";
    private static final String LABELS_FILENAME = "labels.txt";

    // Post-processing thresholds
    private static final float CONFIDENCE_THRESHOLD = 0.75f;
    private static final float NMS_IOU_THRESHOLD = 0.45f;
    private static final int TOP_K_PRE_NMS = 2000;

    // Model input dimensions
    private static final int IMAGE_WIDTH = 640;
    private static final int IMAGE_HEIGHT = 640;

    private final Interpreter interpreter;
    public final List<String> labels;

    private final int outputHeadCount;
    private final int outputInnerSize;
    private final int predsPerHead;
    private final int floatsPerPrediction = 6;

    public Detector(Context context) throws IOException {

        // Load TFLite model from assets
//        MappedByteBuffer buffer = loadModelFile(context, MODEL_FILENAME);
//        interpreter = new Interpreter(buffer);

        MappedByteBuffer modelBuffer = loadModelFile(getFileFromAsset(context, MODEL_FILENAME));
        interpreter = new Interpreter(modelBuffer);


        // Load class labels from assets
        labels = FileUtil.loadLabels(context, LABELS_FILENAME);

        // Inspect output tensor shape: [1, headCount, innerSize]
        int[] shape = interpreter.getOutputTensor(0).shape();
        outputHeadCount = shape[1];
        outputInnerSize = shape[2];
        if (outputInnerSize % floatsPerPrediction != 0) {
            throw new IllegalStateException("Output inner size not divisible by " + floatsPerPrediction);
        }
        predsPerHead = outputInnerSize / floatsPerPrediction;

        Log.i(TAG, "Model loaded: heads=" + outputHeadCount +
                " innerSize=" + outputInnerSize +
                " preds/head=" + predsPerHead);
    }

//    private MappedByteBuffer loadModelFile(Context ctx, String filename) throws IOException {
//        AssetFileDescriptor afd = ctx.getAssets().openFd(filename);
//        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
//        FileChannel channel = fis.getChannel();
//        return channel.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
//    }

    //nayeem


    private static MappedByteBuffer loadModelFile(File modelFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(modelFile, "r")) {
            FileChannel channel = raf.getChannel();
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    private static File getFileFromAsset(Context context, String assetFileName) throws IOException {
        // Get the directory where the file will be copied
        File fileDir = context.getFilesDir();
        File file = new File(fileDir, assetFileName);

        // Check if the file already exists, if not, copy it from assets
        if (!file.exists()) {
            copyAssetFileToInternalStorage(context, assetFileName, file);
        }
        return file;
    }

    private static void copyAssetFileToInternalStorage(Context context, String assetFileName, File outFile) throws IOException {
        InputStream in = context.getAssets().open(assetFileName);
        OutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * Holder for detection results and per-class counts.
     */
    public static class Result {
        public final List<Detection> detections;
        public final Map<String, Integer> counts;

        public Result(List<Detection> det, Map<String, Integer> cnt) {
            this.detections = det;
            this.counts = cnt;
        }
    }

    /**
     * Runs inference + post-processing (top-K, NMS) and returns results.
     */
    public Result detect(Bitmap bitmap) {
        // Preprocess: resize & normalize
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true);
        ByteBuffer input = ByteBuffer.allocateDirect(IMAGE_WIDTH * IMAGE_HEIGHT * 3 * 4)
                .order(ByteOrder.nativeOrder());
        int[] pixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        scaled.getPixels(pixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        for (int p : pixels) {
            input.putFloat(((p >> 16) & 0xFF) / 255f);
            input.putFloat(((p >> 8) & 0xFF) / 255f);
            input.putFloat((p & 0xFF) / 255f);
        }
        input.rewind();

        // Inference
        float[][][] raw = new float[1][outputHeadCount][outputInnerSize];
        interpreter.run(input, raw);

        // Post-process
        List<Detection> all = new ArrayList<>();
        for (int h = 0; h < outputHeadCount; h++) {
            float[] head = raw[0][h];
            for (int i = 0; i < predsPerHead; i++) {
                int b = i * floatsPerPrediction;
                float score = head[b + 4];
                if (score < CONFIDENCE_THRESHOLD) continue;
                int cls = Math.round(head[b + 5]);
                if (cls < 0 || cls >= labels.size()) continue;
                float cx = head[b], cy = head[b + 1], w = head[b + 2], hgt = head[b + 3];
                float left = (cx - w / 2f) * IMAGE_WIDTH;
                float top = (cy - hgt / 2f) * IMAGE_HEIGHT;
                float right = (cx + w / 2f) * IMAGE_WIDTH;
                float bottom = (cy + hgt / 2f) * IMAGE_HEIGHT;
                RectF box = new RectF(left, top, right, bottom);
                all.add(new Detection(box, cls, score));
            }
        }

        // Top-K pre-NMS sort descending by score without lambdas
        Collections.sort(all, new Comparator<Detection>() {
            @Override
            public int compare(Detection a, Detection b) {
                return Float.compare(b.score, a.score);
            }
        });
        if (all.size() > TOP_K_PRE_NMS) {
            all = all.subList(0, TOP_K_PRE_NMS);
        }

        // Per-class NMS
        List<Detection> keep = new ArrayList<>();
        for (int c = 0; c < labels.size(); c++) {
            List<Detection> clsList = new ArrayList<>();
            for (Detection d : all) {
                if (d.classIdx == c) clsList.add(d);
            }
            if (clsList.isEmpty()) continue;
            // sort per-class
            Collections.sort(clsList, new Comparator<Detection>() {
                @Override
                public int compare(Detection a, Detection b) {
                    return Float.compare(b.score, a.score);
                }
            });
            // NMS loop
            List<Detection> clsKeep = new ArrayList<>();
            for (Detection d : clsList) {
                boolean drop = false;
                for (Detection k : clsKeep) {
                    if (iou(d.box, k.box) > NMS_IOU_THRESHOLD) {
                        drop = true;
                        break;
                    }
                }
                if (!drop) clsKeep.add(d);
            }
            keep.addAll(clsKeep);
        }

        // Build counts
        Map<String, Integer> counts = new HashMap<>();
        for (Detection d : keep) {
            String lbl = labels.get(d.classIdx);
            counts.put(lbl, counts.getOrDefault(lbl, 0) + 1);
        }
        return new Result(keep, counts);
    }

    private float iou(RectF a, RectF b) {
        float iw = Math.max(0, Math.min(a.right, b.right) - Math.max(a.left, b.left));
        float ih = Math.max(0, Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top));
        float inter = iw * ih;
        float union = a.width() * a.height() + b.width() * b.height() - inter;
        return union <= 0 ? 0 : (inter / union);
    }

    /**
     * Clean up native resources.
     */
    public void close() {
        interpreter.close();
    }
}
