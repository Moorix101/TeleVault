package com.moorixlabs.televault;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class LocaleHelper {

    private static final String PREFS_NAME = "TeleVaultSettings";
    private static final String KEY_LANGUAGE = "selected_language";
    public static final String DEFAULT_LANG = "en";

    /**
     * Sets the new locale for the app and persists it.
     */
    public static void setLocale(Context context, String languageCode) {
        // 1. Save the preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        // 2. Apply the locale change
        applyLocale(languageCode);
    }

    /**
     * Retrieves the persisted locale code from SharedPreferences.
     * Defaults to "en" (English) if nothing is set.
     */
    public static String getPersistedLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANG); // Default to English
    }

    /**
     * Applies the persisted locale on app startup.
     */
    public static void applyPersistedLocale(Context context) {
        String languageCode = getPersistedLocale(context);
        applyLocale(languageCode);
    }

    /**
     * The core logic to update the app's locale using AppCompatDelegate.
     * This ensures the change is applied consistently.
     */
    private static void applyLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }

    /**
     * Helper to get the human-readable name of the current language.
     */
    public static String getCurrentLanguageDisplay(Context context) {
        String langCode = getPersistedLocale(context);
        switch (langCode) {
            case "fr":
                return "Français (French)";
            case "ar":
                return "العربية (Arabic)";
            default:
                return "English";
        }
    }
}