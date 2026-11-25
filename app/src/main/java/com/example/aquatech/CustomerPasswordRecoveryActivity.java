package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CustomerPasswordRecoveryActivity extends AppCompatActivity {

    private MaterialCardView cardEmail, cardSMS;
    private RadioButton rbEmail, rbSMS;
    private ImageView ivIconEmail, ivIconSMS;
    private TextView tvTitleEmail, tvTitleSMS;
    private MaterialButton btnSendMethod;
    private TextView tvEmailSub, tvPhoneSub;
    private FirebaseAuth mAuth;
    private boolean isEmailSelected = true;
    private String userEmail = "";
    private String userPhone = "";
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_password_recovery);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListeners();
        loadCustomerRecord();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackPassRecovery);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        cardEmail = findViewById(R.id.cardOptionEmail);
        cardSMS = findViewById(R.id.cardOptionSMS);
        rbEmail = findViewById(R.id.rbOptionEmail);
        rbSMS = findViewById(R.id.rbOptionSMS);

        ivIconEmail = findViewById(R.id.ivIconEmail);
        ivIconSMS = findViewById(R.id.ivIconSMS);
        tvTitleEmail = findViewById(R.id.tvTitleEmail);
        tvTitleSMS = findViewById(R.id.tvTitleSMS);

        btnSendMethod = findViewById(R.id.btnSendResetMethod);
        tvEmailSub = findViewById(R.id.tvRegisteredEmailSub);
        tvPhoneSub = findViewById(R.id.tvRegisteredPhoneSub);
    }

    private void setupClickListeners() {
        if (cardEmail != null) {
            cardEmail.setOnClickListener(v -> selectRecoveryMethod(true));
        }

        if (cardSMS != null) {
            cardSMS.setOnClickListener(v -> selectRecoveryMethod(false));
        }

        if (btnSendMethod != null) {
            btnSendMethod.setOnClickListener(v -> executeSelectedRecovery());
        }
    }

    private void selectRecoveryMethod(boolean isEmail) {
        isEmailSelected = isEmail;

        if (rbEmail != null) {
            rbEmail.setChecked(isEmail);
        }
        if (rbSMS != null) {
            rbSMS.setChecked(!isEmail);
        }

        if (cardEmail != null) {
            cardEmail.setStrokeColor(Color.parseColor(isEmail ? "#2196F3" : "#E2E8F0"));
            cardEmail.setStrokeWidth(isEmail ? 6 : 2);
        }

        if (cardSMS != null) {
            cardSMS.setStrokeColor(Color.parseColor(!isEmail ? "#2196F3" : "#E2E8F0"));
            cardSMS.setStrokeWidth(!isEmail ? 6 : 2);
        }

        if (ivIconEmail != null) {
            ivIconEmail.setColorFilter(Color.parseColor(isEmail ? "#2196F3" : "#94A3B8"));
        }
        if (ivIconSMS != null) {
            ivIconSMS.setColorFilter(Color.parseColor(!isEmail ? "#2196F3" : "#94A3B8"));
        }

        if (tvTitleEmail != null) {
            tvTitleEmail.setTextColor(Color.parseColor(isEmail ? "#0284C7" : "#64748B"));
        }
        if (tvTitleSMS != null) {
            tvTitleSMS.setTextColor(Color.parseColor(!isEmail ? "#0284C7" : "#64748B"));
        }

        if (btnSendMethod != null) {
            btnSendMethod.setText(isEmail ? "Send Password Reset Link" : "Send SMS Verification Code");
        }
    }

    private void executeSelectedRecovery() {
        if (isEmailSelected) {
            Intent intent = new Intent(this, ForgetPasswordActivity.class);
            if (!userEmail.isEmpty()) {
                intent.putExtra("PREFILL_EMAIL", userEmail);
            }
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ForgetPasswordActivity.class);
            if (!userPhone.isEmpty()) {
                intent.putExtra("PREFILL_PHONE", userPhone);
            }
            startActivity(intent);
        }
    }

    private void loadCustomerRecord() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        if (user.getEmail() != null) {
            userEmail = user.getEmail();
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    String dbEmail = snapshot.child("email").getValue(String.class);
                    if (dbEmail != null && !dbEmail.isEmpty()) {
                        userEmail = dbEmail;
                    }

                    String dbPhone = snapshot.child("phone").getValue(String.class);
                    if (dbPhone == null) {
                        dbPhone = snapshot.child("customerPhone").getValue(String.class);
                    }
                    if (dbPhone != null && !dbPhone.isEmpty()) {
                        userPhone = dbPhone;
                    }

                    if (tvEmailSub != null) {
                        tvEmailSub.setText(!userEmail.isEmpty() ? ("Send password reset link to " + userEmail) : "Send password reset link to registered email");
                    }

                    if (tvPhoneSub != null) {
                        tvPhoneSub.setText(!userPhone.isEmpty() ? ("Send verification code to " + userPhone) : "Send verification code to registered mobile");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerPasswordRecoveryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
