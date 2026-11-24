package com.example.aquatech;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class CustomerNotificationSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        prefs = getSharedPreferences("AquaTechPrefs", MODE_PRIVATE);
        initializeViews();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupCustomerToggles();
    }

    private void setupCustomerToggles() {
        SwitchCompat swStatus = findViewById(R.id.switchServiceStatus);
        SwitchCompat swArrival = findViewById(R.id.switchTechArrival);
        SwitchCompat swSound = findViewById(R.id.switchNotifSound);
        SwitchCompat swVibrate = findViewById(R.id.switchVibration);

        if (swStatus != null && prefs != null) {
            swStatus.setChecked(prefs.getBoolean("cust_service_status", true));
            swStatus.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("cust_service_status", isChecked).apply();
            });
        }

        if (swArrival != null && prefs != null) {
            swArrival.setChecked(prefs.getBoolean("cust_tech_arrival", true));
            swArrival.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("cust_tech_arrival", isChecked).apply();
            });
        }

        if (swSound != null && prefs != null) {
            swSound.setChecked(prefs.getBoolean("cust_notif_sound", true));
            swSound.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("cust_notif_sound", isChecked).apply();
            });
        }

        if (swVibrate != null && prefs != null) {
            swVibrate.setChecked(prefs.getBoolean("cust_notif_vibrate", true));
            swVibrate.setOnCheckedChangeListener((v, isChecked) -> {
                prefs.edit().putBoolean("cust_notif_vibrate", isChecked).apply();
            });
        }
    }
}
