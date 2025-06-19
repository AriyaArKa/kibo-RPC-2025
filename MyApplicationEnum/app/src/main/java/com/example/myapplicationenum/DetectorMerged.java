package com.example.myapplicationenum;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


public class DetectorMerged {

    private static final String TAG = "Detector";

    private Context context;
    private String modelPath;
    private String labelPath;

    private Interpreter interpreter;
    List<String> labels = new ArrayList<>();

    private int tensorWidth = 0;
    private int tensorHeight = 0;
    private int numChannel = 0;
    private int numElements = 0;

    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float IOU_THRESHOLD = 0.5f;

    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(new CastOp(INPUT_IMAGE_TYPE))
            .build();

    public DetectorMerged(Context context, String modelPath, String labelPath) {
        this.context = context;
        this.modelPath = modelPath;
        this.labelPath = labelPath;

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(context, modelPath), options);

            int[] inputShape = interpreter.getInputTensor(0).shape();
            int[] outputShape = interpreter.getOutputTensor(0).shape();

            tensorWidth = inputShape[1];
            tensorHeight = inputShape[2];
            if (inputShape[1] == 3) {
                tensorWidth = inputShape[2];
                tensorHeight = inputShape[3];
            }

            numChannel = outputShape[1];
            numElements = outputShape[2];

            InputStream inputStream = context.getAssets().open(labelPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                labels.add(line);
            }

            reader.close();
            inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restart(boolean isGpu) {
        interpreter.close();

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        try {
            interpreter = new Interpreter(FileUtil.loadMappedFile(context, modelPath), options);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        interpreter.close();
    }

    public Result detect(Bitmap frame) {
        Log.d(TAG, "DetectorMerged.detect() called");

        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
            Log.w(TAG, "Tensor dimensions not initialized: width=" + tensorWidth + ", height=" + tensorHeight
                    + ", channels=" + numChannel + ", elements=" + numElements);
            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
        }

        Log.d(TAG, "Running detection with image size: " + tensorWidth + "x" + tensorHeight);

        long inferenceTime = SystemClock.uptimeMillis();

        Bitmap resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false);
        Log.i(TAG, "Resized to: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());
        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
        tensorImage.load(resizedBitmap);
        TensorImage processedImage = imageProcessor.process(tensorImage);
        Log.i(TAG, "Processed tensor shape: " + Arrays.toString(processedImage.getTensorBuffer().getShape()));

        TensorBuffer output = TensorBuffer.createFixedSize(
                new int[]{1, numChannel, numElements},
                OUTPUT_IMAGE_TYPE
        );

        Log.i(TAG, "Output shape: " + Arrays.toString(output.getShape()));
        float[] out = output.getFloatArray();
        Log.i(TAG, "Output sample (first 20 floats): " + Arrays.toString(Arrays.copyOfRange(out, 0, 20)));
//        Log.d(TAG, "Output sample: " + Arrays.toString(Arrays.copyOfRange(outputArray, 0, 10)));

        interpreter.run(processedImage.getBuffer(), output.getBuffer());
        Log.i(TAG, "Ran inference. Output shape: [1," + numChannel + "," + numElements + "]");


        List<BoundingBox> bestBoxes = bestBox(output.getFloatArray());
        Log.i(TAG, "Boxes after filtering: " + (bestBoxes == null ? 0 : bestBoxes.size()));
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;


        if (bestBoxes == null) {
            Log.w(TAG, "No boxes passed confidence threshold.");
            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
        }

        Log.d(TAG, "Detected boxes after NMS: " + bestBoxes.size());

        Map<String, Integer> classCounts = new HashMap<>();
        for (BoundingBox box : bestBoxes) {
            classCounts.put(box.clsName, classCounts.getOrDefault(box.clsName, 0) + 1);
        }

        return new Result(bestBoxes, classCounts);
    }

    private List<BoundingBox> bestBox(float[] array) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();

        for (int c = 0; c < numElements; c++) {
            float maxConf = CONFIDENCE_THRESHOLD;
            int maxIdx = -1;
            int j = 4;
            int arrayIdx = c + numElements * j;
            while (j < numChannel) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx];
                    maxIdx = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                String clsName = labels.get(maxIdx);
                float cx = array[c];
                float cy = array[c + numElements];
                float w = array[c + numElements * 2];
                float h = array[c + numElements * 3];
                float x1 = cx - (w / 2f);
                float y1 = cy - (h / 2f);
                float x2 = cx + (w / 2f);
                float y2 = cy + (h / 2f);
                if (x1 < 0f || x1 > 1f || y1 < 0f || y1 > 1f || x2 < 0f || x2 > 1f || y2 < 0f || y2 > 1f){
                    Log.d(TAG, "Rejected box (out of bounds): " + clsName +
                            " box=[" + x1 + "," + y1 + " → " + x2 + "," + y2 + "]");
                    continue;
                }


                Log.d(TAG, "Accepted box: cls=" + clsName + " score=" + maxConf +
                        " box=[" + x1 + "," + y1 + " → " + x2 + "," + y2 + "]");

                Log.d(TAG, "Box: " + clsName + ", conf=" + maxConf);
                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName));
            }
        }

        Log.i(TAG, "Total accepted boxes: " + boundingBoxes.size());

        if (boundingBoxes.isEmpty()) return null;

        return applyNMS(boundingBoxes);
    }

    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> sortedBoxes = new ArrayList<>(boxes);
        Collections.sort(sortedBoxes, new Comparator<BoundingBox>() {
            @Override
            public int compare(BoundingBox o1, BoundingBox o2) {
                return Float.compare(o2.cnf, o1.cnf);
            }
        });

        List<BoundingBox> selectedBoxes = new ArrayList<>();
        while (!sortedBoxes.isEmpty()) {
            BoundingBox first = sortedBoxes.remove(0);
            selectedBoxes.add(first);

            Iterator<BoundingBox> iterator = sortedBoxes.iterator();
            while (iterator.hasNext()) {
                BoundingBox nextBox = iterator.next();
                if (calculateIoU(first, nextBox) >= IOU_THRESHOLD) {
                    iterator.remove();
                }
            }
        }

        return selectedBoxes;
    }

    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
        float x1 = Math.max(box1.x1, box2.x1);
        float y1 = Math.max(box1.y1, box2.y1);
        float x2 = Math.min(box1.x2, box2.x2);
        float y2 = Math.min(box1.y2, box2.y2);
        float intersectionArea = Math.max(0f, x2 - x1) * Math.max(0f, y2 - y1);
        float box1Area = box1.w * box1.h;
        float box2Area = box2.w * box2.h;
        return intersectionArea / (box1Area + box2Area - intersectionArea);
    }

    public String getLabelByIndex(int index) {
        if (index >= 0 && index < labels.size()) {
            return labels.get(index);
        } else {
            return "Unknown";
        }
    }

    // Internal Result class
    public static class Result {
        public final List<BoundingBox> detections;
        public final Map<String, Integer> counts;

        public Result(List<BoundingBox> detections, Map<String, Integer> counts) {
            this.detections = detections;
            this.counts = counts;
        }


    }
}
