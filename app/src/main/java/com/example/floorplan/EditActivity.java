package com.example.floorplan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;

public class EditActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private File tempFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        drawingView = findViewById(R.id.drawingView);
        Button btnUndo = findViewById(R.id.btnUndo);
        Button btnRedo = findViewById(R.id.btnRedo);
        Button btnModeToggle = findViewById(R.id.btnModeToggle);
        Button btnColorBlack = findViewById(R.id.btnColorBlack);
        Button btnColorGray = findViewById(R.id.btnColorGray);
        Button btnCancelEdit = findViewById(R.id.btnCancelEdit);
        Button btnSaveEdit = findViewById(R.id.btnSaveEdit);

        tempFile = new File(getFilesDir(), Constants.TEMP_FILE_NAME);

        if (tempFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
            drawingView.setBitmap(bitmap);
        } else {
            finish();
            return;
        }

        btnUndo.setOnClickListener(v -> drawingView.undo());
        btnRedo.setOnClickListener(v -> drawingView.redo());

        // Handle the Draw / Pan toggle
        btnModeToggle.setOnClickListener(v -> {
            if (drawingView.getMode() == DrawingView.Mode.DRAW) {
                drawingView.setMode(DrawingView.Mode.PAN_ZOOM);
                btnModeToggle.setText("Mode: PAN/ZOOM");
                btnModeToggle.setBackgroundColor(Color.parseColor("#9C27B0")); // Purple
            } else {
                drawingView.setMode(DrawingView.Mode.DRAW);
                btnModeToggle.setText("Mode: DRAW");
                btnModeToggle.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
            }
        });

        btnColorBlack.setOnClickListener(v -> {
            drawingView.setCurrentColor(Color.BLACK);
            btnColorBlack.setAlpha(1.0f);
            btnColorGray.setAlpha(0.5f);
            // Auto-switch to draw mode if they pick a color
            drawingView.setMode(DrawingView.Mode.DRAW);
            btnModeToggle.setText("Mode: DRAW");
            btnModeToggle.setBackgroundColor(Color.parseColor("#FF9800"));
        });

        btnColorGray.setOnClickListener(v -> {
            drawingView.setCurrentColor(Color.GRAY);
            btnColorGray.setAlpha(1.0f);
            btnColorBlack.setAlpha(0.5f);
            // Auto-switch to draw mode if they pick a color
            drawingView.setMode(DrawingView.Mode.DRAW);
            btnModeToggle.setText("Mode: DRAW");
            btnModeToggle.setBackgroundColor(Color.parseColor("#FF9800"));
        });

        btnColorBlack.setAlpha(1.0f);
        btnColorGray.setAlpha(0.5f);

        btnCancelEdit.setOnClickListener(v -> finish());

        btnSaveEdit.setOnClickListener(v -> {
            Bitmap finalImage = drawingView.getResultBitmap();
            if (finalImage != null) {
                try {
                    FileOutputStream out = new FileOutputStream(tempFile);
                    finalImage.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            finish();
        });
    }
}