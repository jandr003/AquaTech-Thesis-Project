package com.example.aquatech;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class TechnicianVerificationActivity extends AppCompatActivity {

    private ImageView ivIdPreview, ivCertPreview, ivStatusIcon;
    private TextView tvVerificationStatus, tvStatusDescription;
    private View layoutIdPlaceholder, layoutCertPlaceholder;
    private MaterialButton btnSubmit;
    private LinearLayout uploadSection;

    private Uri idUri, certUri;
    private String idDownloadUrl, certDownloadUrl;

    private FirebaseAuth mAuth;
    private DatabaseReference techRef, userRef;
    private StorageReference storageRef;
    private String currentUid;
    private String currentTechName;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final ActivityResultLauncher<String> idPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    idUri = uri;
                    layoutIdPlaceholder.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivIdPreview);
                }
            }
    );

    private final ActivityResultLauncher<String> certPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    certUri = uri;
                    layoutCertPlaceholder.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivCertPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_verification);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUid = currentUser.getUid();

        techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(currentUid);
        userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(currentUid);
        storageRef = FirebaseStorage.getInstance().getReference().child("verifications").child(currentUid);

        setupStatusBar();
        initializeViews();
        fetchTechName();
        checkCurrentVerificationStatus();
        setupClickListeners();
    }

    private void fetchTechName() {
        techRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTechName = snapshot.getValue(String.class);
                if (currentTechName == null) {
                    userRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            currentTechName = snapshot.getValue(String.class);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkCurrentVerificationStatus() {
        techRef.child("verification").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.child("status").getValue(String.class);
                idDownloadUrl = snapshot.child("idUrl").getValue(String.class);
                certDownloadUrl = snapshot.child("certUrl").getValue(String.class);
                updateStatusUI(status, idDownloadUrl, certDownloadUrl);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateStatusUI(String status, String idUrl, String certUrl) {
        if ("Verified".equalsIgnoreCase(status)) {
            tvVerificationStatus.setText("VERIFIED");
            tvVerificationStatus.setTextColor(Color.parseColor("#4CAF50"));
            tvStatusDescription.setText("Identity confirmed. You can now accept all job requests.");
            ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            ivStatusIcon.setColorFilter(Color.parseColor("#4CAF50"));
            btnSubmit.setVisibility(View.GONE);
            uploadSection.setVisibility(View.GONE);
            showUploadedImages(idUrl, certUrl);
        } else if ("Reviewing".equalsIgnoreCase(status)) {
            tvVerificationStatus.setText("PENDING REVIEW");
            tvVerificationStatus.setTextColor(Color.parseColor("#FFA000"));
            tvStatusDescription.setText("We are checking your documents. Verification in progress...");
            btnSubmit.setEnabled(false);
            btnSubmit.setText("UNDER REVIEW");
            uploadSection.setVisibility(View.GONE);
            showUploadedImages(idUrl, certUrl);
        } else if ("Rejected".equalsIgnoreCase(status)) {
            tvVerificationStatus.setText("REJECTED");
            tvVerificationStatus.setTextColor(Color.parseColor("#FF4B4B"));
            tvStatusDescription.setText("Your documents were rejected. Please upload valid requirements.");
            btnSubmit.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(true);
            btnSubmit.setText("RESUBMIT FOR REVIEW");
            uploadSection.setVisibility(View.VISIBLE);
            showUploadedImages(idUrl, certUrl);
        } else {
            tvVerificationStatus.setText("NOT VERIFIED");
            tvVerificationStatus.setTextColor(Color.parseColor("#FF4B4B"));
            tvStatusDescription.setText("Upload requirements to start accepting jobs.");
            btnSubmit.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(true);
            btnSubmit.setText("SUBMIT FOR REVIEW");
            uploadSection.setVisibility(View.VISIBLE);
        }
    }

    private void showUploadedImages(String idUrl, String certUrl) {
        if (idUrl != null && !idUrl.isEmpty()) {
            layoutIdPlaceholder.setVisibility(View.GONE);
            Glide.with(this).load(idUrl).centerCrop().into(ivIdPreview);
        }
        if (certUrl != null && !certUrl.isEmpty()) {
            layoutCertPlaceholder.setVisibility(View.GONE);
            Glide.with(this).load(certUrl).centerCrop().into(ivCertPreview);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ivIdPreview = findViewById(R.id.ivIdPreview);
        ivCertPreview = findViewById(R.id.ivCertPreview);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvVerificationStatus = findViewById(R.id.tvVerificationStatus);
        tvStatusDescription = findViewById(R.id.tvStatusDescription);
        layoutIdPlaceholder = findViewById(R.id.layoutIdPlaceholder);
        layoutCertPlaceholder = findViewById(R.id.layoutCertPlaceholder);
        btnSubmit = findViewById(R.id.btnSubmitVerification);
        uploadSection = findViewById(R.id.uploadSection);
    }

    private void setupClickListeners() {
        findViewById(R.id.cardUploadId).setOnClickListener(v -> idPickerLauncher.launch("image/*"));
        findViewById(R.id.cardUploadCert).setOnClickListener(v -> certPickerLauncher.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitVerification());
    }

    private void submitVerification() {
        if (idUri == null || certUri == null) {
            Toast.makeText(this, "Please upload both ID and Certificate", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading documents...");
        pd.setCancelable(false);
        pd.show();

        String timestamp = String.valueOf(System.currentTimeMillis());
        StorageReference idRef = storageRef.child("id_" + timestamp + ".jpg");
        StorageReference certRef = storageRef.child("cert_" + timestamp + ".jpg");

        // Robust Chaining using Task.continueWithTask
        idRef.putFile(idUri).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return idRef.getDownloadUrl();
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            idDownloadUrl = task.getResult().toString();
            pd.setMessage("Uploading Certificate...");
            return certRef.putFile(certUri);
        }).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return certRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            certDownloadUrl = uri.toString();
            saveVerificationData(pd);
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Log.e("VERIFICATION", "Upload failed", e);
            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void saveVerificationData(ProgressDialog pd) {
        pd.setMessage("Finalizing...");
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("status", "Reviewing");
        verificationData.put("idUrl", idDownloadUrl);
        verificationData.put("certUrl", certDownloadUrl);
        verificationData.put("submittedAt", System.currentTimeMillis());

        // Update BOTH nodes to ensure Admin sees it in Manage Technicians
        Map<String, Object> updates = new HashMap<>();
        updates.put("/Technicians/" + currentUid + "/verification", verificationData);
        updates.put("/Users/" + currentUid + "/verification", verificationData);

        FirebaseDatabase.getInstance(DB_URL).getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(TechnicianVerificationActivity.this, "Documents submitted! Admin will review your account.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(TechnicianVerificationActivity.this, "Database update failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
