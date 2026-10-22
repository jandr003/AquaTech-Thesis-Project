package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianAboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_about);

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
        View btnBack = findViewById(R.id.btnBackAbout);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAboutApp = findViewById(R.id.btnAboutApp);
        if (btnAboutApp != null) {
            btnAboutApp.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        }

        View btnTerms = findViewById(R.id.btnTermsOfServiceAbout);
        if (btnTerms != null) {
            btnTerms.setOnClickListener(v -> startActivity(new Intent(this, TermsAndConditionActivity.class)));
        }

        View btnLicenses = findViewById(R.id.btnOpenSourceLicensesAbout);
        if (btnLicenses != null) {
            btnLicenses.setOnClickListener(v -> showInfoDialog(
                    "Open-Source Licenses",
                    "• Firebase Android SDK (Apache 2.0)\n" +
                    "• AndroidX & Material Components (Apache 2.0)\n" +
                    "• Glide Image Loading Library (BSD/MIT)\n" +
                    "• iTextPDF Library (AGPL)\n" +
                    "• Lottie Animations (Apache 2.0)"
            ));
        }
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
