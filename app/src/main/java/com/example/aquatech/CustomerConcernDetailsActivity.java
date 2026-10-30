package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class CustomerConcernDetailsActivity extends AppCompatActivity {

    private static final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private String ticketId;

    private TextView tvConcernID, tvConcernStatusPill, tvCustomerID, tvSubmittedDate;
    private TextView tvUnitItemName, tvCustomerItemCode;
    private TextView tvConcernCategory, tvCustomerConcernText;
    private TextView tvCustomerName, tvServiceAddress, tvContactNumber;
    private TextView tvOriginalReqDate, tvRequestedTime;
    private LinearLayout layoutRequestedTime, layoutRescheduleInfo;
    private TextView tvOriginalReschedText, tvNewReschedText, tvReschedReasonText, tvScheduleStatusPill;
    private TextView tvGovIdStatusPill;
    private ImageView ivGovIdAttachment;
    private TextView tvTechName, tvTechId;
    private TextView tvResolutionAction, tvResolutionRemarks, tvResolvedBy, tvDateResolved;
    private TextView tvCompletionTimeDetails, tvSubmissionTimeDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_concern_details);

        setupStatusBar();
        initializeViews();

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null || ticketId.isEmpty()) {
            Toast.makeText(this, "Concern ID not found", Toast.LENGTH_SHORT).show();
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
        findViewById(R.id.btnBackConcernDetails).setOnClickListener(v -> finish());

        tvConcernID = findViewById(R.id.tvConcernIDDetails);
        tvConcernStatusPill = findViewById(R.id.tvConcernStatusPill);
        tvCustomerID = findViewById(R.id.tvCustomerIDConcern);
        tvSubmittedDate = findViewById(R.id.tvSubmittedDateConcern);

        tvUnitItemName = findViewById(R.id.tvUnitItemNameConcern);
        tvCustomerItemCode = findViewById(R.id.tvCustomerItemCodeConcern);

        tvConcernCategory = findViewById(R.id.tvConcernCategory);
        tvCustomerConcernText = findViewById(R.id.tvCustomerConcernText);

        tvCustomerName = findViewById(R.id.tvCustomerNameConcern);
        tvServiceAddress = findViewById(R.id.tvServiceAddressConcern);
        tvContactNumber = findViewById(R.id.tvContactNumberConcern);

        tvOriginalReqDate = findViewById(R.id.tvOriginalReqDate);
        tvRequestedTime = findViewById(R.id.tvRequestedTime);
        layoutRequestedTime = findViewById(R.id.layoutRequestedTime);

        layoutRescheduleInfo = findViewById(R.id.layoutRescheduleInfo);
        tvScheduleStatusPill = findViewById(R.id.tvScheduleStatusPill);
        tvOriginalReschedText = findViewById(R.id.tvOriginalReschedText);
        tvNewReschedText = findViewById(R.id.tvNewReschedText);
        tvReschedReasonText = findViewById(R.id.tvReschedReasonText);

        tvGovIdStatusPill = findViewById(R.id.tvGovIdStatusPill);
        ivGovIdAttachment = findViewById(R.id.ivGovIdAttachment);

        tvTechName = findViewById(R.id.tvTechNameConcern);
        tvTechId = findViewById(R.id.tvTechIdConcern);

        tvResolutionAction = findViewById(R.id.tvResolutionAction);
        tvResolutionRemarks = findViewById(R.id.tvResolutionRemarks);
        tvResolvedBy = findViewById(R.id.tvResolvedBy);
        tvDateResolved = findViewById(R.id.tvDateResolved);

        tvCompletionTimeDetails = findViewById(R.id.tvCompletionTimeDetails);
        tvSubmissionTimeDetails = findViewById(R.id.tvSubmissionTimeDetails);
    }

    private void setupFirebase() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String concernId = snapshot.child("ticketId").getValue(String.class);
                    if (concernId == null) concernId = snapshot.getKey();
                    tvConcernID.setText(concernId != null ? concernId : "ASC2026-0031");

                    String custId = snapshot.child("customerId").getValue(String.class);
                    if (custId == null) {
                        String uid = snapshot.child("userId").getValue(String.class);
                        if (uid != null && uid.length() > 5) custId = "ASC-C" + uid.substring(0, 4).toUpperCase();
                        else custId = "ASC-C001";
                    }
                    tvCustomerID.setText(custId);

                    String dateStr = snapshot.child("date").getValue(String.class);
                    tvSubmittedDate.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "Sep 30, 2025");

                    String status = snapshot.child("status").getValue(String.class);
                    if (status == null || status.isEmpty()) status = "Resolved";
                    tvConcernStatusPill.setText(status);

                    if ("Completed".equalsIgnoreCase(status) || "Resolved".equalsIgnoreCase(status)) {
                        tvConcernStatusPill.setBackgroundResource(R.drawable.bg_solid_green_pill);
                        tvConcernStatusPill.setTextColor(Color.WHITE);
                    } else if ("Cancelled".equalsIgnoreCase(status)) {
                        tvConcernStatusPill.setBackgroundResource(R.drawable.bg_solid_red_pill);
                        tvConcernStatusPill.setTextColor(Color.WHITE);
                    } else {
                        tvConcernStatusPill.setBackgroundResource(R.drawable.bg_solid_amber_pill);
                        tvConcernStatusPill.setTextColor(Color.WHITE);
                    }

                    String itemUnit = snapshot.child("unitName").getValue(String.class);
                    tvUnitItemName.setText(itemUnit != null && !itemUnit.isEmpty() ? itemUnit : "Registered Water Dispenser");

                    String itemCode = snapshot.child("unitCode").getValue(String.class);
                    if (itemCode == null) itemCode = snapshot.child("unitModelCode").getValue(String.class);
                    tvCustomerItemCode.setText(itemCode != null && !itemCode.isEmpty() ? itemCode : "F-FXCU1-M-HCA-TT-XX-XXX");

                    String category = snapshot.child("concernCategory").getValue(String.class);
                    tvConcernCategory.setText(category != null && !category.isEmpty() ? category : "Equipment Issue");

                    String concern = snapshot.child("customerConcern").getValue(String.class);
                    if (concern == null) concern = snapshot.child("remarks").getValue(String.class);
                    tvCustomerConcernText.setText(concern != null && !concern.isEmpty() ? concern : "Water dispenser is not dispensing cold water.");

                    String custName = snapshot.child("customerName").getValue(String.class);
                    tvCustomerName.setText(custName != null && !custName.isEmpty() ? custName : "Registered Customer Name");

                    String address = snapshot.child("address").getValue(String.class);
                    tvServiceAddress.setText(address != null && !address.isEmpty() ? address : "Complete registered service address");

                    String phone = snapshot.child("customerPhone").getValue(String.class);
                    if (phone == null) phone = snapshot.child("userPhone").getValue(String.class);
                    tvContactNumber.setText(phone != null && !phone.isEmpty() ? phone : "Registered mobile number");

                    // SCHEDULE vs RESCHEDULE VISIBILITY LOGIC
                    Boolean isRescheduled = snapshot.child("isRescheduled").getValue(Boolean.class);
                    String scheduleStatus = snapshot.child("scheduleStatus").getValue(String.class);

                    boolean rescheduled = (isRescheduled != null && isRescheduled) ||
                            "Rescheduled".equalsIgnoreCase(scheduleStatus) ||
                            snapshot.hasChild("rescheduleReason");

                    if (rescheduled) {
                        layoutRequestedTime.setVisibility(View.GONE);
                        layoutRescheduleInfo.setVisibility(View.VISIBLE);

                        String origDate = snapshot.child("originalDate").getValue(String.class);
                        String origTime = snapshot.child("originalTimeRange").getValue(String.class);
                        tvOriginalReschedText.setText("Original: " + (origDate != null ? origDate : "October 2, 2025") + " · " + (origTime != null ? origTime : "9:00–11:00 AM"));

                        String newDate = snapshot.child("date").getValue(String.class);
                        String newTime = snapshot.child("timeRange").getValue(String.class);
                        tvNewReschedText.setText("New Schedule: " + (newDate != null ? newDate : "October 3, 2025") + " · " + (newTime != null ? newTime : "1:00–3:00 PM"));

                        String reason = snapshot.child("rescheduleReason").getValue(String.class);
                        tvReschedReasonText.setText("Reason: " + (reason != null && !reason.isEmpty() ? reason : "Previous schedule was not fulfilled."));
                    } else {
                        layoutRequestedTime.setVisibility(View.VISIBLE);
                        layoutRescheduleInfo.setVisibility(View.GONE);

                        String reqDate = snapshot.child("date").getValue(String.class);
                        tvOriginalReqDate.setText(reqDate != null && !reqDate.isEmpty() ? reqDate : "October 2, 2025");

                        String timeSlot = snapshot.child("timeRange").getValue(String.class);
                        tvRequestedTime.setText(timeSlot != null && !timeSlot.isEmpty() ? timeSlot : "9:00 AM – 11:00 AM");
                    }

                    String govIdUrl = snapshot.child("govIdUrl").getValue(String.class);
                    if (govIdUrl == null) govIdUrl = snapshot.child("idAttachmentUrl").getValue(String.class);
                    if (govIdUrl == null) govIdUrl = snapshot.child("governmentIdUrl").getValue(String.class);

                    Boolean govVerified = snapshot.child("govIdVerified").getValue(Boolean.class);
                    if (govVerified == null) govVerified = snapshot.child("isVerified").getValue(Boolean.class);

                    if (govIdUrl != null && !govIdUrl.isEmpty()) {
                        ivGovIdAttachment.setVisibility(View.VISIBLE);
                        Glide.with(CustomerConcernDetailsActivity.this)
                                .load(govIdUrl)
                                .placeholder(R.drawable.img_place_holder)
                                .into(ivGovIdAttachment);

                        if (govVerified != null && govVerified) {
                            tvGovIdStatusPill.setText("Verified");
                            tvGovIdStatusPill.setBackgroundResource(R.drawable.bg_solid_green_pill);
                            tvGovIdStatusPill.setTextColor(Color.WHITE);
                        } else {
                            tvGovIdStatusPill.setText("Pending Verification");
                            tvGovIdStatusPill.setBackgroundResource(R.drawable.bg_solid_amber_pill);
                            tvGovIdStatusPill.setTextColor(Color.WHITE);
                        }
                    } else {
                        ivGovIdAttachment.setVisibility(View.GONE);
                        tvGovIdStatusPill.setText("Not Submitted");
                        tvGovIdStatusPill.setBackgroundResource(R.drawable.bg_solid_amber_pill);
                        tvGovIdStatusPill.setTextColor(Color.WHITE);
                    }

                    String techName = snapshot.child("assignedTechName").getValue(String.class);
                    tvTechName.setText(techName != null && !techName.isEmpty() ? techName : "Assigned Technician Name");

                    String techId = snapshot.child("assignedTechId").getValue(String.class);
                    tvTechId.setText("Technician ID: " + (techId != null && !techId.isEmpty() ? techId : "TECH-001"));

                    String action = snapshot.child("resolutionAction").getValue(String.class);
                    tvResolutionAction.setText(action != null && !action.isEmpty() ? action : "Unit Inspection and Corrective Service");

                    String remarks = snapshot.child("technicianRemarks").getValue(String.class);
                    tvResolutionRemarks.setText(remarks != null && !remarks.isEmpty() ? remarks : "Recorded explanation of the corrective action and outcome of the service.");

                    tvResolvedBy.setText(techName != null && !techName.isEmpty() ? techName : "Assigned Technician / Authorized Staff");

                    String compDate = snapshot.child("completionTime").getValue(String.class);
                    tvDateResolved.setText(compDate != null && !compDate.isEmpty() ? compDate : (dateStr != null ? dateStr : "Actual resolution date and time"));

                    ImageView ivProofInline = findViewById(R.id.ivProofOfServiceInline);
                    String proofUrl = snapshot.child("proofImageUrl").getValue(String.class);
                    if (proofUrl == null) proofUrl = snapshot.child("proofUrl").getValue(String.class);
                    if (ivProofInline != null && proofUrl != null && !proofUrl.isEmpty()) {
                        Glide.with(CustomerConcernDetailsActivity.this)
                                .load(proofUrl)
                                .placeholder(R.drawable.img_place_holder)
                                .into(ivProofInline);
                    }

                    RatingBar rbRating = findViewById(R.id.rbCustomerRatingInline);
                    TextView tvScore = findViewById(R.id.tvRatingScoreTextInline);
                    Float rating = snapshot.child("rating").getValue(Float.class);
                    if (rbRating != null && rating != null) {
                        rbRating.setRating(rating);
                        if (tvScore != null) tvScore.setText(String.format(Locale.US, "%.1f / 5.0", rating));
                    }

                    String compTimeStr = snapshot.child("maintenanceEndTimeStr").getValue(String.class);
                    if (compTimeStr == null) compTimeStr = snapshot.child("completedTime").getValue(String.class);
                    if (compTimeStr == null && compDate != null && compDate.contains(" ")) {
                        compTimeStr = compDate;
                    }
                    tvCompletionTimeDetails.setText(compTimeStr != null && !compTimeStr.isEmpty() ? compTimeStr : "10:42 AM");

                    String subTimeStr = snapshot.child("submittedAtStr").getValue(String.class);
                    if (subTimeStr == null) subTimeStr = snapshot.child("submissionTimestamp").getValue(String.class);
                    if (subTimeStr == null && compDate != null) {
                        subTimeStr = compDate;
                    }
                    tvSubmissionTimeDetails.setText(subTimeStr != null && !subTimeStr.isEmpty() ? subTimeStr : "10:45 AM");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
