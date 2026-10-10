package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout tilFirstName, tilLastName, tilEmail, tilMobile, tilAddress, tilPassword;
    private EditText etFirstName, etLastName, etEmail, etMobile, etAddress, etPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        initializeViews();
        setupStatusBar();
        setupLoginPrompt();

        findViewById(R.id.btnSignUp).setOnClickListener(v -> performSignup());
        View btnGoogle = findViewById(R.id.btnGoogleSignUp);
        if (btnGoogle != null) btnGoogle.setOnClickListener(v -> Toast.makeText(this, "Connecting to Google Sign-In...", Toast.LENGTH_SHORT).show());
        View btnFacebook = findViewById(R.id.btnFacebookSignUp);
        if (btnFacebook != null) btnFacebook.setOnClickListener(v -> Toast.makeText(this, "Connecting to Facebook Sign-In...", Toast.LENGTH_SHORT).show());
    }

    private void initializeViews() {
        tilFirstName = findViewById(R.id.tilFirstName);
        tilLastName = findViewById(R.id.tilLastName);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);
        tilAddress = findViewById(R.id.tilAddress);
        tilPassword = findViewById(R.id.tilPassword);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
    }

    private void performSignup() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        boolean isValid = true;
        if (firstName.isEmpty()) { tilFirstName.setError("Required"); isValid = false; } else { tilFirstName.setError(null); }
        if (lastName.isEmpty()) { tilLastName.setError("Required"); isValid = false; } else { tilLastName.setError(null); }
        if (email.isEmpty()) { tilEmail.setError("Required"); isValid = false; } else { tilEmail.setError(null); }
        if (mobile.isEmpty()) { tilMobile.setError("Required"); isValid = false; } else { tilMobile.setError(null); }
        if (address.isEmpty()) { tilAddress.setError("Required"); isValid = false; } else { tilAddress.setError(null); }
        if (password.isEmpty()) { tilPassword.setError("Required"); isValid = false; }
        else if (password.length() < 6) { tilPassword.setError("Min 6 characters"); isValid = false; }
        else { tilPassword.setError(null); }

        if (!isValid) return;

        Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserData(userId, firstName, lastName, email, mobile, address);
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserData(String userId, String firstName, String lastName, String email, String mobile, String address) {
        boolean isTechnician = email.toLowerCase().endsWith("@aquasmartguard.ph");
        String role = isTechnician ? "Technician" : "Customer";
        String fullName = firstName + " " + lastName;

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("fullName", fullName);
        userData.put("username", fullName);
        userData.put("email", email);
        userData.put("mobile", mobile);
        userData.put("address", address);
        userData.put("role", role);
        userData.put("userType", role);

        String initialStatus = isTechnician ? "pending" : "active";
        userData.put("accountStatus", initialStatus);

        if (isTechnician) {
            String techId = "ASC-T" + (new Random().nextInt(900) + 100);
            userData.put("techId", techId);

            Map<String, Object> verification = new HashMap<>();
            verification.put("status", "Not Verified");
            userData.put("verification", verification);

            mDatabase.child("Technicians").child(userId).setValue(userData);
        } else {
            String refNo = "ASC-" + (new Random().nextInt(9000) + 1000);
            userData.put("referenceNo", refNo);
            userData.put("avatarResId", R.drawable.man_user_circle_icon);
            userData.put("profileBgColor", "#F0F7FF");
        }

        mDatabase.child("Users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignupActivity.this, role + " Registered Successfully!", Toast.LENGTH_SHORT).show();

                        if (isTechnician) {
                            startActivity(new Intent(SignupActivity.this, TechnicianVerificationActivity.class));
                        } else {
                            startActivity(new Intent(SignupActivity.this, CustomerSetupActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(SignupActivity.this, "Data Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupLoginPrompt() {
        TextView tvLoginPrompt = findViewById(R.id.tvLoginPrompt);
        String text = "Already have an account? LOGIN here.";
        SpannableString ss = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                finish();
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
                ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                ds.setColor(Color.parseColor("#1C6EA4"));
            }
        };
        ss.setSpan(new StyleSpan(Typeface.BOLD), 25, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(clickableSpan, 25, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLoginPrompt.setText(ss);
        tvLoginPrompt.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
