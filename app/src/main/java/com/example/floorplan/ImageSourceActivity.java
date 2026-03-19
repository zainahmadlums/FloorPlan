package com.example.floorplan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;

public class ImageSourceActivity extends AppCompatActivity {

    private Uri cameraImageUri;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    proceedToProcessing(cameraImageUri);
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    proceedToProcessing(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_source);

        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));
    }

    private void launchCamera() {
        File imageDir = new File(getCacheDir(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        File imageFile = new File(imageDir, "camera_capture.jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        takePictureLauncher.launch(cameraImageUri);
    }

    private void proceedToProcessing(Uri imageUri) {
        Intent intent = new Intent(this, ProcessingActivity.class);
        intent.putExtra("imageUri", imageUri.toString());
        startActivity(intent);
        finish();
    }
}