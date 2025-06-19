package jp.jaxa.iss.kibo.rpc.sampleapk;

//import android.content.Context;
//import android.graphics.Bitmap;
//import android.os.SystemClock;
//import android.util.Log;
//
//import org.tensorflow.lite.DataType;
//import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.support.common.ops.CastOp;
//import org.tensorflow.lite.support.common.ops.NormalizeOp;
//import org.tensorflow.lite.support.image.ImageProcessor;
//import org.tensorflow.lite.support.image.TensorImage;
//import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
//import org.tensorflow.lite.support.common.FileUtil;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.RandomAccessFile;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//import java.util.*;
//
//
//public class DetectorMerged {
//
//    private static final String TAG = "Detector";
//
//    private Context context;
//    private String modelPath;
//    private String labelPath;
//
//    private Interpreter interpreter;
//    List<String> labels = new ArrayList<>();
//
//    private int tensorWidth = 0;
//    private int tensorHeight = 0;
//    private int numChannel = 0;
//    private int numElements = 0;
//
//    private static final float INPUT_MEAN = 0f;
//    private static final float INPUT_STANDARD_DEVIATION = 255f;
//    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
//    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
//    private static final float CONFIDENCE_THRESHOLD = 0.3f;
//    private static final float IOU_THRESHOLD = 0.5f;
//
//    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
//            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
//            .add(new CastOp(INPUT_IMAGE_TYPE))
//            .build();
//
////    public DetectorMerged(Context context, String modelPath, String labelPath) {
////        this.context = context;
////        this.modelPath = modelPath;
////        this.labelPath = labelPath;
////
////        Interpreter.Options options = new Interpreter.Options();
////        options.setNumThreads(4);
////
////        try {
////            loadModel(context, modelPath);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////    }
//public DetectorMerged(Context context, String modelPath, String labelPath) {
//    this.context = context;
//    this.modelPath = modelPath;
//    this.labelPath = labelPath;
//
//    Interpreter.Options options = new Interpreter.Options();
//    options.setNumThreads(4);
//
//    try {
//        interpreter = new Interpreter(FileUtil.loadMappedFile(context, modelPath), options);
//
//        int[] inputShape = interpreter.getInputTensor(0).shape();
//        int[] outputShape = interpreter.getOutputTensor(0).shape();
//
//        tensorWidth = inputShape[1];
//        tensorHeight = inputShape[2];
//        if (inputShape[1] == 3) {
//            tensorWidth = inputShape[2];
//            tensorHeight = inputShape[3];
//        }
//
//        numChannel = outputShape[1];
//        numElements = outputShape[2];
//
//        InputStream inputStream = context.getAssets().open(labelPath);
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//        String line;
//        while ((line = reader.readLine()) != null && !line.isEmpty()) {
//            labels.add(line);
//        }
//
//        reader.close();
//        inputStream.close();
//
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
//}
//
//    private void loadModel(Context context, String modelPath) throws IOException {
//        MappedByteBuffer modelBuffer = loadModelFile(getFileFromAsset(context, modelPath));
//        interpreter = new Interpreter(modelBuffer);
//    }
//
//    private static MappedByteBuffer loadModelFile(File modelFile) throws IOException {
//        try (RandomAccessFile raf = new RandomAccessFile(modelFile, "r")) {
//            FileChannel channel = raf.getChannel();
//            return channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
//        }
//    }
//
//    private static File getFileFromAsset(Context context, String assetFileName) throws IOException {
//        // Get the directory where the file will be copied
//        File fileDir = context.getFilesDir();
//        File file = new File(fileDir, assetFileName);
//
//        // Check if the file already exists, if not, copy it from assets
//        if (!file.exists()) {
//            copyAssetFileToInternalStorage(context, assetFileName, file);
//        }
//        return file;
//    }
//
//    private static void copyAssetFileToInternalStorage(Context context, String assetFileName, File outFile) throws IOException {
//        InputStream in = context.getAssets().open(assetFileName);
//        OutputStream out = new FileOutputStream(outFile);
//        byte[] buffer = new byte[1024];
//        int read;
//        while ((read = in.read(buffer)) != -1) {
//            out.write(buffer, 0, read);
//        }
//        in.close();
//        out.flush();
//        out.close();
//    }
//
//    public void restart(boolean isGpu) {
//        interpreter.close();
//
//        Interpreter.Options options = new Interpreter.Options();
//        options.setNumThreads(4);
//
//        try {
//            interpreter = new Interpreter(FileUtil.loadMappedFile(context, modelPath), options);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    public void close() {
//        interpreter.close();
//    }
//
//    public Result detect(Bitmap frame) {
//        Log.d(TAG, "DetectorMerged.detect() called");
//
//        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) {
//            Log.w(TAG, "Tensor dimensions not initialized: width=" + tensorWidth + ", height=" + tensorHeight
//                    + ", channels=" + numChannel + ", elements=" + numElements);
//            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
//        }
//
//        Log.d(TAG, "Running detection with image size: " + tensorWidth + "x" + tensorHeight);
//
//        long inferenceTime = SystemClock.uptimeMillis();
//
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false);
//        Log.i(TAG, "Resized to: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());
//        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
//        tensorImage.load(resizedBitmap);
//        TensorImage processedImage = imageProcessor.process(tensorImage);
//        Log.i(TAG, "Processed tensor shape: " + Arrays.toString(processedImage.getTensorBuffer().getShape()));
//
//        TensorBuffer output = TensorBuffer.createFixedSize(
//                new int[]{1, numChannel, numElements},
//                OUTPUT_IMAGE_TYPE
//        );
//
//        Log.i(TAG, "Output shape: " + Arrays.toString(output.getShape()));
//        float[] out = output.getFloatArray();
//        Log.i(TAG, "Output sample (first 20 floats): " + Arrays.toString(Arrays.copyOfRange(out, 0, 20)));
////        Log.d(TAG, "Output sample: " + Arrays.toString(Arrays.copyOfRange(outputArray, 0, 10)));
//
//        interpreter.run(processedImage.getBuffer(), output.getBuffer());
//        Log.i(TAG, "Ran inference. Output shape: [1," + numChannel + "," + numElements + "]");
//
//
//        List<BoundingBox> bestBoxes = bestBox(output.getFloatArray());
//        Log.i(TAG, "Boxes after filtering: " + (bestBoxes == null ? 0 : bestBoxes.size()));
//        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
//
//
//        if (bestBoxes == null) {
//            Log.w(TAG, "No boxes passed confidence threshold.");
//            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
//        }
//
//        Log.d(TAG, "Detected boxes after NMS: " + bestBoxes.size());
//
//        Map<String, Integer> classCounts = new HashMap<>();
//        for (BoundingBox box : bestBoxes) {
//            classCounts.put(box.clsName, classCounts.getOrDefault(box.clsName, 0) + 1);
//        }
//
//        return new Result(bestBoxes, classCounts);
//    }
//
//    private List<BoundingBox> bestBox(float[] array) {
//        List<BoundingBox> boundingBoxes = new ArrayList<>();
//
//        for (int c = 0; c < numElements; c++) {
//            float maxConf = CONFIDENCE_THRESHOLD;
//            int maxIdx = -1;
//            int j = 4;
//            int arrayIdx = c + numElements * j;
//            while (j < numChannel) {
//                if (array[arrayIdx] > maxConf) {
//                    maxConf = array[arrayIdx];
//                    maxIdx = j - 4;
//                }
//                j++;
//                arrayIdx += numElements;
//            }
//
//            if (maxConf > CONFIDENCE_THRESHOLD) {
//                String clsName = labels.get(maxIdx);
//                float cx = array[c];
//                float cy = array[c + numElements];
//                float w = array[c + numElements * 2];
//                float h = array[c + numElements * 3];
//                float x1 = cx - (w / 2f);
//                float y1 = cy - (h / 2f);
//                float x2 = cx + (w / 2f);
//                float y2 = cy + (h / 2f);
//                if (x1 < 0f || x1 > 1f || y1 < 0f || y1 > 1f || x2 < 0f || x2 > 1f || y2 < 0f || y2 > 1f) {
//                    Log.d(TAG, "Rejected box (out of bounds): " + clsName +
//                            " box=[" + x1 + "," + y1 + " → " + x2 + "," + y2 + "]");
//                    continue;
//                }
//
//
//                Log.d(TAG, "Accepted box: cls=" + clsName + " score=" + maxConf +
//                        " box=[" + x1 + "," + y1 + " → " + x2 + "," + y2 + "]");
//
//                Log.d(TAG, "Box: " + clsName + ", conf=" + maxConf);
//                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName));
//            }
//        }
//
//        Log.i(TAG, "Total accepted boxes: " + boundingBoxes.size());
//
//        if (boundingBoxes.isEmpty()) return null;
//
//        return applyNMS(boundingBoxes);
//    }
//
//    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
//        List<BoundingBox> sortedBoxes = new ArrayList<>(boxes);
//        Collections.sort(sortedBoxes, new Comparator<BoundingBox>() {
//            @Override
//            public int compare(BoundingBox o1, BoundingBox o2) {
//                return Float.compare(o2.cnf, o1.cnf);
//            }
//        });
//
//        List<BoundingBox> selectedBoxes = new ArrayList<>();
//        while (!sortedBoxes.isEmpty()) {
//            BoundingBox first = sortedBoxes.remove(0);
//            selectedBoxes.add(first);
//
//            Iterator<BoundingBox> iterator = sortedBoxes.iterator();
//            while (iterator.hasNext()) {
//                BoundingBox nextBox = iterator.next();
//                if (calculateIoU(first, nextBox) >= IOU_THRESHOLD) {
//                    iterator.remove();
//                }
//            }
//        }
//
//        return selectedBoxes;
//    }
//
//    private float calculateIoU(BoundingBox box1, BoundingBox box2) {
//        float x1 = Math.max(box1.x1, box2.x1);
//        float y1 = Math.max(box1.y1, box2.y1);
//        float x2 = Math.min(box1.x2, box2.x2);
//        float y2 = Math.min(box1.y2, box2.y2);
//        float intersectionArea = Math.max(0f, x2 - x1) * Math.max(0f, y2 - y1);
//        float box1Area = box1.w * box1.h;
//        float box2Area = box2.w * box2.h;
//        return intersectionArea / (box1Area + box2Area - intersectionArea);
//    }
//
//    public String getLabelByIndex(int index) {
//        if (index >= 0 && index < labels.size()) {
//            return labels.get(index);
//        } else {
//            return "Unknown";
//        }
//    }
//
//    // Internal Result class
//    public static class Result {
//        public final List<BoundingBox> detections;
//        public final Map<String, Integer> counts;
//
//        public Result(List<BoundingBox> detections, Map<String, Integer> counts) {
//            this.detections = detections;
//            this.counts = counts;
//        }
//
//    }
//}


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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;


public class DetectorMerged {

    private static final String TAG = "Detector";

    private Context context;
    private String modelPath;
    private String labelPath;

    private Interpreter interpreter;
    public List<String> labels;

    private int tensorWidth = 0;
    private int tensorHeight = 0;
    private int numChannel = 0;
    private int numElements = 0;

    private static final float INPUT_MEAN = 0f;
    private static final float INPUT_STANDARD_DEVIATION = 255f;
    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
    //private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float CONFIDENCE_THRESHOLD = 0.4f;
    private static final float IOU_THRESHOLD = 0.5f;

    private ImageProcessor imageProcessor;

    public DetectorMerged(Context context, String modelPath, String labelPath) {
        this.context = context;
        this.modelPath = modelPath;
        this.labelPath = labelPath;
        this.labels = new ArrayList<String>();

        // Initialize ImageProcessor
        this.imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
                .add(new CastOp(INPUT_IMAGE_TYPE))
                .build();

        Log.d(TAG, "initialize interpereur: " );
        initializeInterpreter();
        loadLabels();
    }

    private void initializeInterpreter() {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        try {
            ByteBuffer modelBuffer =loadModel(context,modelPath);
            interpreter = new Interpreter(modelBuffer, options);

            // Get input tensor shape
            int[] inputShape = interpreter.getInputTensor(0).shape();
            Log.d(TAG, "Output tensor shapee: " + Arrays.toString(inputShape));

            // Get output tensor shape
            int[] outputShape = interpreter.getOutputTensor(0).shape();
            Log.d(TAG, "Output tensor shape: " + Arrays.toString(outputShape));

            // Handle different input formats
            if (inputShape.length == 4) {
                // Format: [batch, height, width, channels] or [batch, channels, height, width]
                if (inputShape[1] == 3 || inputShape[1] == 1) {
                    // NCHW format: [batch, channels, height, width]
                    tensorHeight = inputShape[2];
                    tensorWidth = inputShape[3];
                } else {
                    // NHWC format: [batch, height, width, channels]
                    tensorHeight = inputShape[1];
                    tensorWidth = inputShape[2];
                }
            }

            // Handle output shape
            if (outputShape.length >= 3) {
                numChannel = outputShape[1];
                numElements = outputShape[2];
            }

            Log.d(TAG, "Tensor dimensions - Width: " + tensorWidth + ", Height: " + tensorHeight +
                    ", Channels: " + numChannel + ", Elements: " + numElements);

        } catch (IOException e) {
            Log.e(TAG, "Failed to load model", e);
            e.printStackTrace();
        }

    }


    private MappedByteBuffer loadModel(Context context, String modelPath) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(getFileFromAsset(context, modelPath));
        return modelBuffer;
}
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

    private void loadLabels() {
        try {
            InputStream inputStream = context.getAssets().open(labelPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
                labels.add(line.trim());
            }

            reader.close();
            inputStream.close();

            Log.d(TAG, "Loaded " + labels.size() + " labels");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load labels", e);
            e.printStackTrace();
        }
    }

    public void restart(boolean isGpu) {
        if (interpreter != null) {
            interpreter.close();
        }

        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        try {
            ByteBuffer modelBuffer = FileUtil.loadMappedFile(context, modelPath);
            interpreter = new Interpreter(modelBuffer, options);
        } catch (IOException e) {
            Log.e(TAG, "Failed to restart interpreter", e);
            throw new RuntimeException(e);
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
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

        try {
            interpreter.run(processedImage.getBuffer(), output.getBuffer());
            Log.i(TAG, "Ran inference. Output shape: [1," + numChannel + "," + numElements + "]");
        } catch (Exception e) {
            Log.e(TAG, "Inference failed", e);
            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
        }

        float[] outputArray = output.getFloatArray();
        Log.i(TAG, "Output sample (first 20 floats): " + Arrays.toString(Arrays.copyOfRange(outputArray, 0, Math.min(20, outputArray.length))));

        List<BoundingBox> bestBoxes = bestBox(outputArray);
        Log.i(TAG, "Boxes after filtering: " + (bestBoxes == null ? 0 : bestBoxes.size()));

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
        Log.d(TAG, "Inference time: " + inferenceTime + "ms");

        if (bestBoxes == null || bestBoxes.isEmpty()) {
            Log.w(TAG, "No boxes passed confidence threshold.");
            return new Result(new ArrayList<BoundingBox>(), new HashMap<String, Integer>());
        }

        Log.d(TAG, "Detected boxes after NMS: " + bestBoxes.size());

        Map<String, Integer> classCounts = new HashMap<String, Integer>();
        for (BoundingBox box : bestBoxes) {
            String className = box.clsName;
            Integer currentCount = classCounts.get(className);
            if (currentCount == null) {
                currentCount = 0;
            }
            classCounts.put(className, currentCount + 1);
        }

        return new Result(bestBoxes, classCounts);
    }

    private List<BoundingBox> bestBox(float[] array) {
        List<BoundingBox> boundingBoxes = new ArrayList<BoundingBox>();

        for (int c = 0; c < numElements; c++) {
            float maxConf = CONFIDENCE_THRESHOLD;
            int maxIdx = -1;
            int j = 4;
            int arrayIdx = c + numElements * j;

            while (j < numChannel) {
                if (arrayIdx < array.length && array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx];
                    maxIdx = j - 4;
                }
                j++;
                arrayIdx += numElements;
            }

            if (maxConf > CONFIDENCE_THRESHOLD && maxIdx >= 0 && maxIdx < labels.size()) {
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

                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName));
            }
        }

        Log.i(TAG, "Total accepted boxes: " + boundingBoxes.size());

        if (boundingBoxes.isEmpty()) {
            return null;
        }

        return applyNMS(boundingBoxes);
    }

    private List<BoundingBox> applyNMS(List<BoundingBox> boxes) {
        List<BoundingBox> sortedBoxes = new ArrayList<BoundingBox>(boxes);
        Collections.sort(sortedBoxes, new Comparator<BoundingBox>() {
            @Override
            public int compare(BoundingBox o1, BoundingBox o2) {
                return Float.compare(o2.cnf, o1.cnf);
            }
        });

        List<BoundingBox> selectedBoxes = new ArrayList<BoundingBox>();
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
