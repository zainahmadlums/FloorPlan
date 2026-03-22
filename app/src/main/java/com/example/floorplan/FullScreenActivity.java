package com.example.floorplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.io.File;

public class FullScreenActivity extends AppCompatActivity {

    // Define standard constants for intents so we don't misspell them between activities
    public static final String EXTRA_FILE_PATH = "filePath";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_INDEX = "index";

    // defined modes
    public static final int MODE_PLAN = 1; // viewing generated plan from MainActivity
    public static final int MODE_SOURCE_IMAGE = 2; // viewing picked image from ImageSourceActivity

    private int currentMode = MODE_PLAN;
    private int imagePosition = -1;
    private String mediaString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        ImageView imageView = findViewById(R.id.imageFullScreen);
        Button btnAction = findViewById(R.id.btnDelete); // This is the generic action button

        Intent intent = getIntent();
        mediaString = intent.getStringExtra(EXTRA_FILE_PATH);
        // Default to PLAN mode if not specified, keeps PlanAdapter working without changes
        currentMode = intent.getIntExtra(EXTRA_MODE, MODE_PLAN);
        imagePosition = intent.getIntExtra(EXTRA_INDEX, -1);

        if (mediaString != null) {
            Glide.with(this)
                    .load(mediaString)
                    .into(imageView);
        } else {
            finish();
            return;
        }

        // Configure button text based on mode
        if (currentMode == MODE_SOURCE_IMAGE) {
            btnAction.setText("Remove Image");
        } else {
            btnAction.setText("Delete Plan");
        }

        imageView.setOnClickListener(v -> finish());

        btnAction.setOnClickListener(v -> handleActionButtonClick());
    }

    private void handleActionButtonClick() {
        if (currentMode == MODE_SOURCE_IMAGE) {
            handleRemoveSourceImage();
        } else {
            handleDeleteSavedPlan();
        }
    }

    private void handleRemoveSourceImage() {
        // We don't delete files here (we might not even be able to if it's gallery).
        // We just tell ImageSourceActivity to remove it from the list.
        if (imagePosition != -1) {
            Intent resultData = new Intent();
            resultData.putExtra(EXTRA_INDEX, imagePosition);
            setResult(RESULT_OK, resultData);
        }
        finish();
    }

    private void handleDeleteSavedPlan() {
        // This is a file saved by us, so we can delete it.
        File fileToDelete = new File(mediaString);
        if (fileToDelete.exists() && fileToDelete.delete()) {
            Toast.makeText(this, "Plan deleted", Toast.LENGTH_SHORT).show();
            // Optional: set result OK here so MainActivity can refresh instantly,
            // though MainActivity currently refreshes onResume anyway.
            setResult(RESULT_OK);
        } else {
            Toast.makeText(this, "Failed to delete plan", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}