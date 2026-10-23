package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianTicketsGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_tickets_guide);

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
        View btnBack = findViewById(R.id.btnBackTicketsGuide);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnViewAssigned = findViewById(R.id.btnGuideViewAssigned);
        if (btnViewAssigned != null) {
            btnViewAssigned.setOnClickListener(v -> showGuideDialog(
                    "How to View Assigned Service Requests",
                    "1. Open the AquaTech app and navigate to the 'Tasks' tab in the bottom navigation bar.\n\n" +
                    "2. All active service requests assigned to you by dispatch will appear in real-time.\n\n" +
                    "3. Tap on any service ticket card to view complete customer details, equipment model, and reported issues."
            ));
        }

        View btnServiceDetails = findViewById(R.id.btnGuideServiceDetails);
        if (btnServiceDetails != null) {
            btnServiceDetails.setOnClickListener(v -> showGuideDialog(
                    "Understanding Service Details",
                    "Inside each assigned service request ticket, you will find:\n\n" +
                    "1. Customer Name & Contact Number\n" +
                    "2. Exact Installation / Service Address\n" +
                    "3. Water Purification Model (e.g. Firewall™ UV, Filter Unit)\n" +
                    "4. Reported Symptom / Problem Description\n" +
                    "5. Scheduled Date & Time Slot"
            ));
        }

        View btnUpdateStatus = findViewById(R.id.btnGuideUpdateStatus);
        if (btnUpdateStatus != null) {
            btnUpdateStatus.setOnClickListener(v -> showGuideDialog(
                    "How to Update Service Status",
                    "1. When arriving at the site, tap 'Update Status' and set the request to 'In Progress'.\n\n" +
                    "2. Perform the required inspection, filter replacement, or repair.\n\n" +
                    "3. Once finished and verified, tap 'Update Status' -> 'Completed' to generate the technical service report."
            ));
        }

        View btnMissingRequest = findViewById(R.id.btnGuideMissingRequest);
        if (btnMissingRequest != null) {
            btnMissingRequest.setOnClickListener(v -> showGuideDialog(
                    "Missing or Incorrect Service Request",
                    "If an assigned job is missing or has incorrect customer/address details:\n\n" +
                    "1. Pull down to refresh the Tasks screen.\n\n" +
                    "2. Ensure your 'Online Availability' switch is turned ON in Settings.\n\n" +
                    "3. If details are wrong, contact Dispatch / Help Center immediately via the Call or Chat support buttons."
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
