package com.example.aquatech;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ValidIDActivity extends AppCompatActivity {

    private ImageView btnBack, ivIdPreview, ivStatusIcon;
    private TextView tvVerificationStatus, tvStatusDescription;
    private MaterialButton btnUpdateId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_valid_id);

        setupStatusBar();
        initializeViews();
        setupClickListeners();
        updateUI("none");
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivIdPreview = findViewById(R.id.ivIdPreview);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvStatusDescription = findViewById(R.id.tvStatusDescription);
        btnUpdateId = findViewById(R.id.btnUpdateId);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnUpdateId != null) {
            btnUpdateId.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Gallery/Camera...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateUI(String status) {
        if (status == null) {
            return;
        }

        switch (status) {
            case "none":
                if (ivStatusIcon != null) {
                    ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info);
                    ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4B91C6")));
                }

                if (tvVerificationStatus != null) {
                    tvVerificationStatus.setText("ACCOUNT NOT VERIFIED");
                    tvVerificationStatus.setTextColor(Color.parseColor("#4B91C6"));
                }

                if (tvStatusDescription != null) {
                    tvStatusDescription.setText("Please upload a valid ID to verify your identity.");
                }

                if (btnUpdateId != null) {
                    btnUpdateId.setText("UPLOAD VALID ID");
                    btnUpdateId.setEnabled(true);
                    btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B91C6")));
                }
                break;

            case "pending":
                if (ivStatusIcon != null) {
                    ivStatusIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
                    ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
                }

                if (tvVerificationStatus != null) {
                    tvVerificationStatus.setText("VERIFICATION PENDING");
                    tvVerificationStatus.setTextColor(Color.parseColor("#FF9800"));
                }

                if (tvStatusDescription != null) {
                    tvStatusDescription.setText("Reviewing your ID. Please wait 24 hours.");
                }

                if (btnUpdateId != null) {
                    btnUpdateId.setText("PENDING REVIEW");
                    btnUpdateId.setEnabled(false);
                    btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                }
                break;

            case "verified":
                if (ivStatusIcon != null) {
                    ivStatusIcon.setImageResource(R.drawable.ic_check_on);
                    ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                }

                if (tvVerificationStatus != null) {
                    tvVerificationStatus.setText("ACCOUNT VERIFIED");
                    tvVerificationStatus.setTextColor(Color.parseColor("#4CAF50"));
                }

                if (tvStatusDescription != null) {
                    tvStatusDescription.setText("Your identity has been confirmed. Thank you!");
                }

                if (btnUpdateId != null) {
                    btnUpdateId.setText("UPDATE VALID ID");
                    btnUpdateId.setEnabled(true);
                    btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                }
                break;
        }
    }
}
