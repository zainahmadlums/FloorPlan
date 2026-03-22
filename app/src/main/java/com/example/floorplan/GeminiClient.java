package com.example.floorplan;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executors;

public class GeminiClient {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    public static final int GRID_SIZE = 30;
    public static final int TOTAL_PIXELS = GRID_SIZE * GRID_SIZE;
    String promptText = "I need a floor plan for representing the walkable areas by the instructor in the classroom to guide location tracking." +
            "Act as an expert spatial analyst. Mentally synthesize these first-person images into a single coherent 3D room, then project it into a strict 2D top-down orthographic floor plan. " +
            "If there are multiple images, they represent the same room from different positions." +
            "Map this floor plan to a precise " + GRID_SIZE + "x" + GRID_SIZE + " grid.\n" +
            "Values:\n" +
            "'0' = Impassable (walls, furniture, empty void).\n" +
            "'1' = Passable (clear, walkable floor space, pathway. The tiny space between rows of seats does not count, unless its substantial.).\n" +
            "Rules:\n" +
            "1. Traversal: Read left-to-right, top-to-bottom. The first character is the top-left corner, the last is the bottom-right corner.\n" +
            "2. Padding: If the room is rectangular or irregular, you MUST pad the out-of-bounds edges with '0's to force a perfect square grid.\n" +
            "3. Length: The output MUST be a single, unbroken string of EXACTLY " + TOTAL_PIXELS + " characters.\n" +
            "CRITICAL CONSTRAINT: Output raw text only. No code blocks, no markdown formatting, no spaces, no newlines, and absolutely zero conversational text. Just the " + TOTAL_PIXELS + " binary digits.";

    public interface GeminiCallback {
        void onSuccess(Bitmap floorPlan, String rawGrid);
        void onError(String errorMessage);
    }

    public void generateFloorPlan(List<Bitmap> sourceImages, GeminiCallback callback) {
        try {
            GenerativeModel gm = new GenerativeModel("gemini-3-flash-preview", Constants.GEMINI_API_KEY);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content.Builder contentBuilder = new Content.Builder()
                    .addText(promptText);

            // Loop through all provided images and attach them to the request
            for (Bitmap originalBitmap : sourceImages) {
                Bitmap scaledImage = scaleBitmapDefensively(originalBitmap, 1024);
                contentBuilder.addImage(scaledImage);
            }

            Content content = contentBuilder.build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String gridString = result.getText();
                    if (gridString != null) {
                        String cleanString = gridString.replaceAll("[^01]", "");

                        StringBuilder sb = new StringBuilder(cleanString);
                        while (sb.length() < TOTAL_PIXELS) {
                            sb.append("0");
                        }
                        if (sb.length() > TOTAL_PIXELS) {
                            sb.setLength(TOTAL_PIXELS);
                        }

                        String paddedGrid = sb.toString();
                        Bitmap resultBitmap = convertGridToBitmap(paddedGrid);

                        if (resultBitmap != null) {
                            postSuccess(callback, resultBitmap, paddedGrid);
                        } else {
                            postError(callback, "Failed to parse the grid properly.");
                        }
                    } else {
                        postError(callback, "API returned an empty response.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    postError(callback, t.getMessage());
                }
            }, Executors.newSingleThreadExecutor());

        } catch (Exception e) {
            postError(callback, e.getMessage());
        }
    }

    private Bitmap scaleBitmapDefensively(Bitmap original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();
        if (width <= maxDimension && height <= maxDimension) {
            return original;
        }
        float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newWidth = Math.round(ratio * width);
        int newHeight = Math.round(ratio * height);
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private void postSuccess(GeminiCallback callback, Bitmap bitmap, String rawGrid) {
        mainHandler.post(() -> callback.onSuccess(bitmap, rawGrid));
    }

    private void postError(GeminiCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    private Bitmap convertGridToBitmap(String paddedGrid) {
        Bitmap smallBitmap = Bitmap.createBitmap(GRID_SIZE, GRID_SIZE, Bitmap.Config.ARGB_8888);
        int index = 0;

        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                char c = paddedGrid.charAt(index++);
                smallBitmap.setPixel(x, y, c == '0' ? Color.BLACK : Color.GRAY);
            }
        }

        // Scale it up. The 'false' stops Android from blurring it.
        return Bitmap.createScaledBitmap(smallBitmap, 800, 800, false);
    }
}