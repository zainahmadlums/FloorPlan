package com.example.floorplan;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide; // The correct import
import java.io.File;

public class FullScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        ImageView imageView = findViewById(R.id.imageFullScreen);
        String filePath = getIntent().getStringExtra("filePath");

        // We check for null to prevent crashes, then load with Glide.
        if (filePath != null) {
            Glide.with(this)
                    .load(new File(filePath)) // We can load from a File object
                    .into(imageView);
        } else {
            // If no path was provided, something went wrong.
            finish();
        }

        // Tap the image to close the full-screen view.
        imageView.setOnClickListener(v -> finish());
    }
}