package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

        View btnBack = findViewById(R.id.btnBackProfile);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAnalytics = findViewById(R.id.btnProfileAnalytics);
        if (btnAnalytics != null) {
            btnAnalytics.setOnClickListener(v -> {
                Intent intent = new Intent(this, MonthlyAnalyticsActivity.class);
                startActivity(intent);
            });
        }

        View btnManageItems = findViewById(R.id.btnProfileManageItems);
        if (btnManageItems != null) {
            btnManageItems.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminManageItemsActivity.class);
                startActivity(intent);
            });
        }

        View btnAddTech = findViewById(R.id.btnProfileAddTech);
        if (btnAddTech != null) {
            btnAddTech.setOnClickListener(v -> {
                Intent intent = new Intent(this, ManageTechniciansActivity.class);
                startActivity(intent);
            });
        }

        View btnSettings = findViewById(R.id.btnProfileSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(this, AdminSettingsActivity.class);
                startActivity(intent);
            });
        }

        View btnLogout = findViewById(R.id.btnProfileLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void displayAdminData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getEmail() != null ? user.getEmail().toLowerCase().trim() : "";
            if (profileEmail != null) {
                profileEmail.setText(email);
            }

            String uid = user.getUid();
            FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String fullName = snapshot.child("fullName").getValue(String.class);
                                if (fullName != null && !fullName.isEmpty()) {
                                    if (profileName != null) {
                                        profileName.setText(fullName);
                                    }
                                    return;
                                }
                            }
                            if (profileName != null) {
                                profileName.setText(getDisplayNameByEmail(email));
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (profileName != null) {
                                profileName.setText(getDisplayNameByEmail(email));
                            }
                        }
                    });
        }
    }

    private String getDisplayNameByEmail(String email) {
        if (email != null && (email.equals("management@aquasmartguard.ph") || email.equals("admin@aquasmartguard.ph"))) {
            return "Administrator";
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
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
