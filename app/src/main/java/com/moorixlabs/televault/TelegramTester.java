package com.moorixlabs.televault;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramTester {

    private static final String TAG = "TelegramTester";
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    public interface TestCallback {
        void onTestSuccess(String botName, String messageId);
        void onTestFailed(String error);
    }

    /**
     * Performs a two-step connection test:
     * 1. Uses getMe to verify the bot token.
     * 2. Uses sendMessage to verify the chat ID.
     */
    public void runTest(String botToken, String chatId, TestCallback callback) {
        new Thread(() -> {
            try {
                // Step 1: Verify Bot Token using getMe
                String botName = verifyBotToken(botToken);
                if (botName == null) {
                    callback.onTestFailed("Bot Token Invalid or Connection Error. Check Token.");
                    return;
                }

                // Step 2: Verify Chat ID by sending a test message
                String messageId = sendTestMessage(botToken, chatId, botName);
                if (messageId == null) {
                    callback.onTestFailed("Chat ID Invalid. Ensure the bot is an admin in the channel/group, and the ID is correct.");
                    return;
                }

                callback.onTestSuccess(botName, messageId);

            } catch (Exception e) {
                Log.e(TAG, "Test failed due to exception", e);
                callback.onTestFailed("General Test Failure: " + e.getMessage());
            }
        }).start();
    }

    private String verifyBotToken(String botToken) throws Exception {
        String urlString = TELEGRAM_API_BASE + botToken + "/getMe";
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = getResponse(connection);
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getBoolean("ok")) {
                    return jsonResponse.getJSONObject("result").getString("username");
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private String sendTestMessage(String botToken, String chatId, String botName) throws Exception {
        String testMessage = "✅ TeleVault connection successful! Bot: @" + botName;
        String urlString = TELEGRAM_API_BASE + botToken + "/sendMessage?chat_id=" + chatId +
                "&text=" + java.net.URLEncoder.encode(testMessage, "UTF-8");

        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = getResponse(connection);
                JSONObject jsonResponse = new JSONObject(response);
                if (jsonResponse.getBoolean("ok")) {
                    return jsonResponse.getJSONObject("result").getString("message_id");
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    private String getResponse(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }
}
