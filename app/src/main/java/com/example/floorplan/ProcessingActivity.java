package com.example.floorplan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ProcessingActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);

        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("imageUris");
        if (uriStrings == null || uriStrings.isEmpty()) {
            finish();
            return;
        }

        processImages(uriStrings);
    }

    private void processImages(ArrayList<String> uriStrings) {
        // Move decoding to a background thread so we don't freeze the UI processing 5 huge images
        new Thread(() -> {
            List<Bitmap> bitmaps = new ArrayList<>();
            try {
                for (String uriString : uriStrings) {
                    Uri imageUri = Uri.parse(uriString);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    BitmapFactory.decodeStream(inputStream, null, options);
                    if (inputStream != null) inputStream.close();

                    options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
                    options.inJustDecodeBounds = false;

                    inputStream = getContentResolver().openInputStream(imageUri);
                    Bitmap selectedImage = BitmapFactory.decodeStream(inputStream, null, options);
                    if (inputStream != null) inputStream.close();

                    if (selectedImage != null) {
                        bitmaps.add(selectedImage);
                    }
                }

                if (bitmaps.isEmpty()) throw new Exception("Failed to decode any images.");

                GeminiClient client = new GeminiClient();
                client.generateFloorPlan(bitmaps, new GeminiClient.GeminiCallback() {
                    @Override
                    public void onSuccess(Bitmap floorPlan, String rawGrid) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            StringBuilder formattedGrid = new StringBuilder();
                            for (int i = 0; i < rawGrid.length(); i += GeminiClient.GRID_SIZE) {
                                int end = Math.min(i + GeminiClient.GRID_SIZE, rawGrid.length());
                                formattedGrid.append(rawGrid.substring(i, end)).append("\n");
                            }

                            tvStatus.setText(formattedGrid.toString());

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                saveTempPlanAndReview(floorPlan);
                            }, 5000);
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProcessingActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                            finish();
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to load images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveTempPlanAndReview(Bitmap bitmap) {
        try {
            File tempFile = new File(getFilesDir(), Constants.TEMP_FILE_NAME);
            FileOutputStream out = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            runOnUiThread(() -> {
                Intent intent = new Intent(ProcessingActivity.this, ReviewActivity.class);
                startActivity(intent);
                finish();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}