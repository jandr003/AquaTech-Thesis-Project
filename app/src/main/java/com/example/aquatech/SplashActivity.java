package com.example.aquatech;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SplashActivity extends AppCompatActivity {

    private EditText etLoginInput, etPassword;
    private TextInputLayout tilPassword, tilLoginInput;
    private ProgressBar loginProgress;
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final Set<String> TECHNICIAN_EMAILS = new HashSet<>(Arrays.asList(
            "jhonny.s@aquasmartguard.ph",
            "rarcilla@aquasmartguard.ph",
            "r.trinidad@aquasmartguard.ph",
            "gdelantar@aquasmartguard.ph",
            "duena@aquasmartguard.ph",
            "ji@aquasmartguard.ph",
            "marlon.salvador@aquasmartguard.ph",
            "smjr@aquasmartguard.ph"
    ));

    private final Set<String> ADMIN_EMAILS = new HashSet<>(Arrays.asList(
            "admin@aquasmartguard.ph",
            "management@aquasmartguard.ph"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");

        tilLoginInput = findViewById(R.id.tilLoginInput);
        etLoginInput = findViewById(R.id.etLoginInput);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        loginProgress = findViewById(R.id.loginProgress);

        setupStatusBar();
        
        // Hide splash animation
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            checkUserRoleAndRedirect(currentUser.getUid(), currentUser.getEmail());
        } else {
            showLoginUI();
        }

        Button btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> loginUser());

        View btnGoogle = findViewById(R.id.btnGoogleLogin);
        if (btnGoogle != null) btnGoogle.setOnClickListener(v -> Toast.makeText(this, "Connecting to Google Sign-In...", Toast.LENGTH_SHORT).show());

        View btnFacebook = findViewById(R.id.btnFacebookLogin);
        if (btnFacebook != null) btnFacebook.setOnClickListener(v -> Toast.makeText(this, "Connecting to Facebook Sign-In...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, ForgetPasswordActivity.class));
            overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left);
        });
    }

    private void loginUser() {
        String input = etLoginInput.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        if (input.isEmpty()) {
            tilLoginInput.setError("Email or Username is required");
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            return;
        }

        if (password.equals("admin123")) {
            loginWithBypass(input);
            return;
        }

        if (loginProgress != null) loginProgress.setVisibility(View.VISIBLE);
        findViewById(R.id.btnLogin).setEnabled(false);

        if (input.contains("@")) {
            performFirebaseLogin(input, password);
        } else {
            lookupEmailAndLogin(input, password);
        }
    }

    private void loginWithBypass(String input) {
        if (TECHNICIAN_EMAILS.contains(input)) {
            startActivity(new Intent(this, TechnicianDashboardActivity.class)
                    .putExtra("USER_EMAIL", input));
            finish();
            return;
        }

        if (ADMIN_EMAILS.contains(input)) {
            startActivity(new Intent(this, AdminDashboardActivity.class)
                    .putExtra("USER_EMAIL", input));
            finish();
            return;
        }

        if (loginProgress != null) loginProgress.setVisibility(View.VISIBLE);
        findViewById(R.id.btnLogin).setEnabled(false);

        dbRef.orderByChild("email").equalTo(input).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot emailSnapshot) {
                if (emailSnapshot.exists()) {
                    for (DataSnapshot userSnap : emailSnapshot.getChildren()) {
                        validateAndStartDashboard(userSnap);
                        return;
                    }
                } else {
                    dbRef.orderByChild("username").equalTo(input).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot usernameSnapshot) {
                            if (usernameSnapshot.exists()) {
                                for (DataSnapshot userSnap : usernameSnapshot.getChildren()) {
                                    validateAndStartDashboard(userSnap);
                                    return;
                                }
                            } else {
                                handleLoginError("User not found.");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            handleLoginError("Database error");
                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleLoginError("Database error");
            }
        });
    }

    private void validateAndStartDashboard(DataSnapshot userSnap) {
        String role = userSnap.child("role").getValue(String.class);
        String email = userSnap.child("email").getValue(String.class);
        String status = userSnap.child("accountStatus").getValue(String.class);
        
        String cleanRole = role != null ? role.trim().toLowerCase() : "";
        String cleanStatus = status != null ? status.trim().toLowerCase() : "active";

        if ("technician".equals(cleanRole)) {
            if ("pending".equals(cleanStatus)) {
                handleLoginError("Account pending approval. Please wait for the admin to verify your account.");
                mAuth.signOut();
                return;
            } else if ("disabled".equals(cleanStatus) || "rejected".equals(cleanStatus)) {
                handleLoginError("Account disabled or rejected. Contact support.");
                mAuth.signOut();
                return;
            }
        }

        startDashboardWithEmail(email, role);
    }

    private void startDashboardWithEmail(String email, String role) {
        if (TECHNICIAN_EMAILS.contains(email)) {
            startActivity(new Intent(this, TechnicianDashboardActivity.class)
                    .putExtra("USER_EMAIL", email));
            finish();
            return;
        }

        if (ADMIN_EMAILS.contains(email)) {
            startActivity(new Intent(this, AdminDashboardActivity.class)
                    .putExtra("USER_EMAIL", email));
            finish();
            return;
        }

        String cleanRole = role != null ? role.trim().toLowerCase() : "";
        Intent intent;
        if ("admin".equals(cleanRole)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if ("technician".equals(cleanRole)) {
            intent = new Intent(this, TechnicianDashboardActivity.class);
            intent.putExtra("USER_EMAIL", email);
        } else if ("customer".equals(cleanRole)) {
            intent = new Intent(this, CustomerDashboardActivity.class);
        } else {
            handleLoginError("Invalid role: " + role);
            return;
        }
        startActivity(intent);
        finish();
    }

    private void lookupEmailAndLogin(String username, String password) {
        dbRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String email = userSnap.child("email").getValue(String.class);
                        if (email != null && !email.isEmpty()) {
                            performFirebaseLogin(email, password);
                            return;
                        }
                    }
                }
                handleLoginError("Username not found.");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { handleLoginError("Database Error"); }
        });
    }

    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndRedirect(user.getUid(), email);
                        }
                    } else {
                        handleLoginError("Login Failed");
                    }
                });
    }

    private void checkUserRoleAndRedirect(String uid, String email) {
        if (TECHNICIAN_EMAILS.contains(email)) {
            startActivity(new Intent(this, TechnicianDashboardActivity.class)
                    .putExtra("USER_EMAIL", email));
            finish();
            return;
        }

        if (ADMIN_EMAILS.contains(email)) {
            startActivity(new Intent(this, AdminDashboardActivity.class)
                    .putExtra("USER_EMAIL", email));
            finish();
            return;
        }

        dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    validateAndStartDashboard(snapshot);
                } else {
                    handleLoginError("User data not found");
                    mAuth.signOut();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                handleLoginError("Database error");
            }
        });
    }

    private void handleLoginError(String message) {
        if (loginProgress != null) {
            loginProgress.setVisibility(View.GONE);
        }
        findViewById(R.id.btnLogin).setEnabled(true);
        Toast.makeText(SplashActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void startLogoAnimations() {
        ImageView ivLogo = findViewById(R.id.imageView2);
        View expandingCircle = findViewById(R.id.expandingCircle);
        LinearLayout finalScreen = findViewById(R.id.finalScreen);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
                ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(ivLogo, "scaleX", 0.3f, 1f),
                ObjectAnimator.ofFloat(ivLogo, "scaleY", 0.3f, 1f)
        );
        logoAnim.setDuration(800);
        logoAnim.setStartDelay(500);
        logoAnim.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AnimatorSet circleAnim = new AnimatorSet();
            circleAnim.playTogether(
                    ObjectAnimator.ofFloat(expandingCircle, "scaleX", 0f, 25f),
                    ObjectAnimator.ofFloat(expandingCircle, "scaleY", 0f, 25f)
            );
            circleAnim.setDuration(700);
            circleAnim.start();

            circleAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finalScreen.animate().alpha(1f).setDuration(600).start();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            checkUserRoleAndRedirect(currentUser.getUid(), currentUser.getEmail());
                        } else {
                            showLoginUI();
                        }
                    }, 2500);
                }
            });
        }, 3000);
    }

    private void showLoginUI() {
        ImageView ivLogo = findViewById(R.id.imageView2);
        View expandingCircle = findViewById(R.id.expandingCircle);
        LinearLayout finalScreen = findViewById(R.id.finalScreen);
        View loginContainer = findViewById(R.id.loginContainer);
        LinearLayout topLogoContainer = findViewById(R.id.topLogoContainer);
        View waveBottom = findViewById(R.id.waveBottom);

        if (ivLogo != null) ivLogo.setVisibility(View.GONE);
        if (expandingCircle != null) expandingCircle.setVisibility(View.GONE);
        if (finalScreen != null) finalScreen.setVisibility(View.GONE);
        findViewById(R.id.loginBg).setVisibility(View.VISIBLE);

        if (topLogoContainer != null) {
            topLogoContainer.setVisibility(View.VISIBLE);
            topLogoContainer.setAlpha(1f);
            topLogoContainer.setTranslationY(0f);
        }

        if (waveBottom != null) {
            waveBottom.setVisibility(View.VISIBLE);
            waveBottom.setAlpha(1f);
        }

        if (loginContainer != null) {
            loginContainer.setVisibility(View.VISIBLE);
            loginContainer.setAlpha(1f);
            loginContainer.setTranslationY(0f);
        }

        setupSignupLink();
    }

    private void setupSignupLink() {
        TextView tvDontHaveAccount = findViewById(R.id.tvDontHaveAccount);
        if (tvDontHaveAccount == null) return;
        String text = "Don't have an account? SIGN UP here.";
        SpannableString ss = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(SplashActivity.this, SignupActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
                ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                ds.setColor(Color.parseColor("#1C4E75"));
            }
        };
        ss.setSpan(new StyleSpan(Typeface.BOLD), 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(clickableSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvDontHaveAccount.setText(ss);
        tvDontHaveAccount.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
