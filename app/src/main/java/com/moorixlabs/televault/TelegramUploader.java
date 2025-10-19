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
import java.net.HttpURLConnection; // Make sure this is imported
import java.net.URL;

public class TelegramUploader {

    private static final String TAG = "TelegramUploader";
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot"; // <-- CORRECT
    private final Context context;
    private final String botToken;
    private final String chatId;
    private final Uri fileUri;
    private final String fileName;
    private final UploadCallback callback;

    // --- NEW FIELDS ---
    private volatile boolean isCancelled = false;
    private HttpURLConnection connection = null;
    // --- END NEW FIELDS ---


    public interface UploadCallback {
        void onUploadProgress(int progress);
        void onUploadSuccess(String fileId, String messageId);
        void onUploadFailed(String error);
        void onUploadCancelled(); // --- NEW CALLBACK ---
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

    // --- NEW METHOD ---
    public void cancel() {
        isCancelled = true;
        Log.d(TAG, "Cancel() called. Flag set to true.");

        // Disconnect HTTP connection from a new thread to interrupt blocking IO
        if (connection != null) {
            new Thread(() -> {
                try {
                    Log.d(TAG, "Attempting to disconnect connection from cancel() thread.");
                    connection.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Error disconnecting on cancel", e);
                }
            }).start();
        }
    }
    // --- END NEW METHOD ---


    public void upload() {
        // HttpURLConnection connection = null; // <-- Now a class field
        DataOutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            // Initial progress
            callback.onUploadProgress(0);
            Log.d(TAG, "Starting upload for: " + fileName);

            // Get file input stream and size
            inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                callback.onUploadFailed("Could not read file");
                return;
            }

            long fileSize = FileUtils.getFileSize(context, fileUri);
            Log.d(TAG, "File size: " + fileSize + " bytes");

            // Prepare multipart request
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            String lineEnd = "\r\n";
            String twoHyphens = "--";

            URL url = new URL(TELEGRAM_API_BASE + botToken + "/sendDocument");
            connection = (HttpURLConnection) url.openConnection(); // <-- Assign to class field
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
            int lastReportedProgress = 0;
            byte[] buffer = new byte[8192];
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

            Log.d(TAG, "Starting file transfer...");

            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                // If cancelled, loop will be broken by an exception from connection.disconnect()
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress != lastReportedProgress) {
                        lastReportedProgress = progress;
                        callback.onUploadProgress(progress);
                        Log.d(TAG, "Upload progress: " + progress + "% (" + totalBytesRead + "/" + fileSize + " bytes)");
                    }
                }
            }

            // Check for cancellation *before* trying to get response
            if (isCancelled) {
                Log.d(TAG, "Upload cancelled after loop, before getting response.");
                callback.onUploadCancelled();
                return;
            }

            // ... (rest of the upload logic is fine, but we wrap it)

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            outputStream.flush();
            outputStream.close();
            bufferedInputStream.close();
            inputStream.close();
            inputStream = null; // Set to null

            Log.d(TAG, "File transfer complete, waiting for server response...");

            // Get response
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // ... (JSON parsing logic remains the same)
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

                    Log.i(TAG, "✅ Upload successful! File ID: " + fileId + ", Message ID: " + messageId);
                    callback.onUploadSuccess(fileId, messageId);
                } else {
                    String errorDesc = jsonResponse.optString("description", "Unknown error");
                    Log.e(TAG, "❌ Upload failed: " + errorDesc);
                    callback.onUploadFailed(errorDesc);
                }
            } else {
                Log.e(TAG, "❌ HTTP Error: " + responseCode);
                callback.onUploadFailed("HTTP Error: " + responseCode);
            }

        } catch (java.net.SocketException e) {
            // This exception is often thrown when connection.disconnect() is called
            if (isCancelled) {
                Log.d(TAG, "Upload cancelled (SocketException caught).");
                callback.onUploadCancelled();
            } else {
                Log.e(TAG, "❌ Upload exception (SocketException)", e);
                callback.onUploadFailed("Upload failed: " + e.getMessage());
            }
        } catch (java.io.IOException e) {
            // This can also be thrown
            if (isCancelled) {
                Log.d(TAG, "Upload cancelled (IOException caught).");
                callback.onUploadCancelled();
            } else {
                Log.e(TAG, "❌ Upload exception (IOException)", e);
                callback.onUploadFailed("Upload failed: " + e.getMessage());
            }
        } catch (Exception e) {
            if (!isCancelled) {
                Log.e(TAG, "❌ Upload exception", e);
                callback.onUploadFailed("Upload failed: " + e.getMessage());
            } else {
                Log.d(TAG, "Upload cancelled (Generic Exception caught).");
                callback.onUploadCancelled();
            }
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