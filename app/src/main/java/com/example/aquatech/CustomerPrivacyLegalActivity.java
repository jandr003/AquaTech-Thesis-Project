package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CustomerPrivacyLegalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_privacy_legal);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackCustPrivacy);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupClickListeners() {
        View btnPolicy = findViewById(R.id.btnCustPrivacyPolicy);
        if (btnPolicy != null) {
            btnPolicy.setOnClickListener(v -> startActivity(new Intent(this, CustomerPrivacyPolicyActivity.class)));
        }

        View btnTerms = findViewById(R.id.btnCustTermsOfService);
        if (btnTerms != null) {
            btnTerms.setOnClickListener(v -> startActivity(new Intent(this, TermsAndConditionActivity.class)));
        }
    }
}
