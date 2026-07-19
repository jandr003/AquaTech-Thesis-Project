package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountDeletionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_deletion);

        setupStatusBar();

        // Back Button
        findViewById(R.id.btnBackDelete).setOnClickListener(v -> finish());

        // Confirm Deletion Button
        MaterialButton btnConfirm = findViewById(R.id.btnConfirmDelete);
        btnConfirm.setOnClickListener(v -> {
            // Dito natin ilalagay ang actual deletion logic sa future
            // Sa ngayon, magpapakita muna tayo ng Toast at babalik sa Splash
            deleteAccount();
        });
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Paalala: Sa production, kailangan mag-reauthenticate ng user bago mag-delete
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, SplashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to delete account. Please re-login and try again.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            finish();
        }
    }
}
