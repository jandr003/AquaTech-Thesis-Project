package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class TypingIndicatorSettingsActivity extends AppCompatActivity {

    private SwitchMaterial switchTypingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing_indicator_settings);

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
        View btnBack = findViewById(R.id.btnBackTypingIndicator);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        switchTypingIndicator = findViewById(R.id.switchTypingIndicator);
        if (switchTypingIndicator != null) {
            switchTypingIndicator.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Toast.makeText(this, isChecked ? "Typing indicator enabled" : "Typing indicator disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
