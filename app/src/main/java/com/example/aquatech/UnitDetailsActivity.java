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
    private String selectedModel;
    private int unitImageRes = R.drawable.titled_design1;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_details);

        setupStatusBar();
        initializeViews();

        Intent intent = getIntent();
        if (intent != null) {
            selectedName = intent.getStringExtra("UNIT_NAME");
            selectedModel = intent.getStringExtra("UNIT_MODEL");
            unitImageRes = intent.getIntExtra("UNIT_IMAGE_RES", R.drawable.titled_design1);
        }

        setupUnitDetails(selectedName, selectedModel);

        String[] types = {"Select Type", "SUBSCRIPTION", "OUTRIGHT"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        if (spinnerPurchaseType != null) {
            spinnerPurchaseType.setAdapter(adapter);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                String unitNo = etUnitNumber != null ? etUnitNumber.getText().toString().trim() : "";
                String sroNo = etReferenceNumber != null ? etReferenceNumber.getText().toString().trim() : "";
                String purchaseType = (spinnerPurchaseType != null && spinnerPurchaseType.getSelectedItem() != null) ?
                        spinnerPurchaseType.getSelectedItem().toString() : "Select Type";

                if (sroNo.isEmpty()) {
                    if (etReferenceNumber != null) {
                        etReferenceNumber.setError("Reference Number (SRO) is required");
                    }
                } else if (unitNo.isEmpty()) {
                    if (etUnitNumber != null) {
                        etUnitNumber.setError("Please enter unit number");
                    }
                } else if (purchaseType.equals("Select Type")) {
                    Toast.makeText(this, "Please select purchase type", Toast.LENGTH_SHORT).show();
                } else {
                    saveUnitToFirebase(selectedName, unitNo, sroNo, purchaseType);
                }
            });
        }
    }

    private void saveUnitToFirebase(String name, String unitNo, String sroNo, String purchaseType) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }

        String serialTemplate = tvUnitModel != null ? tvUnitModel.getText().toString() : "";
        String finalSerial = serialTemplate.replace("**-***", unitNo);
        String formattedSro = sroNo != null ? sroNo.toUpperCase() : "";

        if (!formattedSro.startsWith("SRO-") && !formattedSro.startsWith("SR0-") && !formattedSro.startsWith("ASC-")) {
            formattedSro = "SRO-" + formattedSro;
        }

        Map<String, Object> unitData = new HashMap<>();
        unitData.put("unitName", name != null ? name : "Water Purifier");
        unitData.put("serialNo", finalSerial);
        unitData.put("referenceNo", formattedSro);
        unitData.put("purchaseType", purchaseType);
        unitData.put("unitImageRes", unitImageRes);

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
        userRef.child("units").child(formattedSro).setValue(unitData);

        userRef.updateChildren(unitData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Account Setup Complete!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            } else {
                Toast.makeText(this, "Failed to save unit details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, CustomerDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupUnitDetails(String name, String model) {
        String displayName = (name != null && !name.isEmpty()) ? name : "Water Purifier";

        if (tvUnitName != null) {
            tvUnitName.setText(displayName);
        }

        String modelTemplate;
        if (model != null && !model.isEmpty()) {
            modelTemplate = model;
        } else {
            modelTemplate = deriveModelTemplate(displayName);
        }

        if (tvUnitModel != null) {
            tvUnitModel.setText(modelTemplate);
        }

        if (imgUnit != null) {
            imgUnit.setImageResource(unitImageRes);
        }
    }

    private String deriveModelTemplate(String name) {
        if (name == null) {
            return "ST-FXCU1-M-HCA-WT-**-***";
        }

        String upper = name.toUpperCase();
        if (upper.contains("CUBE")) {
            unitImageRes = R.drawable.titled_design1;
            return "F-FXCU1-M-HCA-TT-K1-**-***";
        } else if (upper.contains("SLIM")) {
            unitImageRes = R.drawable.titled_design2;
            return "S-FXCU1-M-HCA-AA-B2-**-***";
        } else if (upper.contains("COUNTER")) {
            unitImageRes = R.drawable.titled_design3;
            return "CT-FXCU1-M-HCA-WT-**-***";
        } else if (upper.contains("STANDING")) {
            unitImageRes = R.drawable.titled_design4;
            return "ST-FXCU1-M-HCA-WT-**-***";
        }

        String prefix = generatePrefixFromName(name);
        return prefix + "-FXCU1-M-HCA-WT-**-***";
    }

    private String generatePrefixFromName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "GEN";
        }

        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(w.charAt(0));
            }
        }

        String prefix = sb.toString().toUpperCase();
        return prefix.length() > 0 ? prefix : "GEN";
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
