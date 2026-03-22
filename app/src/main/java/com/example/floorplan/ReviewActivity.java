package com.example.floorplan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.File;

public class ReviewActivity extends AppCompatActivity {

    private File tempFile;
    private ImageView imageReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        imageReview = findViewById(R.id.imageReview);
        Button btnEdit = findViewById(R.id.btnEdit);
        Button btnAccept = findViewById(R.id.btnAccept);
        Button btnDecline = findViewById(R.id.btnDecline);

        tempFile = new File(getFilesDir(), Constants.TEMP_FILE_NAME);

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditActivity.class);
            startActivity(intent);
        });

        btnAccept.setOnClickListener(v -> acceptPlan());
        btnDecline.setOnClickListener(v -> declinePlan());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the image in onResume so it updates after returning from EditActivity
        if (tempFile.exists()) {
            Glide.with(this)
                    .load(tempFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageReview);
        } else {
            finish();
        }
    }

    private void acceptPlan() {
        if (tempFile.exists()) {
            File newFile = new File(getFilesDir(), "plan_" + System.currentTimeMillis() + ".png");
            tempFile.renameTo(newFile);
        }
        finish();
    }

    private void declinePlan() {
        if (tempFile.exists()) {
            tempFile.delete();
        }
        finish();
    }
}