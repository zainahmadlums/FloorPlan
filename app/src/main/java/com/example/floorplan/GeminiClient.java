package com.example.floorplan;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class GeminiClient {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + Constants.GEMINI_API_KEY;

    public interface GeminiCallback {
        void onSuccess(Bitmap floorPlan);
        void onError(String errorMessage);
    }

    public void generateFloorPlan(Bitmap sourceImage, GeminiCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String base64Image = encodeImageToBase64(sourceImage);
                String jsonPayload = buildJsonPayload(base64Image);

                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == 429) {
                    callback.onError("Limit reached. Please try again later.");
                    return;
                }

                if (responseCode >= 200 && responseCode < 300) {
                    Scanner scanner = new Scanner(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseString = new StringBuilder();
                    while (scanner.hasNextLine()) {
                        responseString.append(scanner.nextLine());
                    }
                    scanner.close();

                    String gridString = parseResponse(responseString.toString());
                    Bitmap resultBitmap = convertGridToBitmap(gridString);
                    if (resultBitmap != null) {
                        callback.onSuccess(resultBitmap);
                    } else {
                        callback.onError("Failed to parse the grid properly.");
                    }
                } else {
                    callback.onError("API Error: " + responseCode);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    private String buildJsonPayload(String base64Image) throws Exception {
        JSONObject root = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject contentObject = new JSONObject();
        JSONArray parts = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("text", "Analyze this room image and generate a 100x100 floor plan grid. Return ONLY a single contiguous string of 10000 characters consisting of '0's (impassable obstacles like seats/walls) and '1's (passable floor). Account for non-square rooms by padding with '0's. NO MARKDOWN, NO SPACES, NO NEWLINES, NO OTHER TEXT.");

        JSONObject inlineDataPart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);
        inlineDataPart.put("inline_data", inlineData);

        parts.put(textPart);
        parts.put(inlineDataPart);
        contentObject.put("parts", parts);
        contents.put(contentObject);
        root.put("contents", contents);

        return root.toString();
    }

    private String parseResponse(String jsonResponse) throws Exception {
        JSONObject json = new JSONObject(jsonResponse);
        String text = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        return text.replaceAll("[^01]", "");
    }

    private Bitmap convertGridToBitmap(String gridString) {
        if (gridString.length() < 10000) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        int index = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                char c = gridString.charAt(index++);
                bitmap.setPixel(x, y, c == '0' ? Color.BLACK : Color.GRAY);
            }
        }
        return bitmap;
    }
}