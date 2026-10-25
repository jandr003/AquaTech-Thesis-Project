package com.example.aquatech;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class TechnicianDisplayActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private Slider sliderFontSize;
    private SwitchMaterial switchSerialNumbers, switchLastServiceDate, switchDistanceToJobSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_display);

        prefs = getSharedPreferences("AquaTechPrefs_Tech", MODE_PRIVATE);

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
        View btnBack = findViewById(R.id.btnBackTechDisplay);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        sliderFontSize = findViewById(R.id.sliderFontSize);
        switchSerialNumbers = findViewById(R.id.switchSerialNumbers);
        switchLastServiceDate = findViewById(R.id.switchLastServiceDate);
        switchDistanceToJobSite = findViewById(R.id.switchDistanceToJobSite);

        // Load saved preferences
        if (sliderFontSize != null) {
            float savedSize = prefs.getFloat("tech_font_size", 14.0f);
            sliderFontSize.setValue(savedSize);
        }

        if (switchSerialNumbers != null) {
            switchSerialNumbers.setChecked(prefs.getBoolean("tech_show_serials", true));
        }

        if (switchLastServiceDate != null) {
            switchLastServiceDate.setChecked(prefs.getBoolean("tech_show_service_date", true));
        }

        if (switchDistanceToJobSite != null) {
            switchDistanceToJobSite.setChecked(prefs.getBoolean("tech_show_distance", true));
        }

        // Save Preferences Button
        View btnSave = findViewById(R.id.btnSaveDisplayPreferences);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();
                if (sliderFontSize != null) {
                    editor.putFloat("tech_font_size", sliderFontSize.getValue());
                }
                if (switchSerialNumbers != null) {
                    editor.putBoolean("tech_show_serials", switchSerialNumbers.isChecked());
                }
                if (switchLastServiceDate != null) {
                    editor.putBoolean("tech_show_service_date", switchLastServiceDate.isChecked());
                }
                if (switchDistanceToJobSite != null) {
                    editor.putBoolean("tech_show_distance", switchDistanceToJobSite.isChecked());
                }
                editor.apply();

                Toast.makeText(this, "Display preferences saved successfully!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }
}
