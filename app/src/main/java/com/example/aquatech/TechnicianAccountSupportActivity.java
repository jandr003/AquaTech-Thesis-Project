package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TechnicianAccountSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_account_support);

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
        View btnBack = findViewById(R.id.btnBackAccountSupport);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnIdVerif = findViewById(R.id.btnSupportIdVerification);
        if (btnIdVerif != null) {
            btnIdVerif.setOnClickListener(v -> startActivity(new Intent(this, TechnicianVerificationActivity.class)));
        }

        View btnLoginPass = findViewById(R.id.btnSupportLoginPassword);
        if (btnLoginPass != null) {
            btnLoginPass.setOnClickListener(v -> startActivity(new Intent(this, ForgetPasswordActivity.class)));
        }

        View btnMobileUpdate = findViewById(R.id.btnSupportMobileUpdate);
        if (btnMobileUpdate != null) {
            btnMobileUpdate.setOnClickListener(v -> showSupportDialog(
                    "Mobile Number Update",
                    "To update your registered mobile phone number:\n\n" +
                    "1. Ensure your current SIM card is active for SMS verification.\n\n" +
                    "2. Submit your updated number during ID Verification or contact Dispatch Admin for immediate profile update."
            ));
        }

        View btnContactAdmin = findViewById(R.id.btnSupportContactAdmin);
        if (btnContactAdmin != null) {
            btnContactAdmin.setOnClickListener(v -> showAdminContactOptions());
        }
    }

    private void showAdminContactOptions() {
        new AlertDialog.Builder(this)
                .setTitle("Contact Administrator")
                .setMessage("Choose how you would like to reach AquaTech Dispatch Support:\n\n• Phone: +63 998 961 9695\n• Email: admin@aquasmartguard.ph\n• Office: GF, Makati City, Metro Manila")
                .setPositiveButton("Call Admin", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:09989619695"));
                    startActivity(intent);
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showSupportDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Got It", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
