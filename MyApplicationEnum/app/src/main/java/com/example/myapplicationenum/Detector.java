//package com.example.myapplicationenum;
//
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.os.SystemClock;
//import android.util.Log;
//
//import org.tensorflow.lite.DataType;
//import org.tensorflow.lite.Interpreter;
////import org.tensorflow.lite.gpu.CompatibilityList;
////import org.tensorflow.lite.gpu.GpuDelegate;
//import org.tensorflow.lite.support.common.FileUtil;
//import org.tensorflow.lite.support.common.ops.CastOp;
//import org.tensorflow.lite.support.common.ops.NormalizeOp;
//import org.tensorflow.lite.support.image.ImageProcessor;
//import org.tensorflow.lite.support.image.TensorImage;
//import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.List;
//
//public class Detector {
//
//    private Context context;
//    private String modelPath;
//    private String labelPath;
//    private DetectorListener detectorListener;
//
//    private Interpreter interpreter;
//    private List<String> labels = new ArrayList<>();
//
//    private int tensorWidth = 0;
//    private int tensorHeight = 0;
//    private int numChannel = 0;
//    private int numElements = 0;
//
//    private ImageProcessor imageProcessor = new ImageProcessor.Builder()
//            .add(new NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
//            .add(new CastOp(INPUT_IMAGE_TYPE))
//            .build();
//
//    public Detector(Context context, String modelPath, String labelPath, DetectorListener listener) {
//        this.context = context;
//        this.modelPath = modelPath;
//        this.labelPath = labelPath;
//        this.detectorListener = listener;
//
////        CompatibilityList compatList = new CompatibilityList();
////
////        Interpreter.Options options = new Interpreter.Options();
////        if (compatList.isDelegateSupportedOnThisDevice()) {
////            options.addDelegate(new GpuDelegate());
////        } else {
////            options.setNumThreads(4);
////        }
//        Interpreter.Options options = new Interpreter.Options();
//        options.setNumThreads(4);
//
//        try {
//            interpreter = new Interpreter(FileUtil.loadMappedFile(context, modelPath), options);
//
//            int[] inputShape = interpreter.getInputTensor(0).shape();
//            int[] outputShape = interpreter.getOutputTensor(0).shape();
//
//            if (inputShape != null) {
//                tensorWidth = inputShape[1];
//                tensorHeight = inputShape[2];
//                if (inputShape[1] == 3) {
//                    tensorWidth = inputShape[2];
//                    tensorHeight = inputShape[3];
//                }
//            }
//
//            if (outputShape != null) {
//                numChannel = outputShape[1];
//                numElements = outputShape[2];
//            }
//
//            InputStream inputStream = context.getAssets().open(labelPath);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            while ((line = reader.readLine()) != null && !line.isEmpty()) {
//                labels.add(line);
//            }
//
//            reader.close();
//            inputStream.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void restart(boolean isGpu) {
//        interpreter.close();
//
//        Interpreter.Options options = new Interpreter.Options();
////        if (isGpu) {
////            CompatibilityList compatList = new CompatibilityList();
////            if (compatList.isDelegateSupportedOnThisDevice()) {
////                options.addDelegate(new GpuDelegate());
////            } else {
////                options.setNumThreads(4);
////            }
////        } else {
////            options.setNumThreads(4);
////        }
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
//    public void detect(Bitmap frame) {
//        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return;
//
//        long inferenceTime = SystemClock.uptimeMillis();
//
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false);
//        TensorImage tensorImage = new TensorImage(INPUT_IMAGE_TYPE);
//        tensorImage.load(resizedBitmap);
//        TensorImage processedImage = imageProcessor.process(tensorImage);
//
//        TensorBuffer output = TensorBuffer.createFixedSize(
//                new int[]{1, numChannel, numElements},
//                OUTPUT_IMAGE_TYPE
//        );
//
//        Log.d("Detector", "Input tensor shape: " + Arrays.toString(interpreter.getInputTensor(0).shape()));
//        Log.d("Detector", "Input tensor datatype: " + interpreter.getInputTensor(0).dataType());
//
//        Log.d("Detector", "Output tensor shape: " + Arrays.toString(interpreter.getOutputTensor(0).shape()));
//        Log.d("Detector", "Output tensor datatype: " + interpreter.getOutputTensor(0).dataType());
//
//
//        interpreter.run(processedImage.getBuffer(), output.getBuffer());
//
//        List<BoundingBox> bestBoxes = bestBox(output.getFloatArray());
//        inferenceTime = SystemClock.uptimeMillis() - inferenceTime;
//
//        if (bestBoxes == null) {
//            detectorListener.onEmptyDetect();
//            return;
//        }
//
//        detectorListener.onDetect(bestBoxes, inferenceTime);
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
//                if (x1 < 0f || x1 > 1f || y1 < 0f || y1 > 1f || x2 < 0f || x2 > 1f || y2 < 0f || y2 > 1f)
//                    continue;
//
//                boundingBoxes.add(new BoundingBox(x1, y1, x2, y2, cx, cy, w, h, maxConf, maxIdx, clsName));
//            }
//        }
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
//    public interface DetectorListener {
//        void onEmptyDetect();
//        void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime);
//    }
//
//    private static final float INPUT_MEAN = 0f;
//    private static final float INPUT_STANDARD_DEVIATION = 255f;
//    private static final DataType INPUT_IMAGE_TYPE = DataType.FLOAT32;
//    private static final DataType OUTPUT_IMAGE_TYPE = DataType.FLOAT32;
//    private static final float CONFIDENCE_THRESHOLD = 0.3f;
//    private static final float IOU_THRESHOLD = 0.5f;
//}
