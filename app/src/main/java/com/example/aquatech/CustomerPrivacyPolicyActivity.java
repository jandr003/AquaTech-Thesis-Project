package com.example.aquatech;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class CustomerPrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_privacy_policy);

        initializeViews();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackPrivacyPolicy);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAccept = findViewById(R.id.btnAcceptPrivacy);
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> finish());
        }
    }
}
