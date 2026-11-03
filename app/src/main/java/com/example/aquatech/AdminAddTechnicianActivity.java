package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AdminAddTechnicianActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private EditText etFullName, etEmail, etPhone, etPassword, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_technician);

        setupStatusBar();
        initializeViews();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackAddTech).setOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etTechFullName);
        etEmail = findViewById(R.id.etTechEmail);
        etPhone = findViewById(R.id.etTechPhone);
        etPassword = findViewById(R.id.etTechPassword);
        etConfirmPassword = findViewById(R.id.etTechConfirmPassword);

        findViewById(R.id.btnSubmitCreateTech).setOnClickListener(v -> handleCreateTechnician());
    }

    private void handleCreateTechnician() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String pass = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.toLowerCase().endsWith("@aquasmartguard.ph")) {
            Toast.makeText(this, "Email must end with @aquasmartguard.ph", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        saveTechnicianData(uid, name, email, phone);
                    } else {
                        String uid = "ASC_T_" + System.currentTimeMillis();
                        saveTechnicianData(uid, name, email, phone);
                    }
                });
    }

    private void saveTechnicianData(String uid, String name, String email, String phone) {
        String formattedTechId = "ASC-T" + (new Random().nextInt(900) + 100);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("techId", formattedTechId);
        data.put("fullName", name);
        data.put("email", email);
        data.put("phone", phone);
        data.put("role", "Technician");
        data.put("accountStatus", "active");
        data.put("userType", "Technician");

        Map<String, Object> updates = new HashMap<>();
        updates.put("/Users/" + uid, data);
        updates.put("/Technicians/" + uid, data);

        FirebaseDatabase.getInstance(DB_URL).getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Technician account created successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to create account in database", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
