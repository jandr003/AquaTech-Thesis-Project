package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianTroubleshootingGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_troubleshooting_guide);

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
        View btnBack = findViewById(R.id.btnBackTroubleGuide);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnAppWontOpen = findViewById(R.id.btnGuideAppWontOpen);
        if (btnAppWontOpen != null) {
            btnAppWontOpen.setOnClickListener(v -> showGuideDialog(
                    "App Won't Open or Keeps Loading",
                    "1. Force close AquaTech from your phone's Recent Apps menu and re-open.\n\n" +
                    "2. Check if your phone has sufficient internal storage space.\n\n" +
                    "3. Go to Phone Settings -> Apps -> AquaTech -> Clear Cache (Do NOT clear data) and restart the app."
            ));
        }

        View btnInternetProblems = findViewById(R.id.btnGuideInternetProblems);
        if (btnInternetProblems != null) {
            btnInternetProblems.setOnClickListener(v -> showGuideDialog(
                    "Internet Connection Problems",
                    "1. Verify that Mobile Data or Wi-Fi is enabled on your device.\n\n" +
                    "2. Toggle Airplane Mode ON for 5 seconds, then OFF to reset cellular connection.\n\n" +
                    "3. AquaTech requires an active data connection to sync real-time Firebase tickets and dispatch updates."
            ));
        }

        View btnRequestsNotLoading = findViewById(R.id.btnGuideRequestsNotLoading);
        if (btnRequestsNotLoading != null) {
            btnRequestsNotLoading.setOnClickListener(v -> showGuideDialog(
                    "Assigned Service Requests Not Loading",
                    "1. Go to Tasks screen and pull down to refresh the list.\n\n" +
                    "2. Ensure you are logged into your correct technician account.\n\n" +
                    "3. Verify with Dispatch if any new tickets were assigned to your Technician ID."
            ));
        }

        View btnStatusNotUpdating = findViewById(R.id.btnGuideStatusNotUpdating);
        if (btnStatusNotUpdating != null) {
            btnStatusNotUpdating.setOnClickListener(v -> showGuideDialog(
                    "Service Status Not Updating",
                    "1. Make sure 'Online Availability' is enabled in your Settings screen.\n\n" +
                    "2. Check if your device signal indicator shows active internet access.\n\n" +
                    "3. Status updates will retry automatically once a stable connection is restored."
            ));
        }

        View btnReportFailed = findViewById(R.id.btnGuideReportFailed);
        if (btnReportFailed != null) {
            btnReportFailed.setOnClickListener(v -> showGuideDialog(
                    "Service Report Submission Failed",
                    "1. Ensure customer signature, technical findings, and work done are all filled out.\n\n" +
                    "2. Check if your device allows camera/photo permissions for attached report photos.\n\n" +
                    "3. Saved PDF reports remain in memory and will auto-upload when reconnected."
            ));
        }

        View btnNotifNotReceived = findViewById(R.id.btnGuideNotifNotReceived);
        if (btnNotifNotReceived != null) {
            btnNotifNotReceived.setOnClickListener(v -> showGuideDialog(
                    "Notifications Not Received",
                    "1. Go to Phone Settings -> Notifications -> AquaTech and ensure 'Allow Notifications' is ON.\n\n" +
                    "2. Turn OFF Battery Saver / Do Not Disturb mode while on duty.\n\n" +
                    "3. Open AquaTech Settings -> Notification Alerts to verify alert permissions."
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
