package com.example.floorplan;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class ImageSourceActivity extends AppCompatActivity {

    private Uri cameraImageUri;
    private final ArrayList<String> selectedImageUris = new ArrayList<>();
    private SelectedImageAdapter adapter;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    addImageToList(cameraImageUri.toString());
                }
            });

    private final ActivityResultLauncher<String> pickGalleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    addImageToList(uri.toString());
                }
            });

    // Launcher to handle removal request from FullScreenActivity
    private final ActivityResultLauncher<Intent> fullScreenLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int indexToRemove = result.getData().getIntExtra(FullScreenActivity.EXTRA_INDEX, -1);
                    if (indexToRemove != -1) {
                        removeImageFromList(indexToRemove);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_source);

        RecyclerView rvSelectedImages = findViewById(R.id.rvSelectedImages);
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnGallery = findViewById(R.id.btnGallery);
        Button btnGenerate = findViewById(R.id.btnGenerate);

        rvSelectedImages.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapter with the click listener
        adapter = new SelectedImageAdapter(this, selectedImageUris, (uri, position) -> {
            Intent intent = new Intent(this, FullScreenActivity.class);
            intent.putExtra(FullScreenActivity.EXTRA_FILE_PATH, uri);
            // Tell FullScreen we are viewing a SOURCE image, not a saved PLAN
            intent.putExtra(FullScreenActivity.EXTRA_MODE, FullScreenActivity.MODE_SOURCE_IMAGE);
            // Pass the index so we know which one to remove later
            intent.putExtra(FullScreenActivity.EXTRA_INDEX, position);
            fullScreenLauncher.launch(intent);
        });
        rvSelectedImages.setAdapter(adapter);

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> pickGalleryLauncher.launch("image/*"));

        btnGenerate.setOnClickListener(v -> {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, "Select at least one image first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ProcessingActivity.class);
            intent.putStringArrayListExtra("imageUris", selectedImageUris);
            startActivity(intent);
            finish();
        });
    }

    private void launchCamera() {
        File imageDir = new File(getCacheDir(), "images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        File imageFile = new File(imageDir, "camera_capture_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        takePictureLauncher.launch(cameraImageUri);
    }

    private void addImageToList(String uriString) {
        selectedImageUris.add(uriString);
        adapter.notifyItemInserted(selectedImageUris.size() - 1);
    }

    private void removeImageFromList(int index) {
        if (index >= 0 && index < selectedImageUris.size()) {
            selectedImageUris.remove(index);
            adapter.notifyItemRemoved(index);
            // Critical: notifyItemRemoved doesn't update positions of remaining items,
            // we must range update so future removals have correct indices.
            adapter.notifyItemRangeChanged(index, selectedImageUris.size());
        }
    }
}