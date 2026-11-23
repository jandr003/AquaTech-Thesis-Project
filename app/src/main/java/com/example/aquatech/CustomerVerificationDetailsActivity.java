package com.example.aquatech;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomerVerificationDetailsActivity extends AppCompatActivity {

    private ImageView ivBannerIcon;
    private ImageView icStepBasic;
    private ImageView icStepSemi;
    private ImageView icStepFull;
    private TextView tvBannerTitle;
    private TextView tvBannerSubtitle;
    private TextView tvLevelVal;
    private TextView tvIdTypeVal;
    private TextView tvIdNumVal;
    private TextView tvStatusVal;
    private TextView tvDateVal;
    private FirebaseAuth mAuth;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;
    private String selectedIdType = "Government-issued ID";
    private String enteredIdNumber = "";
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_verification_details);

        mAuth = FirebaseAuth.getInstance();
        setupPhotoPicker();
        initializeViews();
        loadVerificationRecord();
    }

    private void setupPhotoPicker() {
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        uploadIdPhotoToFirebase(uri);
                    }
                }
        );
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackVerificationDetails);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ivBannerIcon = findViewById(R.id.ivVerifBannerIcon);
        tvBannerTitle = findViewById(R.id.tvVerifBannerTitle);
        tvBannerSubtitle = findViewById(R.id.tvVerifBannerSubtitle);

        tvLevelVal = findViewById(R.id.tvVerifLevelVal);
        tvIdTypeVal = findViewById(R.id.tvVerifIdTypeVal);
        tvIdNumVal = findViewById(R.id.tvVerifIdNumberVal);
        tvStatusVal = findViewById(R.id.tvVerifStatusVal);
        tvDateVal = findViewById(R.id.tvVerifDateVal);

        icStepBasic = findViewById(R.id.icStepBasic);
        icStepSemi = findViewById(R.id.icStepSemi);
        icStepFull = findViewById(R.id.icStepFull);

        View btnUploadId = findViewById(R.id.btnUploadIdForVerif);
        if (btnUploadId != null) {
            btnUploadId.setOnClickListener(v -> showIdTypeSelectionDialog());
        }
    }

    private void showIdTypeSelectionDialog() {
        String[] idTypes = {
                "Driver's License",
                "Passport",
                "UMID / SSS ID",
                "PhilHealth ID",
                "Postal ID",
                "Voter's ID",
                "PRC ID",
                "Government-issued ID"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Government ID Type");
        builder.setItems(idTypes, (dialog, which) -> {
            selectedIdType = idTypes[which];
            promptIdNumberInput();
        });
        builder.show();
    }

    private void promptIdNumberInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter " + selectedIdType + " Number");

        final EditText input = new EditText(this);
        input.setHint("Enter your official ID Number");
        input.setPadding(36, 36, 36, 36);
        builder.setView(input);

        builder.setPositiveButton("Next: Select ID Photo", (dialog, which) -> {
            String val = input.getText().toString().trim();
            if (val.isEmpty()) {
                Toast.makeText(this, "Please enter your ID Number to proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            enteredIdNumber = val;
            launchPhotoPicker();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }

    private void launchPhotoPicker() {
        if (pickMediaLauncher != null) {
            pickMediaLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        }
    }

    private void uploadIdPhotoToFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || imageUri == null) {
            return;
        }

        Toast.makeText(this, "Uploading ID for verification...", Toast.LENGTH_SHORT).show();

        String uid = user.getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("CustomerGovIDs").child(uid + ".jpg");

        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String photoUrl = downloadUri.toString();
                String todayStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());

                Map<String, Object> updates = new HashMap<>();
                updates.put("govIdUrl", photoUrl);
                updates.put("govIdType", selectedIdType);
                if (!enteredIdNumber.isEmpty()) {
                    updates.put("govIdNumber", enteredIdNumber);
                }
                updates.put("govIdVerified", false);
                updates.put("verificationLevel", "Semi-Verified");
                updates.put("verificationDate", todayStr);

                FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(CustomerVerificationDetailsActivity.this, "ID submitted successfully! Status updated to Semi-Verified.", Toast.LENGTH_LONG).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CustomerVerificationDetailsActivity.this, "Failed to update record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(CustomerVerificationDetailsActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadVerificationRecord() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (s.exists()) {
                    Boolean isVerified = s.child("govIdVerified").getValue(Boolean.class);
                    if (isVerified == null) {
                        isVerified = s.child("isVerified").getValue(Boolean.class);
                    }

                    String level = s.child("verificationLevel").getValue(String.class);
                    if (level == null || level.isEmpty()) {
                        level = (isVerified != null && isVerified) ? "Fully Verified" : "Basic Level";
                    }

                    String idType = s.child("govIdType").getValue(String.class);
                    if (idType == null) {
                        idType = s.child("idType").getValue(String.class);
                    }

                    String idNum = s.child("govIdNumber").getValue(String.class);
                    if (idNum == null) {
                        idNum = s.child("idNumber").getValue(String.class);
                    }

                    String verifDate = s.child("verificationDate").getValue(String.class);
                    if (verifDate == null) {
                        verifDate = s.child("verifiedAt").getValue(String.class);
                    }

                    if (tvLevelVal != null) {
                        tvLevelVal.setText(level);
                    }

                    if (isVerified != null && isVerified) {
                        if (tvIdTypeVal != null) {
                            tvIdTypeVal.setText(idType != null && !idType.isEmpty() ? idType : "Government-issued ID");
                        }
                        if (tvIdNumVal != null) {
                            if (idNum != null && !idNum.isEmpty()) {
                                tvIdNumVal.setText("••••••••" + (idNum.length() >= 4 ? idNum.substring(idNum.length() - 4) : idNum));
                            } else {
                                tvIdNumVal.setText("••••••••5678");
                            }
                        }
                        if (tvDateVal != null) {
                            tvDateVal.setText(verifDate != null && !verifDate.isEmpty() ? verifDate : "Approved Record");
                        }
                        if (ivBannerIcon != null) {
                            ivBannerIcon.setImageResource(R.drawable.ic_check_on);
                            ivBannerIcon.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Fully Verified");
                            tvBannerTitle.setTextColor(Color.parseColor("#15803D"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("Your account verification is complete and approved.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Approved");
                            tvStatusVal.setTextColor(Color.parseColor("#16A34A"));
                        }
                        if (icStepBasic != null) {
                            icStepBasic.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (icStepSemi != null) {
                            icStepSemi.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (icStepFull != null) {
                            icStepFull.setColorFilter(Color.parseColor("#16A34A"));
                        }
                    } else if ("Semi-Verified".equalsIgnoreCase(level)) {
                        if (tvIdTypeVal != null) {
                            tvIdTypeVal.setText(idType != null && !idType.isEmpty() ? idType : "Government-issued ID");
                        }
                        if (tvIdNumVal != null) {
                            if (idNum != null && !idNum.isEmpty()) {
                                tvIdNumVal.setText("••••••••" + (idNum.length() >= 4 ? idNum.substring(idNum.length() - 4) : idNum));
                            } else {
                                tvIdNumVal.setText("Under Admin Review");
                            }
                        }
                        if (tvDateVal != null) {
                            tvDateVal.setText(verifDate != null && !verifDate.isEmpty() ? verifDate : "Pending Approval");
                        }
                        if (ivBannerIcon != null) {
                            ivBannerIcon.setImageResource(R.drawable.ic_clock);
                            ivBannerIcon.setColorFilter(Color.parseColor("#0284C7"));
                        }
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Semi-Verified");
                            tvBannerTitle.setTextColor(Color.parseColor("#0284C7"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("ID photo submitted successfully. Under Dispatch Review.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Under Review");
                            tvStatusVal.setTextColor(Color.parseColor("#0284C7"));
                        }
                        if (icStepBasic != null) {
                            icStepBasic.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (icStepSemi != null) {
                            icStepSemi.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (icStepFull != null) {
                            icStepFull.setColorFilter(Color.parseColor("#94A3B8"));
                        }
                    } else {
                        if (tvIdTypeVal != null) {
                            tvIdTypeVal.setText("Not Submitted");
                        }
                        if (tvIdNumVal != null) {
                            tvIdNumVal.setText("Not Submitted");
                        }
                        if (tvDateVal != null) {
                            tvDateVal.setText("Not Available");
                        }
                        if (ivBannerIcon != null) {
                            ivBannerIcon.setImageResource(R.drawable.ic_help_center);
                            ivBannerIcon.setColorFilter(Color.parseColor("#D97706"));
                        }
                        if (tvBannerTitle != null) {
                            tvBannerTitle.setText("Basic Level");
                            tvBannerTitle.setTextColor(Color.parseColor("#D97706"));
                        }
                        if (tvBannerSubtitle != null) {
                            tvBannerSubtitle.setText("Initial account info completed. Upload ID for full verification.");
                        }
                        if (tvStatusVal != null) {
                            tvStatusVal.setText("Basic Registration");
                            tvStatusVal.setTextColor(Color.parseColor("#D97706"));
                        }
                        if (icStepBasic != null) {
                            icStepBasic.setColorFilter(Color.parseColor("#16A34A"));
                        }
                        if (icStepSemi != null) {
                            icStepSemi.setColorFilter(Color.parseColor("#94A3B8"));
                        }
                        if (icStepFull != null) {
                            icStepFull.setColorFilter(Color.parseColor("#94A3B8"));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CustomerVerificationDetailsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
