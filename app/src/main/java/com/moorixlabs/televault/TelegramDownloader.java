package com.moorixlabs.televault;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramDownloader {

    private static final String TAG = "TelegramDownloader";
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";
    private static final String TELEGRAM_FILE_BASE = "https://api.telegram.org/file/bot";
    private static final String APP_FOLDER = "TeleVault";
    private static final String DOWNLOAD_FOLDER = "Download";

    private final Context context;
    private final String botToken; // Now passed in
    private final String fileId;
    private final String fileName;
    private final DownloadCallback callback;

    public interface DownloadCallback {
        void onDownloadProgress(int progress);
        void onDownloadSuccess(Uri fileUri);
        void onDownloadFailed(String error);
    }

    public TelegramDownloader(Context context, String botToken, String fileId, // Updated constructor
                              String fileName, DownloadCallback callback) {
        this.context = context;
        this.botToken = botToken;
        this.fileId = fileId;
        this.fileName = fileName;
        this.callback = callback;
    }

    public void download() {
        try {
            // Step 1: Get file path from Telegram
            String filePath = getFilePath();
            if (filePath == null) {
                callback.onDownloadFailed("Could not get file path from Telegram. Check your bot token and file ID.");
                return;
            }

            Log.i(TAG, "File path obtained: " + filePath);

            // Step 2: Download file to app's download folder
            Uri downloadedUri = downloadFile(filePath);
            if (downloadedUri != null) {
                callback.onDownloadSuccess(downloadedUri);
            } else {
                callback.onDownloadFailed("Failed to download file");
            }

        } catch (Exception e) {
            Log.e(TAG, "Download exception", e);
            callback.onDownloadFailed("Download error: " + e.getMessage());
        }
    }

    /**
     * Step 1: Get file path from Telegram using getFile API
     */
    private String getFilePath() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            String urlString = TELEGRAM_API_BASE + botToken + "/getFile?file_id=" + fileId;
            Log.i(TAG, "Getting file info from Telegram...");

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            Log.i(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Log.i(TAG, "Response: " + response.toString());

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getBoolean("ok")) {
                    JSONObject result = jsonResponse.getJSONObject("result");
                    return result.getString("file_path");
                } else {
                    String errorDesc = jsonResponse.optString("description", "Unknown error");
                    Log.e(TAG, "API error: " + errorDesc);
                    return null;
                }
            } else {
                // Read error response
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e(TAG, "HTTP error " + responseCode + ": " + errorResponse.toString());
                    errorReader.close();
                }
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting file path", e);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error closing connection", e);
            }
        }
    }

    /**
     * Step 2: Download file from Telegram and save to Documents/TeleVault/Download
     */
    private Uri downloadFile(String filePath) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Download URL
            String downloadUrl = TELEGRAM_FILE_BASE + botToken + "/" + filePath;
            Log.i(TAG, "Downloading from: " + downloadUrl);

            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP error: " + responseCode);
                return null;
            }

            long fileSize = connection.getContentLengthLong();
            Log.i(TAG, "File size: " + fileSize + " bytes");

            inputStream = new BufferedInputStream(connection.getInputStream());

            // Save to Documents/TeleVault/Download folder
            return saveToAppDownloadFolder(inputStream, fileSize);

        } catch (Exception e) {
            Log.e(TAG, "Error downloading file", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error closing connections", e);
            }
        }
    }

    /**
     * Save file to Documents/TeleVault/Download folder
     */
    private Uri saveToAppDownloadFolder(InputStream inputStream, long fileSize) throws Exception {
        // Create app download folder: Documents/TeleVault/Download
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File appFolder = new File(documentsDir, APP_FOLDER);
        File downloadFolder = new File(appFolder, DOWNLOAD_FOLDER);

        if (!downloadFolder.exists()) {
            boolean created = downloadFolder.mkdirs();
            if (!created) {
                throw new Exception("Could not create download folder");
            }
            Log.i(TAG, "Created download folder: " + downloadFolder.getAbsolutePath());
        }

        // Create output file with unique name if exists
        File outputFile = new File(downloadFolder, fileName);
        int counter = 1;
        String nameWithoutExt = fileName.contains(".") ?
                fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf('.')) : "";

        while (outputFile.exists()) {
            outputFile = new File(downloadFolder, nameWithoutExt + " (" + counter + ")" + extension);
            counter++;
        }

        Log.i(TAG, "Saving to: " + outputFile.getAbsolutePath());

        // Write file with progress tracking
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytesRead = 0;
            int lastProgress = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Report progress
                if (fileSize > 0) {
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    if (progress != lastProgress && progress % 10 == 0) {
                        callback.onDownloadProgress(progress);
                        Log.i(TAG, "Download progress: " + progress + "%");
                        lastProgress = progress;
                    }
                }
            }

            outputStream.flush();
            callback.onDownloadProgress(100);
        }

        Log.i(TAG, "File saved successfully: " + outputFile.getAbsolutePath());
        return Uri.fromFile(outputFile);
    }
}
