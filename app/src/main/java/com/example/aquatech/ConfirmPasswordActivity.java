package com.example.aquatech;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConfirmPasswordActivity extends AppCompatActivity {

    private EditText yourNewPass, reNewPass;
    private TextView yourNewPassWarning, reNewPassWarning;
    private Button btnDonePassword;
    private ImageView indicatorEight, indicatorLower, indicatorUpper, indicatorNumber, indicatorSpecial;

    private FirebaseAuth mAuth;
    private String userId; // 🔑 Dito mase-save yung UID galing sa VerificationCodeActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_password);

        mAuth = FirebaseAuth.getInstance();
        
        // 🔑 Kunin ang USER_ID na ipinasa ni VerificationCodeActivity
        userId = getIntent().getStringExtra("USER_ID");

        setupStatusBar();
        initializeViews();
        setupFilters();
        setupRealTimeValidation();
        
        setupPasswordToggle(yourNewPass);
        setupPasswordToggle(reNewPass);

        btnDonePassword.setOnClickListener(v -> handlePasswordReset());
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

    private void initializeViews() {
        yourNewPass = findViewById(R.id.yourNewPass);
        reNewPass   = findViewById(R.id.reNewPass);
        yourNewPassWarning = findViewById(R.id.yourNewPassWarning);
        reNewPassWarning   = findViewById(R.id.reNewPassWarning);
        btnDonePassword = findViewById(R.id.btnDonePassword);

        indicatorEight   = findViewById(R.id.IndicatorEight);
        indicatorLower   = findViewById(R.id.IndicatorLower);
        indicatorUpper   = findViewById(R.id.IndicatorUpper);
        indicatorNumber  = findViewById(R.id.IndicatorNumber);
        indicatorSpecial = findViewById(R.id.IndicatorSpecial);
    }

    private void setupFilters() {
        InputFilter[] filters = new InputFilter[]{ new InputFilter.LengthFilter(15) };
        yourNewPass.setFilters(filters);
        reNewPass.setFilters(filters);
    }

    private void setupRealTimeValidation() {
        yourNewPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateIndicators(s.toString());
            }
        });
    }

    private void updateIndicators(String password) {
        indicatorEight.setImageResource(password.length() >= 8 ? R.drawable.ic_check_on : R.drawable.circle_indicator);
        indicatorLower.setImageResource(password.matches(".*[a-z].*") ? R.drawable.ic_check_on : R.drawable.circle_indicator);
        indicatorUpper.setImageResource(password.matches(".*[A-Z].*") ? R.drawable.ic_check_on : R.drawable.circle_indicator);
        indicatorNumber.setImageResource(password.matches(".*\\d.*") ? R.drawable.ic_check_on : R.drawable.circle_indicator);
        indicatorSpecial.setImageResource(password.matches(".*[!@#$%^&*()_+\\-=\\{\\}\\[\\]|\\\\:;\"'<>,.?/].*") ? R.drawable.ic_check_on : R.drawable.circle_indicator);
    }

    private void handlePasswordReset() {
        String pass1 = yourNewPass.getText().toString().trim();
        String pass2 = reNewPass.getText().toString().trim();

        yourNewPassWarning.setVisibility(View.GONE);
        reNewPassWarning.setVisibility(View.GONE);

        if (pass1.length() < 8) {
            yourNewPassWarning.setText("Password should be 8–15 characters");
            yourNewPassWarning.setVisibility(View.VISIBLE);
            return;
        }

        if (!pass1.equals(pass2)) {
            reNewPassWarning.setVisibility(View.VISIBLE);
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Session error. Please restart the forgot password process.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Saving new password...");
        pd.setCancelable(false);
        pd.show();

        // 🔑 FIREBASE UPDATE: I-update ang password sa 'Users' node sa Realtime Database
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        userRef.child("password").setValue(pass1).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Kung may logged in user (halimbawa galing Phone Auth), i-update din ang Auth password
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.updatePassword(pass1);
                }
                
                pd.dismiss();
                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_LONG).show();
                
                // Balik sa Login/Splash screen
                Intent intent = new Intent(this, SplashActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                pd.dismiss();
                Toast.makeText(this, "Failed to update: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPasswordToggle(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable[] drawables = editText.getCompoundDrawables();
                if (drawables[DRAWABLE_END] != null) {
                    if (event.getX() >= (editText.getWidth() - editText.getPaddingRight() - drawables[DRAWABLE_END].getBounds().width())) {
                        int selection = editText.getSelectionEnd();
                        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        } else {
                            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        }
                        editText.setSelection(selection);
                        return true;
                    }
                }
            }
            return false;
        });
    }
}
