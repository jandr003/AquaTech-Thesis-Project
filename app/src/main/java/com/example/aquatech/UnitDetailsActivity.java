package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class UnitDetailsActivity extends AppCompatActivity {

    private ImageView imgUnit, btnBack;
    private TextView tvUnitName, tvUnitModel;
    private EditText etUnitNumber, etReferenceNumber;
    private Spinner spinnerPurchaseType;
    private Button btnConfirm;

    private String selectedName;
    private int unitImageRes = R.drawable.titled_design1;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_details);

        setupStatusBar();
        initializeViews();

        selectedName = getIntent().getStringExtra("UNIT_NAME");
        setupUnitDetails(selectedName);

        String[] types = {"Select Type", "SUBSCRIPTION", "OUTRIGHT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPurchaseType.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            String unitNo = etUnitNumber.getText().toString().trim();
            String sroNo = etReferenceNumber.getText().toString().trim();
            String purchaseType = spinnerPurchaseType.getSelectedItem().toString();

            if (sroNo.isEmpty()) {
                etReferenceNumber.setError("Reference Number (SRO) is required");
            } else if (unitNo.isEmpty()) {
                etUnitNumber.setError("Please enter unit number");
            } else if (purchaseType.equals("Select Type")) {
                Toast.makeText(this, "Please select purchase type", Toast.LENGTH_SHORT).show();
            } else {
                saveUnitToFirebase(selectedName, unitNo, sroNo, purchaseType);
            }
        });
    }

    private void saveUnitToFirebase(String name, String unitNo, String sroNo, String purchaseType) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String serialTemplate = tvUnitModel.getText().toString();
        String finalSerial = serialTemplate.replace("**-***", unitNo);
        String formattedSro = sroNo.toUpperCase();

        if (formattedSro.contains("ASC")) formattedSro = formattedSro.replace("ASC", "SRO");
        if (!formattedSro.startsWith("SRO-")) formattedSro = "SRO-" + formattedSro;

        Map<String, Object> unitData = new HashMap<>();
        unitData.put("unitName", name);
        unitData.put("serialNo", finalSerial);
        unitData.put("referenceNo", formattedSro);
        unitData.put("purchaseType", purchaseType);
        unitData.put("unitImageRes", unitImageRes);

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
        
        // 1. I-save sa 'units' list
        userRef.child("units").child(formattedSro).setValue(unitData);

        // 2. I-set bilang 'Active Unit' at pumunta sa DASHBOARD
        userRef.updateChildren(unitData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Account Setup Complete!", Toast.LENGTH_SHORT).show();
                navigateToDashboard(); // 📍 FIXED: Diretso sa Dashboard
            } else {
                Toast.makeText(this, "Failed to save unit details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, CustomerDashboardActivity.class);
        // Clear stack para hindi na makabalik sa setup screens
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupUnitDetails(String name) {
        if (name == null) return;
        tvUnitName.setText(name);
        switch (name) {
            case "WL CUBE FIREWALL": unitImageRes = R.drawable.titled_design1; tvUnitModel.setText("F-FXCU1-M-HCA-TT-K1-**-***"); break;
            case "SMART SLIM": unitImageRes = R.drawable.titled_design2; tvUnitModel.setText("S-FXCU1-M-HCA-AA-B2-**-***"); break;
            case "COUNTER TOP WATER PURIFIER": unitImageRes = R.drawable.titled_design3; tvUnitModel.setText("CT-FXCU1-M-HCA-WT-**-***"); break;
            case "STANDING WATER PURIFIER": unitImageRes = R.drawable.titled_design4; tvUnitModel.setText("ST-FXCU1-M-HCA-WT-**-***"); break;
        }
        imgUnit.setImageResource(unitImageRes);
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.lessThanIcon);
        imgUnit = findViewById(R.id.ItemOrder01);
        tvUnitName = findViewById(R.id.itemWlCubeFirewall01);
        tvUnitModel = findViewById(R.id.ItemSubName01);
        etUnitNumber = findViewById(R.id.etUnitNumber);
        etReferenceNumber = findViewById(R.id.etReferenceNumber);
        spinnerPurchaseType = findViewById(R.id.purchaseTypeDropdown);
        btnConfirm = findViewById(R.id.btnConfirm);
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
