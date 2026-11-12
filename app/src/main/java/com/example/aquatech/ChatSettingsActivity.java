package com.example.aquatech;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class ChatSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private SwitchCompat switchTechNotif, switchActiveStatus;
    private View btnBotLanguage, btnClearBotChat;
    private TextView tvCurrentLanguage;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AquaTechChatSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        setupStatusBar();
        initializeViews();
        loadSavedSettings();
        setupClickListeners();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        switchTechNotif = findViewById(R.id.switchTechNotif);
        switchActiveStatus = findViewById(R.id.switchActiveStatus);
        btnBotLanguage = findViewById(R.id.btnBotLanguage);
        btnClearBotChat = findViewById(R.id.btnClearBotChat);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
    }

    private void loadSavedSettings() {
        if (sharedPreferences == null) {
            return;
        }

        boolean notifOn = sharedPreferences.getBoolean("tech_notif", true);
        boolean activeOn = sharedPreferences.getBoolean("active_status", true);
        String lang = sharedPreferences.getString("bot_lang", "Taglish");

        if (switchTechNotif != null) {
            switchTechNotif.setChecked(notifOn);
        }

        if (switchActiveStatus != null) {
            switchActiveStatus.setChecked(activeOn);
        }

        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(lang);
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnBotLanguage != null) {
            btnBotLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        if (switchTechNotif != null) {
            switchTechNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putBoolean("tech_notif", isChecked).apply();
                }
            });
        }

        if (switchActiveStatus != null) {
            switchActiveStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (sharedPreferences != null) {
                    sharedPreferences.edit().putBoolean("active_status", isChecked).apply();
                }
            });
        }

        if (btnClearBotChat != null) {
            btnClearBotChat.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Clear Chat?")
                        .setMessage("Delete all AquaBuddy history?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            Toast.makeText(this, "Chat history cleared!", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void showLanguageDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_select_language);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RadioGroup rgLanguages = dialog.findViewById(R.id.rgLanguages);
        Button btnSave = dialog.findViewById(R.id.btnSaveLanguage);

        String current = tvCurrentLanguage != null ? tvCurrentLanguage.getText().toString() : "";
        if (rgLanguages != null) {
            if ("Tagalog".equals(current)) {
                rgLanguages.check(R.id.rbTagalog);
            } else if ("English".equals(current)) {
                rgLanguages.check(R.id.rbEnglish);
            } else {
                rgLanguages.check(R.id.rbTaglish);
            }
        }

        if (btnSave != null && rgLanguages != null) {
            btnSave.setOnClickListener(v -> {
                int selectedId = rgLanguages.getCheckedRadioButtonId();
                String lang = "Taglish";

                if (selectedId == R.id.rbTagalog) {
                    lang = "Tagalog";
                } else if (selectedId == R.id.rbEnglish) {
                    lang = "English";
                }

                updateLanguage(dialog, lang);
            });
        }

        dialog.show();
    }

    private void updateLanguage(Dialog dialog, String lang) {
        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(lang);
        }

        if (sharedPreferences != null) {
            sharedPreferences.edit().putString("bot_lang", lang).apply();
        }

        Toast.makeText(this, "Language set to " + lang, Toast.LENGTH_SHORT).show();
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
