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
