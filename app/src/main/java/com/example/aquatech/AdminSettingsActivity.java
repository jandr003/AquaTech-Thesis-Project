package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        prefs = getSharedPreferences("AquaTechAdminPrefs", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Change Password
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            startActivity(new Intent(this, ConfirmPasswordActivity.class));
        });

        // Notifications
        SwitchMaterial swRequest = findViewById(R.id.switchRequestAlerts);
        SwitchMaterial swUrgent = findViewById(R.id.switchUrgentAlerts);

        swRequest.setChecked(prefs.getBoolean("admin_notif_requests", true));
        swUrgent.setChecked(prefs.getBoolean("admin_notif_urgent", true));

        swRequest.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean("admin_notif_requests", isChecked).apply());
        
        swUrgent.setOnCheckedChangeListener((buttonView, isChecked) -> 
            prefs.edit().putBoolean("admin_notif_urgent", isChecked).apply());

        // Preferences
        findViewById(R.id.btnLanguage).setOnClickListener(v -> showLanguageDialog());
        findViewById(R.id.btnDisplay).setOnClickListener(v -> Toast.makeText(this, "Display settings coming soon", Toast.LENGTH_SHORT).show());

        // Support
        findViewById(R.id.btnHelpCenter).setOnClickListener(v -> startActivity(new Intent(this, HelpCenterActivity.class)));
        findViewById(R.id.btnAbout).setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        
        // Admin Profile (if button exists in layout)
        View btnProfile = findViewById(R.id.btnAdminProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                // You can add logic here to show a profile edit screen or dialog
                Toast.makeText(this, "Profile management coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showLanguageDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_language);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.btnLangEnglish).setOnClickListener(v -> {
            prefs.edit().putString("app_language", "en").apply();
            dialog.dismiss();
            Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.btnCancelLang).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
