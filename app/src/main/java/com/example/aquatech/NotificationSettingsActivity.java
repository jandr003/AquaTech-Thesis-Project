package com.example.aquatech;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class NotificationSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private boolean isTechnician = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if user is technician from intent
        isTechnician = getIntent().getBooleanExtra("IS_TECH", false);

        // USE THE CORRECT LAYOUT BASED ON ROLE
        if (isTechnician) {
            setContentView(R.layout.activity_technician_notification_settings);
        } else {
            setContentView(R.layout.activity_notification_settings);
        }

        prefs = getSharedPreferences("AquaTechPrefs", MODE_PRIVATE);

        setupStatusBar();
        initializeViews();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (isTechnician) {
            setupTechnicianToggles();
        } else {
            setupCustomerToggles();
        }
    }

    private void setupTechnicianToggles() {
        SwitchCompat swNewTasks = findViewById(R.id.switchNewTasks);
        SwitchCompat swChat = findViewById(R.id.switchChatNotif);
        SwitchCompat swSound = findViewById(R.id.switchTechSound);
        SwitchCompat swVibrate = findViewById(R.id.switchTechVibration);

        if (swNewTasks != null) {
            swNewTasks.setChecked(prefs.getBoolean("tech_new_tasks", true));
            swNewTasks.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("tech_new_tasks", isChecked).apply());
        }
        if (swChat != null) {
            swChat.setChecked(prefs.getBoolean("tech_chat_notif", true));
            swChat.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("tech_chat_notif", isChecked).apply());
        }
        if (swSound != null) {
            swSound.setChecked(prefs.getBoolean("tech_notif_sound", true));
            swSound.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("tech_notif_sound", isChecked).apply());
        }
        if (swVibrate != null) {
            swVibrate.setChecked(prefs.getBoolean("tech_notif_vibrate", true));
            swVibrate.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("tech_notif_vibrate", isChecked).apply());
        }
    }

    private void setupCustomerToggles() {
        SwitchCompat swStatus = findViewById(R.id.switchServiceStatus);
        SwitchCompat swArrival = findViewById(R.id.switchTechArrival);
        SwitchCompat swSound = findViewById(R.id.switchNotifSound);
        SwitchCompat swVibrate = findViewById(R.id.switchVibration);

        if (swStatus != null) {
            swStatus.setChecked(prefs.getBoolean("cust_service_status", true));
            swStatus.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("cust_service_status", isChecked).apply());
        }
        if (swArrival != null) {
            swArrival.setChecked(prefs.getBoolean("cust_tech_arrival", true));
            swArrival.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("cust_tech_arrival", isChecked).apply());
        }
        if (swSound != null) {
            swSound.setChecked(prefs.getBoolean("cust_notif_sound", true));
            swSound.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("cust_notif_sound", isChecked).apply());
        }
        if (swVibrate != null) {
            swVibrate.setChecked(prefs.getBoolean("cust_notif_vibrate", true));
            swVibrate.setOnCheckedChangeListener((v, isChecked) -> prefs.edit().putBoolean("cust_notif_vibrate", isChecked).apply());
        }
    }
}
