package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
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

public class TechnicianEditProfileActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private DatabaseReference userRef, techRef;

    private EditText etFullName, etPhone, etEmail;
    private TextView tvTechEditUID, tvTechEditSpecialization, tvTechEditAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
            techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(user.getUid());
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
        View btnBack = findViewById(R.id.btnBackTechEditProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etTechEditFullName);
        etPhone = findViewById(R.id.etTechEditPhone);
        etEmail = findViewById(R.id.etTechEditEmail);

        tvTechEditUID = findViewById(R.id.tvTechEditUID);
        tvTechEditSpecialization = findViewById(R.id.tvTechEditSpecialization);
        tvTechEditAddress = findViewById(R.id.tvTechEditAddress);

        View btnSave = findViewById(R.id.btnSaveTechEditProfile);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveProfileChanges());
        }
    }

    private void loadTechnicianData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (etEmail != null) etEmail.setText(user.getEmail());

            if (tvTechEditUID != null) {
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                tvTechEditUID.setText(getTechIdFromEmail(email));
            }
        }

        if (userRef != null) {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null || name.isEmpty()) name = snapshot.child("name").getValue(String.class);
                    if (etFullName != null && name != null) etFullName.setText(name);

                    String mobile = snapshot.child("mobileNumber").getValue(String.class);
                    if (mobile == null || mobile.isEmpty()) mobile = snapshot.child("phone").getValue(String.class);
                    if (etPhone != null && mobile != null) etPhone.setText(mobile);

                    String spec = snapshot.child("specialization").getValue(String.class);
                    if (tvTechEditSpecialization != null && spec != null && !spec.isEmpty()) {
                        tvTechEditSpecialization.setText(spec);
                    }

                    String addr = snapshot.child("address").getValue(String.class);
                    if (tvTechEditAddress != null && addr != null && !addr.isEmpty()) {
                        tvTechEditAddress.setText(addr);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void saveProfileChanges() {
        String name = etFullName != null ? etFullName.getText().toString().trim() : "";
        String phone = etPhone != null ? etPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            if (etFullName != null) etFullName.setError("Full name required");
            return;
        }

        if (userRef != null) {
            userRef.child("fullName").setValue(name);
            userRef.child("name").setValue(name);
            userRef.child("mobileNumber").setValue(phone);
            userRef.child("phone").setValue(phone);
        }

        if (techRef != null) {
            techRef.child("fullName").setValue(name);
            techRef.child("name").setValue(name);
            techRef.child("mobileNumber").setValue(phone);
        }

        Toast.makeText(this, "Technician Profile updated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getTechIdFromEmail(String email) {
        if (email.contains("john@")) return "TECH-0001";
        if (email.contains("jhonny@")) return "TECH-0002";
        if (email.contains("ricky@")) return "TECH-0003";
        if (email.contains("ruel@")) return "TECH-0004";
        if (email.contains("glenda@")) return "TECH-0005";
        if (email.contains("jerry@")) return "TECH-0006";
        if (email.contains("jonnifer@")) return "TECH-0007";
        if (email.contains("marlon@")) return "TECH-0008";
        if (email.contains("selvino@")) return "TECH-0009";
        return "TECH-0001";
    }
}
