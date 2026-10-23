package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianAccountGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_account_guide);

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
        View btnBack = findViewById(R.id.btnBackAccountGuide);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnUnableToLogin = findViewById(R.id.btnGuideUnableToLogin);
        if (btnUnableToLogin != null) {
            btnUnableToLogin.setOnClickListener(v -> showGuideDialog(
                    "Unable to Log In",
                    "1. Double check that your technician email address (e.g. john@...) is spelled correctly.\n\n" +
                    "2. Ensure Caps Lock is not active when entering your password.\n\n" +
                    "3. Check your internet connection and verify that your account has been approved by dispatch."
            ));
        }

        View btnForgotPassword = findViewById(R.id.btnGuideForgotPassword);
        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v -> {
                startActivity(new Intent(this, ForgetPasswordActivity.class));
            });
        }

        View btnUpdatingProfile = findViewById(R.id.btnGuideUpdatingProfile);
        if (btnUpdatingProfile != null) {
            btnUpdatingProfile.setOnClickListener(v -> showGuideDialog(
                    "Updating Profile Information",
                    "1. Go to Settings -> Account & Security -> Identity Verification.\n\n" +
                    "2. Upload your company ID card or updated contact details.\n\n" +
                    "3. Updated mobile numbers and ID status will be reviewed and verified by management."
            ));
        }

        View btnAccessIssues = findViewById(R.id.btnGuideAccessIssues);
        if (btnAccessIssues != null) {
            btnAccessIssues.setOnClickListener(v -> showGuideDialog(
                    "Account Access Issues",
                    "If you suspect unauthorized access or your account is locked:\n\n" +
                    "1. Immediately change your password via Settings -> Account & Security -> Change Password.\n\n" +
                    "2. Contact dispatch support or AquaTech admin to review account logs and secure your account."
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
