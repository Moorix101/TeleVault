package com.moorixlabs.televault;

import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramDeleter {

    private static final String TAG = "TelegramDeleter";
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final String botToken;
    private final String chatId;
    private final String messageId;
    private final DeleteCallback callback;

    public interface DeleteCallback {
        void onDeleteSuccess();
        void onDeleteFailed(String error);
    }

    public TelegramDeleter(String botToken, String chatId, String messageId, DeleteCallback callback) {
        this.botToken = botToken;
        this.chatId = chatId;
        this.messageId = messageId;
        this.callback = callback;
    }

    public void delete() {
        HttpURLConnection connection = null;
        try {
            // Construct the URL for the deleteMessage API endpoint
            String urlString = TELEGRAM_API_BASE + botToken + "/deleteMessage?chat_id=" + chatId + "&message_id=" + messageId;
            URL url = new URL(urlString);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getBoolean("ok")) {
                    Log.i(TAG, "Message deleted successfully from Telegram.");
                    callback.onDeleteSuccess();
                } else {
                    String errorDesc = jsonResponse.optString("description", "Unknown API error");
                    Log.e(TAG, "API Error: " + errorDesc);
                    callback.onDeleteFailed(errorDesc);
                }
            } else {
                Log.e(TAG, "HTTP Error: " + responseCode);
                callback.onDeleteFailed("HTTP Error: " + responseCode);
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception during deletion", e);
            callback.onDeleteFailed("Deletion failed: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}