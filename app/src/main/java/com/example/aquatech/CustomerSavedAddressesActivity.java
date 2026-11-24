package com.example.aquatech;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class CustomerSavedAddressesActivity extends AppCompatActivity {

    private TextView tvAddressVal;
    private FirebaseAuth mAuth;
    private String currentSavedAddress = "";
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_saved_addresses);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListeners();
        CustomerDashboardNavHelper.setupBottomNavigation(this);
        loadSavedAddress();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackSavedAddress);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvAddressVal = findViewById(R.id.tvSavedAddressFullVal);
    }

    private void setupClickListeners() {
        View btnEdit = findViewById(R.id.btnEditSavedAddress);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> startActivity(new Intent(this, EditAddressActivity.class)));
        }

        View btnAddMap = findViewById(R.id.btnAddAddressMap);
        if (btnAddMap != null) {
            btnAddMap.setOnClickListener(v -> startActivity(new Intent(this, MapPickerActivity.class)));
        }
    }

    private void showEditAddressDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_address, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etInput = dialogView.findViewById(R.id.etDialogAddressInput);
        if (etInput != null) {
            etInput.setText(currentSavedAddress);
        }

        View btnCancel = dialogView.findViewById(R.id.btnCancelDialogAddress);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnPickMap = dialogView.findViewById(R.id.btnDialogPickOnMap);
        if (btnPickMap != null) {
            btnPickMap.setOnClickListener(v -> {
                dialog.dismiss();
                startActivity(new Intent(this, MapPickerActivity.class));
            });
        }

        View btnSave = dialogView.findViewById(R.id.btnSaveDialogAddress);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String newAddr = etInput != null ? etInput.getText().toString().trim() : "";
                if (newAddr.isEmpty()) {
                    Toast.makeText(this, "Address cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveAddressToFirebase(newAddr);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void saveAddressToFirebase(String newAddress) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.child("address").setValue(newAddress)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CustomerSavedAddressesActivity.this, "Service address updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CustomerSavedAddressesActivity.this, "Failed to update address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSavedAddress() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    String addr = snapshot.child("address").getValue(String.class);
                    currentSavedAddress = addr != null ? addr : "";
                    if (tvAddressVal != null) {
                        tvAddressVal.setText(!currentSavedAddress.isEmpty() ? currentSavedAddress : "No service address saved yet.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerSavedAddressesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
