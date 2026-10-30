package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class AdminServiceHistoryDetailsActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private String ticketId;

    private TextView tvTicketID, tvStatusPill, tvServiceType, tvRequestDate, tvCompletionDate;
    private TextView tvItemName, tvItemCode, tvCustomerName, tvServiceAddress, tvCustomerPhone;
    private TextView tvTechName, tvTechId, tvWorkPerformed, tvInspectionFindings, tvTechRemarks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_service_history_details);

        setupStatusBar();
        initializeViews();

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null || ticketId.isEmpty()) {
            Toast.makeText(this, "Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupFirebase();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackHistoryDetails).setOnClickListener(v -> finish());

        tvTicketID = findViewById(R.id.tvTicketIDDetails);
        tvStatusPill = findViewById(R.id.tvStatusPillDetails);

        tvServiceType = findViewById(R.id.tvServiceTypeDetails);
        tvRequestDate = findViewById(R.id.tvRequestDateDetails);
        tvCompletionDate = findViewById(R.id.tvCompletionDateDetails);

        tvItemName = findViewById(R.id.tvItemNameDetails);
        tvItemCode = findViewById(R.id.tvItemCodeDetails);

        tvCustomerName = findViewById(R.id.tvCustomerNameDetails);
        tvServiceAddress = findViewById(R.id.tvServiceAddressDetails);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhoneDetails);

        tvTechName = findViewById(R.id.tvTechNameDetails);
        tvTechId = findViewById(R.id.tvTechIdDetails);

        tvWorkPerformed = findViewById(R.id.tvWorkPerformedDetails);
        tvInspectionFindings = findViewById(R.id.tvInspectionFindingsDetails);
        tvTechRemarks = findViewById(R.id.tvTechRemarksDetails);
    }

    private void setupFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String sroNumber = snapshot.child("referenceNo").getValue(String.class);
                    if (sroNumber == null) sroNumber = snapshot.child("sroNumber").getValue(String.class);
                    if (sroNumber == null) sroNumber = ticketId;

                    if (sroNumber != null && !sroNumber.isEmpty()) {
                        String upper = sroNumber.toUpperCase().trim();
                        if (upper.startsWith("SRO-")) {
                            tvTicketID.setText(upper);
                        } else if (upper.startsWith("SRO")) {
                            tvTicketID.setText("SRO-" + upper.substring(3));
                        } else {
                            try {
                                long num = Long.parseLong(upper.replaceAll("[^0-9]", ""));
                                tvTicketID.setText(String.format(Locale.US, "SRO-%08d", num));
                            } catch (Exception e) {
                                tvTicketID.setText("SRO-" + upper);
                            }
                        }
                    } else {
                        tvTicketID.setText("SRO-00000001");
                    }

                    String status = snapshot.child("status").getValue(String.class);
                    if (status == null || status.isEmpty()) status = "Completed";
                    tvStatusPill.setText(status);

                    if ("Completed".equalsIgnoreCase(status)) {
                        tvStatusPill.setBackgroundResource(R.drawable.bg_solid_green_pill);
                        tvStatusPill.setTextColor(Color.WHITE);
                    } else if ("Cancelled".equalsIgnoreCase(status)) {
                        tvStatusPill.setBackgroundResource(R.drawable.bg_solid_red_pill);
                        tvStatusPill.setTextColor(Color.WHITE);
                    } else if ("In Progress".equalsIgnoreCase(status) || "Assigned".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status)) {
                        tvStatusPill.setBackgroundResource(R.drawable.bg_solid_blue_pill);
                        tvStatusPill.setTextColor(Color.WHITE);
                    } else {
                        tvStatusPill.setBackgroundResource(R.drawable.bg_solid_amber_pill);
                        tvStatusPill.setTextColor(Color.WHITE);
                    }

                    String unitName = snapshot.child("unitName").getValue(String.class);
                    if (unitName == null || unitName.isEmpty()) unitName = "WL CUBE FIREWALL";
                    tvItemName.setText(unitName);

                    String unitCode = snapshot.child("unitCode").getValue(String.class);
                    if (unitCode == null) unitCode = snapshot.child("unitModelCode").getValue(String.class);
                    if (unitCode == null) unitCode = snapshot.child("serialNumber").getValue(String.class);
                    if (unitCode == null) unitCode = snapshot.child("itemCode").getValue(String.class);
                    if (unitCode == null) unitCode = snapshot.child("skuCode").getValue(String.class);

                    tvItemCode.setText((unitCode != null && !unitCode.isEmpty()) ? unitCode : "F-FXCU1-M-HCA-TT-XX-XXX");

                    String customer = snapshot.child("customerName").getValue(String.class);
                    tvCustomerName.setText(customer != null && !customer.isEmpty() ? customer : "Registered Customer Name");

                    String address = snapshot.child("address").getValue(String.class);
                    tvServiceAddress.setText(address != null && !address.isEmpty() ? address : "Complete saved service address");

                    String phone = snapshot.child("customerPhone").getValue(String.class);
                    if (phone == null) phone = snapshot.child("userPhone").getValue(String.class);
                    tvCustomerPhone.setText(phone != null && !phone.isEmpty() ? phone : "Registered contact number");

                    String techName = snapshot.child("assignedTechName").getValue(String.class);
                    tvTechName.setText(techName != null && !techName.isEmpty() ? techName : "Assigned technician");

                    String techId = snapshot.child("assignedTechId").getValue(String.class);
                    tvTechId.setText("Technician ID: " + (techId != null && !techId.isEmpty() ? techId : "TECH-001"));

                    String dateStr = snapshot.child("date").getValue(String.class);
                    tvRequestDate.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "Recorded date");

                    String compDate = snapshot.child("completionTime").getValue(String.class);
                    tvCompletionDate.setText(compDate != null && !compDate.isEmpty() ? compDate : (dateStr != null ? dateStr : "Recorded date"));

                    String serviceType = snapshot.child("serviceTypeLabel").getValue(String.class);
                    tvServiceType.setText(serviceType != null && !serviceType.isEmpty() ? serviceType : "Water System Service");

                    String work = snapshot.child("workPerformed").getValue(String.class);
                    tvWorkPerformed.setText(work != null && !work.isEmpty() ? work : "Recorded maintenance or repair activities");

                    String findings = snapshot.child("inspectionFindings").getValue(String.class);
                    tvInspectionFindings.setText(findings != null && !findings.isEmpty() ? findings : "Recorded inspection results");

                    String remarks = snapshot.child("technicianRemarks").getValue(String.class);
                    tvTechRemarks.setText(remarks != null && !remarks.isEmpty() ? remarks : "Recorded service remarks and outcome");

                    ImageView ivProofInline = findViewById(R.id.ivProofOfServiceInline2);
                    String proofUrl = snapshot.child("proofImageUrl").getValue(String.class);
                    if (proofUrl == null) proofUrl = snapshot.child("proofUrl").getValue(String.class);
                    if (ivProofInline != null && proofUrl != null && !proofUrl.isEmpty()) {
                        Glide.with(AdminServiceHistoryDetailsActivity.this)
                                .load(proofUrl)
                                .placeholder(R.drawable.img_place_holder)
                                .into(ivProofInline);
                    }

                    RatingBar rbRating = findViewById(R.id.rbCustomerRatingInline2);
                    TextView tvScore = findViewById(R.id.tvRatingScoreTextInline2);
                    Float rating = snapshot.child("rating").getValue(Float.class);
                    if (rating != null && rating > 0) {
                        if (rbRating != null) {
                            rbRating.setVisibility(View.VISIBLE);
                            rbRating.setRating(rating);
                        }
                        if (tvScore != null) tvScore.setText(String.format(Locale.US, "%.1f Stars", rating));
                    } else {
                        if (rbRating != null) rbRating.setVisibility(View.GONE);
                        if (tvScore != null) tvScore.setText("Not Rated Yet");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
