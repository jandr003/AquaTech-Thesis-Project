package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

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

        View btnAddTech = findViewById(R.id.btnProfileAddTech);
        if (btnAddTech != null) {
            btnAddTech.setOnClickListener(v -> showAddTechnicianDialog());
        }

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

    private void showAddTechnicianDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_admin_add_tech);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = dialog.findViewById(R.id.etAddTechName);
        EditText etEmail = dialog.findViewById(R.id.etAddTechEmail);
        EditText etPass = dialog.findViewById(R.id.etAddTechPass);

        dialog.findViewById(R.id.btnCancelAddTech).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btnConfirmAddTech).setOnClickListener(v -> {
            String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
            String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pass = etPass != null && etPass.getText() != null ? etPass.getText().toString().trim() : "";

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveTechnicianToFirebase(name, email, pass);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveTechnicianToFirebase(String name, String email, String pass) {
        DatabaseReference techsRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");

        String uid = techsRef.push().getKey();
        if (uid == null) uid = "TECH_" + System.currentTimeMillis();

        Map<String, Object> data = new HashMap<>();
        data.put("fullName", name);
        data.put("email", email);
        data.put("accountStatus", "active");
        data.put("role", "technician");
        data.put("techId", uid);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Users/" + uid, data);
        updates.put("/Technicians/" + uid, data);

        FirebaseDatabase.getInstance(DB_URL).getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Technician account created successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show();
                    }
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
