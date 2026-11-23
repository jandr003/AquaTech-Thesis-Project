package com.example.aquatech;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerVerificationDetailsActivity extends AppCompatActivity {

    private ImageView ivBannerIcon, icStepBasic, icStepSemi, icStepFull;
    private TextView tvBannerTitle, tvBannerSubtitle;
    private TextView tvLevelVal, tvIdTypeVal, tvIdNumVal, tvStatusVal, tvDateVal;
    private FirebaseAuth mAuth;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_verification_details);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        loadVerificationRecord();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackVerificationDetails);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ivBannerIcon = findViewById(R.id.ivVerifBannerIcon);
        tvBannerTitle = findViewById(R.id.tvVerifBannerTitle);
        tvBannerSubtitle = findViewById(R.id.tvVerifBannerSubtitle);

        tvLevelVal = findViewById(R.id.tvVerifLevelVal);
        tvIdTypeVal = findViewById(R.id.tvVerifIdTypeVal);
        tvIdNumVal = findViewById(R.id.tvVerifIdNumberVal);
        tvStatusVal = findViewById(R.id.tvVerifStatusVal);
        tvDateVal = findViewById(R.id.tvVerifDateVal);

        icStepBasic = findViewById(R.id.icStepBasic);
        icStepSemi = findViewById(R.id.icStepSemi);
        icStepFull = findViewById(R.id.icStepFull);
    }

    private void loadVerificationRecord() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (s.exists()) {
                    Boolean isVerified = s.child("govIdVerified").getValue(Boolean.class);
                    if (isVerified == null) {
                        isVerified = s.child("isVerified").getValue(Boolean.class);
                    }

                    String level = s.child("verificationLevel").getValue(String.class);
                    if (level == null || level.isEmpty()) {
                        level = (isVerified != null && isVerified) ? "Fully Verified" : "Basic Level";
                    }

                    String idType = s.child("govIdType").getValue(String.class);
                    if (idType == null) {
                        idType = s.child("idType").getValue(String.class);
                    }

                    String idNum = s.child("govIdNumber").getValue(String.class);
                    if (idNum == null) {
                        idNum = s.child("idNumber").getValue(String.class);
                    }

                    String verifDate = s.child("verificationDate").getValue(String.class);
                    if (verifDate == null) {
                        verifDate = s.child("verifiedAt").getValue(String.class);
                    }

                    if (tvLevelVal != null) {
                        tvLevelVal.setText(level);
                    }

                    if (tvIdTypeVal != null) {
                        tvIdTypeVal.setText(idType != null && !idType.isEmpty() ? idType : "Government-issued ID");
                    }

                    if (tvIdNumVal != null) {
                        if (idNum != null && idNum.length() >= 4) {
                            tvIdNumVal.setText("••••••••" + idNum.substring(idNum.length() - 4));
                        } else {
                            tvIdNumVal.setText("••••••••1234");
                        }
                    }

                    if (tvDateVal != null) {
                        tvDateVal.setText(verifDate != null && !verifDate.isEmpty() ? verifDate : "Recorded Date");
                    }

                    if (isVerified != null && isVerified) {
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Fully Verified");
                            tvBannerTitle.setTextColor(Color.parseColor("#0369A1"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("Your account verification is complete and approved.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Approved");
                            tvStatusVal.setTextColor(Color.parseColor("#0284C7"));
                        }
                        if (icStepBasic != null) icStepBasic.setColorFilter(Color.parseColor("#0284C7"));
                        if (icStepSemi != null) icStepSemi.setColorFilter(Color.parseColor("#0284C7"));
                        if (icStepFull != null) icStepFull.setColorFilter(Color.parseColor("#0284C7"));
                    } else if ("Semi-Verified".equalsIgnoreCase(level)) {
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Semi-Verified");
                            tvBannerTitle.setTextColor(Color.parseColor("#0284C7"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("Additional verification requirements completed.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Under Review");
                            tvStatusVal.setTextColor(Color.parseColor("#0284C7"));
                        }
                        if (icStepBasic != null) icStepBasic.setColorFilter(Color.parseColor("#16A34A"));
                        if (icStepSemi != null) icStepSemi.setColorFilter(Color.parseColor("#16A34A"));
                        if (icStepFull != null) icStepFull.setColorFilter(Color.parseColor("#94A3B8"));
                    } else {
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Basic Level");
                            tvBannerTitle.setTextColor(Color.parseColor("#D97706"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("Initial account info completed. Upload ID for full verification.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Basic Registration");
                            tvStatusVal.setTextColor(Color.parseColor("#D97706"));
                        }
                        if (icStepBasic != null) icStepBasic.setColorFilter(Color.parseColor("#16A34A"));
                        if (icStepSemi != null) icStepSemi.setColorFilter(Color.parseColor("#94A3B8"));
                        if (icStepFull != null) icStepFull.setColorFilter(Color.parseColor("#94A3B8"));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerVerificationDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
