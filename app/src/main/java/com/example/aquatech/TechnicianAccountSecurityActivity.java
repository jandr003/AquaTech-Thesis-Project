package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TechnicianAccountSecurityActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private TextView tvTechUID, tvTechStatus, tvTechPhone, tvTechSince;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_account_security);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        }

        setupStatusBar();
        initializeViews();
        loadTechnicianData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackTechAccountSec);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTechUID = findViewById(R.id.tvTechUID);
        tvTechStatus = findViewById(R.id.tvTechStatus);
        tvTechPhone = findViewById(R.id.tvTechPhone);
        tvTechSince = findViewById(R.id.tvTechSince);

        View btnIdentity = findViewById(R.id.btnTechIdentity);
        if (btnIdentity != null) {
            btnIdentity.setOnClickListener(v -> startActivity(new Intent(this, TechnicianVerificationActivity.class)));
        }

        View btnChangePassword = findViewById(R.id.btnTechChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ConfirmPasswordActivity.class)));
        }

        View btnForgotPassword = findViewById(R.id.btnTechForgotPassword);
        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgetPasswordActivity.class)));
        }

        View btnAssistance = findViewById(R.id.btnTechAccountAssistance);
        if (btnAssistance != null) {
            btnAssistance.setOnClickListener(v -> startActivity(new Intent(this, TechnicianAccountSupportActivity.class)));
        }
    }

    private void loadTechnicianData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (tvTechUID != null) {
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                tvTechUID.setText(getTechIdFromEmail(email));
            }

            if (tvTechSince != null) {
                FirebaseUserMetadata metadata = user.getMetadata();
                if (metadata != null) {
                    long creationTimestamp = metadata.getCreationTimestamp();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    tvTechSince.setText(sdf.format(new Date(creationTimestamp)));
                }
            }

            if (userRef != null) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String status = snapshot.child("verificationStatus").getValue(String.class);
                        if (tvTechStatus != null) {
                            if ("Verified".equalsIgnoreCase(status)) {
                                tvTechStatus.setText("Identity Verified");
                                tvTechStatus.setTextColor(Color.parseColor("#4CAF50"));
                            } else if ("Reviewing".equalsIgnoreCase(status)) {
                                tvTechStatus.setText("Pending Review");
                                tvTechStatus.setTextColor(Color.parseColor("#FFA000"));
                            } else {
                                tvTechStatus.setText("Unverified");
                                tvTechStatus.setTextColor(Color.parseColor("#FF4B4B"));
                            }
                        }

                        String mobile = snapshot.child("mobileNumber").getValue(String.class);
                        if (tvTechPhone != null && mobile != null && !mobile.isEmpty()) {
                            tvTechPhone.setText(mobile);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }
    }

    private String getTechIdFromEmail(String email) {
        if (email.contains("john@")) return "ASC-T001";
        if (email.contains("jhonny@")) return "ASC-T002";
        if (email.contains("ricky@")) return "ASC-T003";
        if (email.contains("ruel@")) return "ASC-T004";
        if (email.contains("glenda@")) return "ASC-T005";
        if (email.contains("jerry@")) return "ASC-T006";
        if (email.contains("jonnifer@")) return "ASC-T007";
        if (email.contains("marlon@")) return "ASC-T008";
        if (email.contains("selvino@")) return "ASC-T009";
        return "ASC-T100";
    }

    private void showAssistanceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Account Assistance")
                .setMessage("For technician account verification, UID reassignment, or account recovery support, please contact Dispatch Admin:\n\n• Tech Support: +63 998 961 9695\n• Email: admin@aquasmartguard.ph\n• Office: GF, Makati City, Metro Manila")
                .setPositiveButton("Got It", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
