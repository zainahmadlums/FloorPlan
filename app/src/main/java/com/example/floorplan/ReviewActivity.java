package com.example.floorplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide; // The correct import
import java.io.File;

public class ReviewActivity extends AppCompatActivity {

    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        ImageView imageReview = findViewById(R.id.imageReview);
        Button btnAccept = findViewById(R.id.btnAccept);
        Button btnDecline = findViewById(R.id.btnDecline);

        tempFile = new File(getFilesDir(), Constants.TEMP_FILE_NAME);

        // We still check if the file exists before trying to load it
        if (tempFile.exists()) {
            // Glide is now used to load the temporary image, off the main thread.
            Glide.with(this)
                    .load(tempFile)
                    .into(imageReview);
        } else {
            // If for some reason the temp file isn't there, we just close this activity.
            finish();
        }

        btnAccept.setOnClickListener(v -> acceptPlan());
        btnDecline.setOnClickListener(v -> declinePlan());
    }

    private void acceptPlan() {
        if (tempFile.exists()) {
            // "Accept" means we rename the temp file to a permanent one.
            File newFile = new File(getFilesDir(), "plan_" + System.currentTimeMillis() + ".png");
            tempFile.renameTo(newFile);
        }
        finish(); // Go back to the main screen.
    }

    private void declinePlan() {
        if (tempFile.exists()) {
            // "Decline" just means we delete the temp file.
            tempFile.delete();
        }
        finish(); // Go back to the main screen.
    }
}