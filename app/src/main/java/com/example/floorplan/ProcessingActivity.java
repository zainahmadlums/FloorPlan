package com.example.floorplan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProcessingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing);

        String uriString = getIntent().getStringExtra("imageUri");
        if (uriString == null) {
            finish();
            return;
        }

        Uri imageUri = Uri.parse(uriString);
        processImage(imageUri);
    }

    private void processImage(Uri imageUri) {
        try {
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

            if (selectedImage == null) throw new Exception("Failed to decode image.");

            GeminiClient client = new GeminiClient();
            client.generateFloorPlan(selectedImage, new GeminiClient.GeminiCallback() {
                @Override
                public void onSuccess(Bitmap floorPlan) {
                    saveTempPlanAndReview(floorPlan);
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
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            finish();
        }
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