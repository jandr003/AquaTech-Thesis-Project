package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TechnicianProfileActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private ImageView ivProfilePic;
    private TextView tvTechName, tvTechRole, tvTechVerifiedBadge, tvTechUID, tvTechEmail, tvTechPhone, tvTechPosition, tvTechBranch, tvTechFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        }

        setupStatusBar();
        initializeViews();
        loadTechnicianProfileData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackTechProfile);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ivProfilePic = findViewById(R.id.ivTechProfilePic);
        tvTechName = findViewById(R.id.tvTechName);
        tvTechRole = findViewById(R.id.tvTechRole);
        tvTechVerifiedBadge = findViewById(R.id.tvTechVerifiedBadge);
        tvTechUID = findViewById(R.id.tvTechUID);
        tvTechEmail = findViewById(R.id.tvTechEmail);
        tvTechPhone = findViewById(R.id.tvTechPhone);
        tvTechPosition = findViewById(R.id.tvTechPosition);
        tvTechBranch = findViewById(R.id.tvTechBranch);

        tvTechFullName = findViewById(R.id.tvTechFullName);

        View btnChangePic = findViewById(R.id.btnChangeTechPic);
        if (btnChangePic != null) {
            btnChangePic.setOnClickListener(v -> showAvatarSelectionDialog());
        }

        if (tvTechVerifiedBadge != null) {
            tvTechVerifiedBadge.setOnClickListener(v -> startActivity(new Intent(this, TechnicianVerificationActivity.class)));
        }

        View btnUploadGovId = findViewById(R.id.btnUploadGovId);
        if (btnUploadGovId != null) {
            btnUploadGovId.setOnClickListener(v -> startActivity(new Intent(this, TechnicianVerificationActivity.class)));
        }

        View btnEditPersonal = findViewById(R.id.btnEditPersonalProfile);
        if (btnEditPersonal != null) {
            btnEditPersonal.setOnClickListener(v -> showEditPersonalProfileDialog());
        }

        View btnBranchDropdown = findViewById(R.id.btnTechBranchDropdown);
        View panelBranchDetails = findViewById(R.id.panelBranchDetails);
        ImageView ivBranchArrow = findViewById(R.id.ivBranchDropdownArrow);

        if (btnBranchDropdown != null && panelBranchDetails != null && ivBranchArrow != null) {
            btnBranchDropdown.setOnClickListener(v -> {
                if (panelBranchDetails.getVisibility() == View.GONE) {
                    panelBranchDetails.setVisibility(View.VISIBLE);
                    ivBranchArrow.setRotation(270f);
                } else {
                    panelBranchDetails.setVisibility(View.GONE);
                    ivBranchArrow.setRotation(90f);
                }
            });
        }

        View btnAccountSec = findViewById(R.id.btnProfileAccountSecurity);
        if (btnAccountSec != null) {
            btnAccountSec.setOnClickListener(v -> startActivity(new Intent(this, TechnicianAccountSecurityActivity.class)));
        }

        View btnHelpSupport = findViewById(R.id.btnProfileHelpSupport);
        if (btnHelpSupport != null) {
            btnHelpSupport.setOnClickListener(v -> startActivity(new Intent(this, TechnicianHelpCenterActivity.class)));
        }

        View btnLogout = findViewById(R.id.btnTechProfileLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        BottomNavigationView techBottomNav = findViewById(R.id.techBottomNav);
        if (techBottomNav != null) {
            techBottomNav.setSelectedItemId(R.id.nav_tech_profile);
            CurvedBottomNavHelper.setupAnimatedNav(techBottomNav);

            techBottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                CurvedBottomNavHelper.animateItemClick(techBottomNav, id);

                if (id == R.id.nav_tech_dashboard) {
                    Intent intent = new Intent(this, TechnicianDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_tech_performance) {
                    Intent intent = new Intent(this, WorkPerformanceActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_tech_messages) {
                    Intent intent = new Intent(this, CustomerChatListActivity.class);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_tech_profile) {
                    return true;
                }
                return true;
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of your AquaTech Technician account?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    if (mAuth != null) {
                        mAuth.signOut();
                    }
                    SharedPreferences prefs = getSharedPreferences("AquaTechPrefs", MODE_PRIVATE);
                    if (prefs != null) {
                        prefs.edit().clear().apply();
                    }
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, SplashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadTechnicianProfileData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (tvTechEmail != null) {
                tvTechEmail.setText(user.getEmail());
            }

            if (tvTechUID != null) {
                String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";
                tvTechUID.setText(getTechIdFromEmail(email));
            }

            TechnicianIdentityHelper.attachLiveAvatarListener(DB_URL, user.getUid(), ivProfilePic);

            if (userRef != null) {
                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null || name.isEmpty()) name = snapshot.child("name").getValue(String.class);
                        if (tvTechName != null && name != null && !name.isEmpty()) {
                            tvTechName.setText(name);
                        }
                        if (tvTechFullName != null && name != null && !name.isEmpty()) {
                            tvTechFullName.setText(name);
                        }

                        String mobile = snapshot.child("mobileNumber").getValue(String.class);
                        if (mobile == null || mobile.isEmpty()) mobile = snapshot.child("phone").getValue(String.class);
                        if (tvTechPhone != null && mobile != null && !mobile.isEmpty()) {
                            tvTechPhone.setText(mobile);
                        }

                        String status = snapshot.child("verificationStatus").getValue(String.class);
                        if (tvTechVerifiedBadge != null) {
                            if ("Verified".equalsIgnoreCase(status)) {
                                tvTechVerifiedBadge.setText("Account Verified");
                                tvTechVerifiedBadge.setTextColor(Color.parseColor("#166534"));
                            } else {
                                tvTechVerifiedBadge.setText("Pending Verification");
                                tvTechVerifiedBadge.setTextColor(Color.parseColor("#D97706"));
                            }
                        }

                        String position = snapshot.child("position").getValue(String.class);
                        if (tvTechPosition != null && position != null && !position.isEmpty()) {
                            tvTechPosition.setText(position);
                        }

                        String branch = snapshot.child("branch").getValue(String.class);
                        if (tvTechBranch != null && branch != null && !branch.isEmpty()) {
                            tvTechBranch.setText(branch);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }
    }

    private void showEditPersonalProfileDialog() {
        startActivity(new Intent(this, TechnicianEditProfileActivity.class));
    }

    private void showAvatarSelectionDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_select_avatar);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View btnCancel = dialog.findViewById(R.id.btnCancelAvatar);
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String getTechIdFromEmail(String email) {
        if (email.contains("john@")) return "TECH-0001";
        if (email.contains("jhonny@")) return "TECH-0002";
        if (email.contains("ricky@")) return "TECH-0003";
        if (email.contains("ruel@")) return "TECH-0004";
        if (email.contains("glenda@")) return "TECH-0005";
        if (email.contains("jerry@")) return "TECH-0006";
        if (email.contains("jonnifer@")) return "TECH-0007";
        if (email.contains("marlon@")) return "TECH-0008";
        if (email.contains("selvino@")) return "TECH-0009";
        return "TECH-0001";
    }
}
