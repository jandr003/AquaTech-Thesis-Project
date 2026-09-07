package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminProfileActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private TextView profileName, profileEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);
        
        mAuth = FirebaseAuth.getInstance();
        setupStatusBar();
        initializeViews();
        displayAdminData();
    }

    private void initializeViews() {
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);

        findViewById(R.id.btnBackProfile).setOnClickListener(v -> finish());

        findViewById(R.id.btnProfileAnalytics).setOnClickListener(v -> {
            startActivity(new Intent(this, MonthlyAnalyticsActivity.class));
        });

        findViewById(R.id.btnProfileSettings).setOnClickListener(v -> {
            startActivity(new Intent(this, AdminSettingsActivity.class));
        });

        findViewById(R.id.btnProfileLogout).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void displayAdminData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail().toLowerCase().trim() : "";
            profileEmail.setText(email);
            profileName.setText(getDisplayNameByEmail(email));
        }
    }

    private String getDisplayNameByEmail(String email) {
        if (email.equals("management@aquasmartguard.ph") || email.equals("admin@aquasmartguard.ph")) {
            return "Administrator";
        } else {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
        }
        return "Administrator";
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
