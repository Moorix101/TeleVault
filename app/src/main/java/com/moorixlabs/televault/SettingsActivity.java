package com.moorixlabs.televault;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private EditText etBotToken;
    private EditText etChatId;
    private TextView tvConnectionStatus;
    private Button btnCheckConnection;
    private Button btnSaveSettings;
    private TextView tvCurrentLanguage;

    // Language cards
    private CardView cardEnglish;
    private CardView cardArabic;
    private CardView cardFrench;

    // Language indicators
    private View indicatorEnglish;
    private View indicatorArabic;
    private View indicatorFrench;

    private String selectedLanguage = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings_title));
        }

        initializeViews();
        loadCurrentSettings();
        setupClickListeners();

        // Load and display current language
        selectedLanguage = LocaleHelper.getPersistedLocale(this);
        updateLanguageSelection(selectedLanguage);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initializeViews() {
        etBotToken = findViewById(R.id.etBotToken);
        etChatId = findViewById(R.id.etChatId);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnCheckConnection = findViewById(R.id.btnCheckConnection);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);

        // Language cards
        cardEnglish = findViewById(R.id.cardEnglish);
        cardArabic = findViewById(R.id.cardArabic);
        cardFrench = findViewById(R.id.cardFrench);

        // Language indicators
        indicatorEnglish = findViewById(R.id.indicatorEnglish);
        indicatorArabic = findViewById(R.id.indicatorArabic);
        indicatorFrench = findViewById(R.id.indicatorFrench);
    }

    private void loadCurrentSettings() {
        String token = SettingsUtils.getBotToken(this);
        String chatId = SettingsUtils.getChatId(this);

        etBotToken.setText(token);
        etChatId.setText(chatId);

        // Use ContextCompat for color loading
        if (SettingsUtils.isConfigured(this)) {
            tvConnectionStatus.setText(getString(R.string.status_configured));
            tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            tvConnectionStatus.setText(R.string.status_not_tested);
            tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        }
    }

    private void setupClickListeners() {
        btnSaveSettings.setOnClickListener(v -> saveSettings());
        btnCheckConnection.setOnClickListener(v -> checkConnection());

        // Language card click listeners
        cardEnglish.setOnClickListener(v -> selectLanguage("en"));
        cardArabic.setOnClickListener(v -> selectLanguage("ar"));
        cardFrench.setOnClickListener(v -> selectLanguage("fr"));
    }

    private void selectLanguage(String languageCode) {
        selectedLanguage = languageCode;
        updateLanguageSelection(languageCode);

        // Save and apply the language immediately
        LocaleHelper.setLocale(this, languageCode);

        // Update hidden TextView for compatibility
        tvCurrentLanguage.setText(LocaleHelper.getCurrentLanguageDisplay(this));

        // Show feedback to user
        String languageName = getLanguageName(languageCode);
        Toast.makeText(this, getString(R.string.language_changed) + " " + languageName, Toast.LENGTH_SHORT).show();

        // Recreate activity to apply changes
        recreate();
    }

    private void updateLanguageSelection(String languageCode) {
        // Hide all indicators first
        indicatorEnglish.setVisibility(View.GONE);
        indicatorArabic.setVisibility(View.GONE);
        indicatorFrench.setVisibility(View.GONE);

        // Show the selected indicator
        switch (languageCode) {
            case "en":
                indicatorEnglish.setVisibility(View.VISIBLE);
                break;
            case "ar":
                indicatorArabic.setVisibility(View.VISIBLE);
                break;
            case "fr":
                indicatorFrench.setVisibility(View.VISIBLE);
                break;
            default:
                indicatorEnglish.setVisibility(View.VISIBLE);
                break;
        }
    }

    private String getLanguageName(String languageCode) {
        switch (languageCode) {
            case "ar":
                return "العربية";
            case "fr":
                return "Français";
            default:
                return "English";
        }
    }

    private void saveSettings() {
        String token = etBotToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, R.string.settings_required_title, Toast.LENGTH_LONG).show();
            return;
        }

        SettingsUtils.saveSettings(this, token, chatId);
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_LONG).show();

        setResult(RESULT_OK);
        finish();
    }

    private void checkConnection() {
        String token = etBotToken.getText().toString().trim();
        String chatId = etChatId.getText().toString().trim();

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, R.string.settings_required_message, Toast.LENGTH_LONG).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.test_connection_in_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new TelegramTester().runTest(token, chatId, new TelegramTester.TestCallback() {
            @Override
            public void onTestSuccess(String botName, String messageId) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    tvConnectionStatus.setText(getString(R.string.status_success_format, botName));
                    tvConnectionStatus.setTextColor(ContextCompat.getColor(SettingsActivity.this, android.R.color.holo_green_light));
                    Toast.makeText(SettingsActivity.this, R.string.test_success, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onTestFailed(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    tvConnectionStatus.setText(R.string.status_failed_general);
                    tvConnectionStatus.setTextColor(ContextCompat.getColor(SettingsActivity.this, android.R.color.holo_red_light));

                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.test_failed_title)
                            .setMessage(error)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                });
            }
        });
    }
}