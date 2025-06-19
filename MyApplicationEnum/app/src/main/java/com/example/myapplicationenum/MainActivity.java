package com.example.myapplicationenum;

//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.myapplicationenum.R;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final int PICK_IMAGE_REQUEST = 100;
//    private ImageView imageView;
//    private Button selectImageButton;
//
//    private OverlayView overlayView;
//
//    private Detector detector;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        imageView = findViewById(R.id.imageView);
//        selectImageButton = findViewById(R.id.selectImageButton);
//        overlayView = findViewById(R.id.overlayView);
//
//        selectImageButton.setOnClickListener(view -> {
//            Intent intent = new Intent(Intent.ACTION_PICK);
//            intent.setType("image/*");
//            startActivityForResult(intent, PICK_IMAGE_REQUEST);
//        });
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
//            Uri selectedImageUri = data.getData();
//            try {
//                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
//                imageView.setImageBitmap(bitmap);
//
//                // Initialize Detector (only once in your activity, ideally)
//                if (detector == null) {
//                    detector = new Detector(
//                            this,
//                            "best_float32_krpc .tflite",  // your model file in assets folder
//                            "labelmap.txt",    // your labels file in assets folder
//                            new Detector.DetectorListener() {
//                                @Override
//                                public void onEmptyDetect() {
//                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No objects detected", Toast.LENGTH_SHORT).show());
//                                    overlayView.clear();
//                                }
//
//                                @Override
//                                public void onDetect(List<BoundingBox> boundingBoxes, long inferenceTime) {
//                                    overlayView.setResults(boundingBoxes);
//
//                                    // Map to count each detected class
//                                    Map<String, Integer> classCounts = new HashMap<>();
//                                    for (BoundingBox box : boundingBoxes) {
//                                        String label = box.clsName;
//                                        classCounts.put(label, classCounts.getOrDefault(label, 0) + 1);
//                                    }
//
//                                    for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
//                                        Log.d("Detector", "Detected " + entry.getValue() + " × " + entry.getKey());
//                                    }
//
//                                    // Optional toast (can comment out if you want)
//                                    StringBuilder toastMsg = new StringBuilder("Detected:\n");
//                                    for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
//                                        toastMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
//                                    }
//                                    Toast.makeText(MainActivity.this, toastMsg.toString().trim(), Toast.LENGTH_LONG).show();
//                                }
//                            }
//                    );
//                }
//
//                // Run detection
//                detector.detect(bitmap);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//}
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final String TAG = "MainActivity";

    private ImageView imageView;
    private Button selectImageButton;
    private OverlayView overlayView;
    private DetectorMerged detector;
    private ExecutorService detectionExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selectImageButton);
        overlayView = findViewById(R.id.overlayView);
        detectionExecutor = Executors.newSingleThreadExecutor();

        // Initialize detector once
        detector = new DetectorMerged(this, "best_float32_krpc .tflite", "labelmap.txt");

        selectImageButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                imageView.setImageBitmap(bitmap);

                // Run detection in background thread
                detectionExecutor.execute(() -> {
                    DetectorMerged.Result result = detector.detect(bitmap);
                    List<BoundingBox> boundingBoxes = result.detections;
                    Map<String, Integer> classCounts = result.counts;

                    runOnUiThread(() -> {
                        if (boundingBoxes == null || boundingBoxes.isEmpty()) {
                            overlayView.clear();
                            Toast.makeText(MainActivity.this, "No objects detected", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        overlayView.setResults(boundingBoxes);

                        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
                            Log.d(TAG, "Detected " + entry.getValue() + " × " + entry.getKey());
                        }

                        StringBuilder toastMsg = new StringBuilder("Detected:\n");
                        for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
                            toastMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                        Toast.makeText(MainActivity.this, toastMsg.toString().trim(), Toast.LENGTH_LONG).show();
                    });
                });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (detectionExecutor != null) detectionExecutor.shutdown();
        if (detector != null) detector.close();
    }
}
