package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianProceduresGuideActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_procedures_guide);

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
        View btnBack = findViewById(R.id.btnBackProceduresGuide);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnBeforeService = findViewById(R.id.btnGuideBeforeService);
        if (btnBeforeService != null) {
            btnBeforeService.setOnClickListener(v -> showGuideDialog(
                    "Before Starting a Service",
                    "1. Verify ticket assignment and customer address in AquaTech.\n" +
                    "2. Inspect tool kit (wrench, UVC testing gauge, replacement filters).\n" +
                    "3. Wear required PPE (gloves, shoe covers, company ID).\n" +
                    "4. Confirm customer availability before departing for site."
            ));
        }

        View btnConductInspection = findViewById(R.id.btnGuideConductInspection);
        if (btnConductInspection != null) {
            btnConductInspection.setOnClickListener(v -> showGuideDialog(
                    "Conducting a Service Inspection",
                    "1. Perform visual check for water leaks or pressure drops.\n" +
                    "2. Test Firewall™ UVC lamp operation and power connections.\n" +
                    "3. Measure water TDS levels before and after filtration.\n" +
                    "4. Document any physical damage or worn components."
            ));
        }

        View btnRoutineMaintenance = findViewById(R.id.btnGuideRoutineMaintenance);
        if (btnRoutineMaintenance != null) {
            btnRoutineMaintenance.setOnClickListener(v -> showGuideDialog(
                    "Routine Maintenance Procedures",
                    "1. Shut off water supply valve before opening housing.\n" +
                    "2. Replace sediment & carbon block filter cartridges every 6 months.\n" +
                    "3. Clean quartz sleeve and inspect Firewall™ UV lamp.\n" +
                    "4. Flush system for 2-3 minutes and check all push-fit fittings for tight seal."
            ));
        }

        View btnOnsiteIssues = findViewById(R.id.btnGuideOnsiteIssues);
        if (btnOnsiteIssues != null) {
            btnOnsiteIssues.setOnClickListener(v -> showGuideDialog(
                    "Handling On-site Issues",
                    "1. If major plumbing leaks or electrical defects are found, shut main valve.\n" +
                    "2. Document issue with photos and notify Senior Technical Support.\n" +
                    "3. Explain issue clearly to customer and reschedule replacement if parts are missing."
            ));
        }

        View btnSafetyGuidelines = findViewById(R.id.btnGuideSafetyGuidelines);
        if (btnSafetyGuidelines != null) {
            btnSafetyGuidelines.setOnClickListener(v -> showGuideDialog(
                    "Safety Guidelines",
                    "1. Always unplug power cord before touching electrical components or UV ballast.\n" +
                    "2. Never look directly into an active UVC lamp.\n" +
                    "3. Wipe up any water spills immediately to prevent slip hazards.\n" +
                    "4. Follow electrical grounding standards for all dispenser installations."
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
