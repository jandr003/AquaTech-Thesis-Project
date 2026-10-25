package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianPrivacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_privacy);

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
        View btnBack = findViewById(R.id.btnBackTechPrivacy);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnPrivacyPolicy = findViewById(R.id.btnTechPrivacyPolicy);
        if (btnPrivacyPolicy != null) {
            btnPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(this, TermsAndConditionActivity.class)));
        }

        View btnAppPermissions = findViewById(R.id.btnTechAppPermissions);
        if (btnAppPermissions != null) {
            btnAppPermissions.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            });
        }

        View btnDataUsage = findViewById(R.id.btnTechDataUsage);
        if (btnDataUsage != null) {
            btnDataUsage.setOnClickListener(v -> showGuideDialog(
                    "Data Usage",
                    "1. Technician Data: Account UID, name, phone, availability status, and GPS location during active service visits.\n\n" +
                    "2. Customer Data: Contact information and installation addresses are accessible only for assigned service tickets.\n\n" +
                    "3. Storage: Service reports and uploaded equipment photos are securely stored in encrypted Firebase Cloud Storage."
            ));
        }

        View btnPrivacySupport = findViewById(R.id.btnTechPrivacySupport);
        if (btnPrivacySupport != null) {
            btnPrivacySupport.setOnClickListener(v -> showGuideDialog(
                    "Privacy Support",
                    "For any privacy inquiries, data correction, or permission concerns:\n\n" +
                    "1. Email: privacy@aquasmartguard.ph\n" +
                    "2. Phone: +63 998 961 9695\n" +
                    "3. Address: Aqua Smartguard Corp., GF, Makati City, 1209 Metro Manila"
            ));
        }
    }

    private void showGuideDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got It", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
