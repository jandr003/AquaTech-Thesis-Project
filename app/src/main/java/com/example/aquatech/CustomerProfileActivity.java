package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomerProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvName, tvPhone, tvMemberSince, btnVerifiedBadge;
    private View btnViewBenefits;
    private FirebaseAuth mAuth;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupClickListeners();
        CustomerDashboardNavHelper.setupBottomNavigation(this);
        loadCustomerData();
    }

    private void initializeViews() {
        ivAvatar = findViewById(R.id.ivCustProfileAvatar);
        tvName = findViewById(R.id.tvCustProfileName);
        tvPhone = findViewById(R.id.tvCustProfilePhone);
        tvMemberSince = findViewById(R.id.tvCustMemberSince);
        btnViewBenefits = findViewById(R.id.btnViewBenefits);
        btnVerifiedBadge = findViewById(R.id.btnVerifiedBadge);
    }

    private void setupClickListeners() {
        if (btnViewBenefits != null) {
            btnViewBenefits.setOnClickListener(v -> startActivity(new Intent(this, CustomerVerificationDetailsActivity.class)));
        }

        View headerCard = findViewById(R.id.cardCustProfileHeader);
        if (headerCard != null) {
            headerCard.setOnClickListener(v -> startActivity(new Intent(this, CustomerVerificationDetailsActivity.class)));
        }

        View menuEdit = findViewById(R.id.menuEditProfileGCash);
        if (menuEdit != null) {
            menuEdit.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        }

        View menuAddress = findViewById(R.id.menuServiceAddressCust);
        if (menuAddress != null) {
            menuAddress.setOnClickListener(v -> startActivity(new Intent(this, CustomerSavedAddressesActivity.class)));
        }

        View menuNotif = findViewById(R.id.menuNotificationPreferencesCust);
        if (menuNotif != null) {
            menuNotif.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        View menuSecurity = findViewById(R.id.menuAccountSecurityCust);
        if (menuSecurity != null) {
            menuSecurity.setOnClickListener(v -> startActivity(new Intent(this, ConfirmPasswordActivity.class)));
        }

        View menuSupport = findViewById(R.id.menuSupportHelp);
        if (menuSupport != null) {
            menuSupport.setOnClickListener(v -> startActivity(new Intent(this, TechnicianHelpCenterActivity.class)));
        }

        View menuAbout = findViewById(R.id.menuAboutAquaTechCust);
        if (menuAbout != null) {
            menuAbout.setOnClickListener(v -> startActivity(new Intent(this, TechnicianAboutActivity.class)));
        }

        View menuPrivacy = findViewById(R.id.menuPrivacyLegalCust);
        if (menuPrivacy != null) {
            menuPrivacy.setOnClickListener(v -> startActivity(new Intent(this, TechnicianPrivacyActivity.class)));
        }

        View btnLogout = findViewById(R.id.btnCustLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }
    }

    private void loadCustomerData() {
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
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null) {
                        name = snapshot.child("name").getValue(String.class);
                    }
                    if (tvName != null) {
                        tvName.setText(name != null && !name.isEmpty() ? name : "Customer");
                    }

                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null) {
                        phone = snapshot.child("customerPhone").getValue(String.class);
                    }
                    if (tvPhone != null) {
                        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Registered Account");
                    }

                    Long creationTs = snapshot.child("createdAt").getValue(Long.class);
                    if (creationTs == null) {
                        creationTs = snapshot.child("timestamp").getValue(Long.class);
                    }
                    if (creationTs == null && user.getMetadata() != null) {
                        creationTs = user.getMetadata().getCreationTimestamp();
                    }

                    if (tvMemberSince != null) {
                        if (creationTs != null && creationTs > 0) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
                            tvMemberSince.setText("Member Since: " + sdf.format(new Date(creationTs)));
                        } else {
                            tvMemberSince.setText("Member Since: Active");
                        }
                    }

                    Boolean isVerified = snapshot.child("govIdVerified").getValue(Boolean.class);
                    if (isVerified == null) {
                        isVerified = snapshot.child("isVerified").getValue(Boolean.class);
                    }

                    String level = snapshot.child("verificationLevel").getValue(String.class);
                    if (level == null || level.isEmpty()) {
                        level = (isVerified != null && isVerified) ? "Fully Verified" : "Basic Level";
                    }

                    if (btnVerifiedBadge != null) {
                        btnVerifiedBadge.setText(level);
                    }

                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    if (ivAvatar != null) {
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            Glide.with(CustomerProfileActivity.this)
                                    .load(imgUrl)
                                    .placeholder(R.drawable.customer_avatar1)
                                    .circleCrop()
                                    .into(ivAvatar);
                        } else {
                            ivAvatar.setImageResource(R.drawable.customer_avatar1);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutUser() {
        if (mAuth != null) {
            mAuth.signOut();
        }
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
