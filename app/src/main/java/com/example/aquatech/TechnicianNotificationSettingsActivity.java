package com.example.aquatech;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class TechnicianNotificationSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_notification_settings);

        prefs = getSharedPreferences("AquaTechPrefs", MODE_PRIVATE);
        initializeViews();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupTechnicianToggles();
    }

    private void setupTechnicianToggles() {
        SwitchCompat swNewTasks = findViewById(R.id.switchNewTasks);
        SwitchCompat swChat = findViewById(R.id.switchChatNotif);
        SwitchCompat swSound = findViewById(R.id.switchTechSound);
        SwitchCompat swVibrate = findViewById(R.id.switchTechVibration);

        if (swNewTasks != null && prefs != null) {
            swNewTasks.setChecked(prefs.getBoolean("tech_new_tasks", true));
            swNewTasks.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("tech_new_tasks", isChecked).apply();
            });
        }

        if (swChat != null && prefs != null) {
            swChat.setChecked(prefs.getBoolean("tech_chat_notif", true));
            swChat.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("tech_chat_notif", isChecked).apply();
            });
        }

        if (swSound != null && prefs != null) {
            swSound.setChecked(prefs.getBoolean("tech_notif_sound", true));
            swSound.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("tech_notif_sound", isChecked).apply();
            });
        }

        if (swVibrate != null && prefs != null) {
            swVibrate.setChecked(prefs.getBoolean("tech_notif_vibrate", true));
            swVibrate.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("tech_notif_vibrate", isChecked).apply();
            });
        }
    }
}
