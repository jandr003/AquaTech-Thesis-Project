package com.example.aquatech;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class ForgetPasswordActivity extends AppCompatActivity {

    private TextView tvForgotHeader, tvForgotPassword;
    private EditText etMobileNumber;
    private Button btnResetPassword;
    private CardView floatingCard;
    private String phoneNumber;
    private CheckBox cbRememberMe;
    private DatabaseReference usersRef, resetsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        resetsRef = FirebaseDatabase.getInstance().getReference("PasswordResets");

        setupStatusBar();
        handlePermissions();
        initializeViews();
        loadPrefs();

        createNotificationChannel();

        btnResetPassword.setOnClickListener(v -> handleReset());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void handlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void initializeViews() {
        tvForgotHeader = findViewById(R.id.tvForgotHeader);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        floatingCard = findViewById(R.id.floatingCard);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        tvForgotHeader.setText("Forgot Your Password?");
        tvForgotPassword.setText("Please enter your registered mobile number");
    }

    private void loadPrefs() {
        SharedPreferences prefs = getSharedPreferences("aquatech_prefs", MODE_PRIVATE);
        boolean remembered = prefs.getBoolean("remember_me", false);
        String savedMobile = prefs.getString("saved_mobile", "");

        cbRememberMe.setChecked(remembered);
        if (remembered && !savedMobile.isEmpty()) {
            etMobileNumber.setText(savedMobile);
        }
    }

    private void handleReset() {
        String rawInput = etMobileNumber.getText().toString().trim();

        if (rawInput.matches("^09\\d{9}$")) {
            phoneNumber = "+63" + rawInput.substring(1);
        } else if (rawInput.matches("^\\+63\\d{10}$")) {
            phoneNumber = rawInput;
        } else {
            Toast.makeText(this, "Please enter a valid 11-digit mobile number", Toast.LENGTH_SHORT).show();
            etMobileNumber.setError("Invalid number");
            return;
        }

        String mobileToSearch = rawInput.matches("^09\\d{9}$") ? rawInput : "0" + rawInput.substring(3);
        
        btnResetPassword.setEnabled(false);
        Toast.makeText(this, "Verifying number...", Toast.LENGTH_SHORT).show();
        
        usersRef.orderByChild("mobile").equalTo(mobileToSearch).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    processOtp(mobileToSearch, rawInput);
                } else {
                    btnResetPassword.setEnabled(true);
                    Toast.makeText(ForgetPasswordActivity.this, "Mobile number not registered.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnResetPassword.setEnabled(true);
                Toast.makeText(ForgetPasswordActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processOtp(String mobileToSearch, String rawInput) {
        SharedPreferences prefs = getSharedPreferences("aquatech_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean("remember_me", true);
            editor.putString("saved_mobile", rawInput);
        } else {
            editor.putBoolean("remember_me", false);
            editor.remove("saved_mobile");
        }
        editor.apply();

        String otpCode = generateOtp();

        // 📍 FIREBASE SYNC: Save OTP to Firebase
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", otpCode);
        otpData.put("timestamp", ServerValue.TIMESTAMP);

        resetsRef.child(mobileToSearch).setValue(otpData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Save OTP locally as fallback
                getSharedPreferences("otp_prefs", MODE_PRIVATE).edit().putString("last_otp", otpCode).apply();

                showOtpNotification(otpCode);

                Intent intent = new Intent(this, VerificationCodeActivity.class);
                intent.putExtra("phoneNumber", phoneNumber);
                intent.putExtra("mobile", mobileToSearch);
                startActivity(intent);
                btnResetPassword.setEnabled(true);
            } else {
                btnResetPassword.setEnabled(true);
                Toast.makeText(this, "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOtpNotification(String otpCode) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "otp_channel")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AquaTech OTP Code")
                .setContentText("Your verification code is: " + otpCode)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1001, builder.build());
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "OTP Channel";
            String description = "Channel for OTP notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("otp_channel", name, importance);
            channel.setDescription(description);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
