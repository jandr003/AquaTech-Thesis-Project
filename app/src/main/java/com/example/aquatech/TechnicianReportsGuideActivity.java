package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianReportsGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_reports_guide);

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
        View btnBack = findViewById(R.id.btnBackReportsGuide);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnCreateReport = findViewById(R.id.btnGuideCreateReport);
        if (btnCreateReport != null) {
            btnCreateReport.setOnClickListener(v -> showGuideDialog(
                    "How to Create a Service Report",
                    "1. Open the completed or in-progress task from your Tasks screen.\n\n" +
                    "2. Tap the 'Generate Service Report' button at the bottom.\n\n" +
                    "3. The app will pre-fill the customer details, ticket number, technician name, and date automatically."
            ));
        }

        View btnRequiredInfo = findViewById(R.id.btnGuideRequiredInfo);
        if (btnRequiredInfo != null) {
            btnRequiredInfo.setOnClickListener(v -> showGuideDialog(
                    "Required Report Information",
                    "Every technician service report must contain:\n\n" +
                    "1. Work Performed (Filter replacement, UV lamp change, leak repair, etc.)\n" +
                    "2. Technical Findings & Water Quality Readings\n" +
                    "3. Replacement Parts Used (with serial numbers if applicable)\n" +
                    "4. Technician Remarks & Recommendations\n" +
                    "5. Customer Signature"
            ));
        }

        View btnSubmitReport = findViewById(R.id.btnGuideSubmitReport);
        if (btnSubmitReport != null) {
            btnSubmitReport.setOnClickListener(v -> showGuideDialog(
                    "How to Submit a Service Report",
                    "1. Review all entered technical findings and customer signature.\n\n" +
                    "2. Tap 'Submit PDF Report'.\n\n" +
                    "3. The generated PDF will be saved, uploaded to Firebase Storage, and automatically shared with customer records."
            ));
        }

        View btnSubmissionIssues = findViewById(R.id.btnGuideSubmissionIssues);
        if (btnSubmissionIssues != null) {
            btnSubmissionIssues.setOnClickListener(v -> showGuideDialog(
                    "Report Submission Issues",
                    "If your report fails to submit:\n\n" +
                    "1. Ensure all mandatory fields (remarks, work done, signature) are filled.\n\n" +
                    "2. Check your mobile internet or Wi-Fi connection.\n\n" +
                    "3. If connection is unstable, saved reports will auto-sync once back online."
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
