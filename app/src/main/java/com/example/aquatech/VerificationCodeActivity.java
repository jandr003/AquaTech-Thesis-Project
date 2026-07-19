package com.example.aquatech;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

public class VerificationCodeActivity extends AppCompatActivity {

    private String phoneNumber, mobile;
    private EditText etOtpCode;
    private Button btnResetPassword;
    private TextView yourMobileNum, yourResendCode;
    private DatabaseReference usersRef, resetsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        // Firebase Init
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        resetsRef = FirebaseDatabase.getInstance().getReference("PasswordResets");

        etOtpCode = findViewById(R.id.etOtpCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        yourMobileNum = findViewById(R.id.yourMobileNum);
        yourResendCode = findViewById(R.id.yourResendCode);

        phoneNumber = getIntent().getStringExtra("phoneNumber");
        mobile = getIntent().getStringExtra("mobile"); // e.g. 09123456789
        
        yourMobileNum.setText("We sent a code to " + (phoneNumber != null ? phoneNumber : mobile));

        setupStatusBar();
        createNotificationChannel();

        btnResetPassword.setOnClickListener(v -> {
            String code = etOtpCode.getText().toString().trim();
            if (!code.isEmpty()) {
                verifyWithFirebase(code);
            } else {
                Toast.makeText(this, "Enter verification code", Toast.LENGTH_SHORT).show();
            }
        });

        yourResendCode.setText(Html.fromHtml("Didn't receive code? <b>RESEND CODE</b>"));
        yourResendCode.setOnClickListener(v -> resendOtp());
    }

    private void verifyWithFirebase(String inputCode) {
        if (mobile == null) return;

        resetsRef.child(mobile).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firebaseOtp = snapshot.child("otp").getValue(String.class);
                    if (inputCode.equals(firebaseOtp)) {
                        proceedToChangePassword();
                    } else {
                        Toast.makeText(VerificationCodeActivity.this, "Invalid OTP. Try again.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VerificationCodeActivity.this, "No valid OTP found. Please resend.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void proceedToChangePassword() {
        // Hahanapin ang UID base sa mobile number para sa password reset
        usersRef.orderByChild("mobile").equalTo(mobile).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String uid = "";
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        uid = ds.getKey();
                    }
                    
                    // Clear OTP after success
                    resetsRef.child(mobile).removeValue();

                    Intent intent = new Intent(VerificationCodeActivity.this, ConfirmPasswordActivity.class);
                    intent.putExtra("USER_ID", uid);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(VerificationCodeActivity.this, "Account data not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void resendOtp() {
        if (mobile == null) return;
        
        String newOtp = generateOtp(6);
        Map<String, Object> otpData = new HashMap<>();
        otpData.put("otp", newOtp);
        otpData.put("timestamp", ServerValue.TIMESTAMP);

        resetsRef.child(mobile).setValue(otpData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showOtpNotification(newOtp);
                Toast.makeText(this, "New code sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showOtpNotification(String otpCode) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "otp_channel")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("AquaTech OTP Code")
                .setContentText("Your new verification code is: " + otpCode)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(this).notify(1001, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private String generateOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("otp_channel", "OTP Channel", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
