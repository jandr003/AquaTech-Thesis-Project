package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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

import java.util.HashMap;
import java.util.Map;

public class EditAddressActivity extends AppCompatActivity {

    private EditText etAddressInput, etLandmarkInput;
    private FirebaseAuth mAuth;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_address);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListeners();
        loadCurrentSavedAddress();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackEditAddress);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etAddressInput = findViewById(R.id.etFullAddressInput);
        etLandmarkInput = findViewById(R.id.etAddressLandmark);
    }

    private void setupClickListeners() {
        View btnPickMap = findViewById(R.id.btnPickOnMap);
        if (btnPickMap != null) {
            btnPickMap.setOnClickListener(v -> startActivity(new Intent(this, MapPickerActivity.class)));
        }

        View btnSave = findViewById(R.id.btnSaveAddress);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveAddressToFirebase());
        }
    }

    private void loadCurrentSavedAddress() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    String addr = snapshot.child("address").getValue(String.class);
                    if (addr != null && etAddressInput != null) {
                        etAddressInput.setText(addr);
                    }

                    String landmark = snapshot.child("landmark").getValue(String.class);
                    if (landmark != null && etLandmarkInput != null) {
                        etLandmarkInput.setText(landmark);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditAddressActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAddressToFirebase() {
        String newAddress = etAddressInput != null ? etAddressInput.getText().toString().trim() : "";
        String landmark = etLandmarkInput != null ? etLandmarkInput.getText().toString().trim() : "";

        if (newAddress.isEmpty()) {
            Toast.makeText(this, "Please enter complete service address.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("address", newAddress);
        if (!landmark.isEmpty()) {
            updates.put("landmark", landmark);
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditAddressActivity.this, "Service address updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditAddressActivity.this, "Failed to update address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
