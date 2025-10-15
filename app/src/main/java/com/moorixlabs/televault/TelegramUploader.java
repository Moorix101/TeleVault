package com.moorixlabs.televault;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramUploader {

    private static final String TAG = "TelegramUploader";
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final Context context;
    private final String botToken;
    private final String chatId;
    private final Uri fileUri;
    private final String fileName;
    private final UploadCallback callback;

    public interface UploadCallback {
        void onUploadProgress(int progress);
        void onUploadSuccess(String fileId, String messageId);
        void onUploadFailed(String error);
    }

    public TelegramUploader(Context context, String botToken, String chatId,
                            Uri fileUri, String fileName, UploadCallback callback) {
        this.context = context;
        this.botToken = botToken;
        this.chatId = chatId;
        this.fileUri = fileUri;
        this.fileName = fileName;
        this.callback = callback;
    }

    public void upload() {
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            // Get file input stream and size
            inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                callback.onUploadFailed("Could not read file");
                return;
            }

            long fileSize = FileUtils.getFileSize(context, fileUri);

            // Prepare multipart request
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL(TELEGRAM_API_BASE + botToken + "/sendDocument");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());

            // Add chat_id parameter
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(chatId + lineEnd);

            // Add document file
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);
            outputStream.writeBytes("Content-Disposition: form-data; name=\"document\";filename=\""
                    + fileName + "\"" + lineEnd);
            outputStream.writeBytes("Content-Type: application/octet-stream" + lineEnd);
            outputStream.writeBytes(lineEnd);

            // Read and write file with progress tracking
            int bytesRead;
            long totalBytesRead = 0;
            byte[] buffer = new byte[8192];
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Calculate and report progress
                if (fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    callback.onUploadProgress(progress);
                }
            }

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            outputStream.flush();
            outputStream.close();
            bufferedInputStream.close();
            inputStream.close();

            // Get response
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response to get file_id and message_id
                JSONObject jsonResponse = new JSONObject(response.toString());

                if (jsonResponse.getBoolean("ok")) {
                    JSONObject result = jsonResponse.getJSONObject("result");
                    String messageId = result.getString("message_id");

                    // Get file_id from the document object
                    JSONObject document = result.getJSONObject("document");
                    String fileId = document.getString("file_id");

                    Log.i(TAG, "Upload successful. File ID: " + fileId + ", Message ID: " + messageId);
                    callback.onUploadSuccess(fileId, messageId);
                } else {
                    String errorDesc = jsonResponse.optString("description", "Unknown error");
                    Log.e(TAG, "Upload failed: " + errorDesc);
                    callback.onUploadFailed(errorDesc);
                }
            } else {
                Log.e(TAG, "HTTP Error: " + responseCode);
                callback.onUploadFailed("HTTP Error: " + responseCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Upload exception", e);
            callback.onUploadFailed("Upload failed: " + e.getMessage());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error closing connections", e);
            }
        }
    }
}