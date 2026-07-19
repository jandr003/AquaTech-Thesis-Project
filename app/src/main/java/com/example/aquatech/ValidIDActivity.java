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
import androidx.core.content.ContextCompat;

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

        // HALIMBAWA: Dito mo tatawagin yung data galing Firebase
        // Status pwedeng: "none", "pending", or "verified"
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
        btnBack.setOnClickListener(v -> finish());

        btnUpdateId.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Gallery/Camera...", Toast.LENGTH_SHORT).show();
            // Pagkatapos mag-upload, pwede mong tawagin ang updateUI("pending");
        });
    }

    /**
     * Binabago ang itsura ng screen depende sa status
     * @param status - "none", "pending", "verified"
     */
    private void updateUI(String status) {
        switch (status) {
            case "none":
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4B91C6")));
                tvVerificationStatus.setText("ACCOUNT NOT VERIFIED");
                tvVerificationStatus.setTextColor(Color.parseColor("#4B91C6"));
                tvStatusDescription.setText("Please upload a valid ID to verify your identity.");
                btnUpdateId.setText("UPLOAD VALID ID");
                btnUpdateId.setEnabled(true);
                btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B91C6")));
                break;

            case "pending":
                ivStatusIcon.setImageResource(android.R.drawable.ic_menu_recent_history);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
                tvVerificationStatus.setText("VERIFICATION PENDING");
                tvVerificationStatus.setTextColor(Color.parseColor("#FF9800"));
                tvStatusDescription.setText("Reviewing your ID. Please wait 24 hours.");
                btnUpdateId.setText("PENDING REVIEW");
                btnUpdateId.setEnabled(false);
                btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
                break;

            case "verified":
                ivStatusIcon.setImageResource(R.drawable.ic_check_on);
                ivStatusIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                tvVerificationStatus.setText("ACCOUNT VERIFIED");
                tvVerificationStatus.setTextColor(Color.parseColor("#4CAF50"));
                tvStatusDescription.setText("Your identity has been confirmed. Thank you!");
                btnUpdateId.setText("UPDATE VALID ID");
                btnUpdateId.setEnabled(true);
                btnUpdateId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                break;
        }
    }
}
