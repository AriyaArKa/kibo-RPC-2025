//package jp.jaxa.iss.kibo.rpc.sampleapk;
//
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.net.Uri;
//import android.provider.MediaStore;
//import android.support.annotation.Nullable;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MainActivity extends AppCompatActivity{
//
//    private static final int PICK_IMAGE_REQUEST = 100;
//    private static final String TAG = "MainActivity";
//
//    private ImageView imageView;
//    private Button selectImageButton;
//    //    private OverlayView overlayView;
//    private DetectorMerged detector;
//    private ExecutorService detectionExecutor;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState){
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        imageView = findViewById(R.id.imageView);
//        selectImageButton = findViewById(R.id.selectImageButton);
////        overlayView = findViewById(R.id.overlayView);
//        detectionExecutor = Executors.newSingleThreadExecutor();
//
//        // Initialize detector once
//        detector = new DetectorMerged(this, "best_float32.tflite", "labelmap.txt");
//
//        selectImageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(Intent.ACTION_PICK);
//                intent.setType("image/*");
//                startActivityForResult(intent, PICK_IMAGE_REQUEST);
//            }
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
//                // Run detection in background thread
//                detectionExecutor.execute(new DetectionRunnable(bitmap));
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private class DetectionRunnable implements Runnable {
//        private Bitmap bitmap;
//
//        public DetectionRunnable(Bitmap bitmap) {
//            this.bitmap = bitmap;
//        }
//
//        @Override
//        public void run() {
//            DetectorMerged.Result result = detector.detect(bitmap);
//            List<BoundingBox> boundingBoxes = result.detections;
//            Map<String, Integer> classCounts = result.counts;
//
//            runOnUiThread(new UiUpdateRunnable(boundingBoxes, classCounts));
//        }
//    }
//
//    private class UiUpdateRunnable implements Runnable {
//        private List<BoundingBox> boundingBoxes;
//        private Map<String, Integer> classCounts;
//
//        public UiUpdateRunnable(List<BoundingBox> boundingBoxes, Map<String, Integer> classCounts) {
//            this.boundingBoxes = boundingBoxes;
//            this.classCounts = classCounts;
//        }
//
//        @Override
//        public void run() {
//            if (boundingBoxes == null || boundingBoxes.isEmpty()) {
////                overlayView.clear();
//                Toast.makeText(MainActivity.this, "No objects detected", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
////            overlayView.setResults(boundingBoxes);
//
//            for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
//                Log.d(TAG, "Detected " + entry.getValue() + " × " + entry.getKey());
//            }
//
//            StringBuilder toastMsg = new StringBuilder("Detected:\n");
//            for (Map.Entry<String, Integer> entry : classCounts.entrySet()) {
//                toastMsg.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
//            }
//            Toast.makeText(MainActivity.this, toastMsg.toString().trim(), Toast.LENGTH_LONG).show();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (detectionExecutor != null) detectionExecutor.shutdown();
//        if (detector != null) detector.close();
//}
//}
package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
}
}