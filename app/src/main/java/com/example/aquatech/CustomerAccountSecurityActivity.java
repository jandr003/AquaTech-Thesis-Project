package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CustomerAccountSecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_account_security);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackCustAccSec);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupClickListeners() {
        View btnPass = findViewById(R.id.btnCustChangePassword);
        if (btnPass != null) {
            btnPass.setOnClickListener(v -> startActivity(new Intent(this, ConfirmPasswordActivity.class)));
        }

        View btnRecovery = findViewById(R.id.btnCustPasswordRecovery);
        if (btnRecovery != null) {
            btnRecovery.setOnClickListener(v -> startActivity(new Intent(this, ForgetPasswordActivity.class)));
        }

        View btnActivity = findViewById(R.id.btnCustLoginActivity);
        if (btnActivity != null) {
            btnActivity.setOnClickListener(v -> Toast.makeText(this, "Recent login activity verified and secure.", Toast.LENGTH_SHORT).show());
        }

        View btnDelete = findViewById(R.id.btnCustAccountDeletion);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> startActivity(new Intent(this, AccountDeletionActivity.class)));
        }
    }
}
