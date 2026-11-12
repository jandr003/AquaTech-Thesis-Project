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
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(this, ConfirmPasswordActivity.class);
                startActivity(intent);
            });
        }

        SwitchMaterial swRequest = findViewById(R.id.switchRequestAlerts);
        SwitchMaterial swUrgent = findViewById(R.id.switchUrgentAlerts);

        if (swRequest != null && prefs != null) {
            swRequest.setChecked(prefs.getBoolean("admin_notif_requests", true));
            swRequest.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("admin_notif_requests", isChecked).apply();
            });
        }

        if (swUrgent != null && prefs != null) {
            swUrgent.setChecked(prefs.getBoolean("admin_notif_urgent", true));
            swUrgent.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("admin_notif_urgent", isChecked).apply();
            });
        }

        View btnLanguage = findViewById(R.id.btnLanguage);
        if (btnLanguage != null) {
            btnLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        View btnDisplay = findViewById(R.id.btnDisplay);
        if (btnDisplay != null) {
            btnDisplay.setOnClickListener(v -> {
                Toast.makeText(this, "Display settings coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        View btnHelpCenter = findViewById(R.id.btnHelpCenter);
        if (btnHelpCenter != null) {
            btnHelpCenter.setOnClickListener(v -> {
                Intent intent = new Intent(this, HelpCenterActivity.class);
                startActivity(intent);
            });
        }

        View btnAbout = findViewById(R.id.btnAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
            });
        }

        View btnProfile = findViewById(R.id.btnAdminProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                Toast.makeText(this, "Profile management coming soon", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showLanguageDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_language);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnEng = dialog.findViewById(R.id.btnLangEnglish);
        if (btnEng != null) {
            btnEng.setOnClickListener(v -> {
                if (prefs != null) {
                    prefs.edit().putString("app_language", "en").apply();
                }
                dialog.dismiss();
                Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show();
            });
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelLang);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
