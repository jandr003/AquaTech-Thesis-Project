package com.example.aquatech;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResubmitReportActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private EditText etRemarks;
    private Uri proofUri;
    private String ticketId;
    private String currentTechUid = "";
    private String currentTechName = "";

    private DatabaseReference mDatabase;
    private StorageReference storageRef;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    proofUri = uri;
                    displayPreview();
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success) displayPreview();
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) launchCamera();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resubmit_report);

        setupStatusBar();

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null || ticketId.isEmpty()) {
            Toast.makeText(this, "Error: Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentTechUid = user.getUid();
            currentTechName = TechnicianIdentityHelper.resolveTechNameFromEmail(user.getEmail());
        }

        initializeViews();
    }

    private void initializeViews() {
        ivPreview = findViewById(R.id.ivResubmitPreview);
        etRemarks = findViewById(R.id.etResubmitRemarks);

        findViewById(R.id.btnBackResubmit).setOnClickListener(v -> finish());
        findViewById(R.id.btnResubmitCamera).setOnClickListener(v -> checkCameraPermission());
        findViewById(R.id.btnResubmitGallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
        findViewById(R.id.btnSubmitResubmission).setOnClickListener(v -> performResubmit());
    }

    private void displayPreview() {
        if (ivPreview != null && proofUri != null) {
            ivPreview.setImageTintList(null);
            ivPreview.setPadding(0, 0, 0, 0);
            Glide.with(this)
                    .load(proofUri)
                    .signature(new ObjectKey(System.currentTimeMillis()))
                    .centerCrop()
                    .into(ivPreview);
        }
    }

    private void launchCamera() {
        try {
            File tempFile = File.createTempFile("RESUBMIT_PROOF_" + System.currentTimeMillis(), ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            proofUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", tempFile);
            cameraLauncher.launch(proofUri);
        } catch (IOException ignored) {
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void performResubmit() {
        if (proofUri == null) {
            Toast.makeText(this, "Please attach a new proof photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        String remarks = etRemarks.getText().toString().trim();
        if (remarks.isEmpty()) {
            Toast.makeText(this, "Please enter service remarks", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Resubmitting report...");
        pd.setCancelable(false);
        pd.show();

        String fileName = "resubmit_" + System.currentTimeMillis() + ".jpg";
        StorageReference fileRef = storageRef.child("Reports/" + fileName);

        fileRef.putFile(proofUri)
                .addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                    saveResubmittedReport(uri.toString(), remarks, pd);
                }))
                .addOnFailureListener(e -> {
                    try { pd.dismiss(); } catch (Exception ignored) {}
                    Toast.makeText(ResubmitReportActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveResubmittedReport(String downloadUrl, String remarks, ProgressDialog pd) {
        DatabaseReference dbReportRef = mDatabase.child("Reports").push();
        Map<String, Object> data = new HashMap<>();
        data.put("techUid", currentTechUid);
        data.put("techName", currentTechName);
        data.put("remarks", remarks);
        data.put("proofUrl", downloadUrl);
        data.put("ticketId", ticketId);
        data.put("timestamp", ServerValue.TIMESTAMP);

        dbReportRef.setValue(data).addOnCompleteListener(task -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "Submitted");
            updates.put("technicianRemarks", remarks);
            updates.put("proofImageUrl", downloadUrl);
            updates.put("resubmissionTimestamp", ServerValue.TIMESTAMP);

            mDatabase.child("ServiceRequests").child(ticketId).updateChildren(updates).addOnCompleteListener(t -> {
                try { pd.dismiss(); } catch (Exception ignored) {}
                AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " resubmitted report for ticket #" + ticketId);
                Toast.makeText(ResubmitReportActivity.this, "Report Resubmitted Successfully!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(ResubmitReportActivity.this, TechnicianDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
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
