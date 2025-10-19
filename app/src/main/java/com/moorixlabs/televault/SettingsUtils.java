package com.moorixlabs.televault;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsUtils {

    private static final String PREFS_NAME = "TelevaultPrefs";
    private static final String KEY_BOT_TOKEN = "bot_token";
    private static final String KEY_CHAT_ID = "chat_id";

    /**
     * Retrieves the stored Bot Token.
     */
    public static String getBotToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Returns an empty string if not found, simplifying the check
        return prefs.getString(KEY_BOT_TOKEN, "");
    }

    /**
     * Retrieves the stored Chat ID.
     */
    public static String getChatId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CHAT_ID, "");
    }

    /**
     * Saves the Bot Token and Chat ID.
     */
    public static void saveSettings(Context context, String botToken, String chatId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_BOT_TOKEN, botToken.trim())
                .putString(KEY_CHAT_ID, chatId.trim())
                .apply();
    }

    /**
     * Checks if both required settings are configured.
     */
    public static boolean isConfigured(Context context) {
        String token = getBotToken(context);
        String chatId = getChatId(context);
        return !token.isEmpty() && !chatId.isEmpty();
    }
}
