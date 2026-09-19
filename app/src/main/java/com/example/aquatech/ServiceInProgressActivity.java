package com.example.aquatech;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class ServiceInProgressActivity extends AppCompatActivity {

    private String ticketId;
    private DatabaseReference requestRef;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_in_progress_template);

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) {
            Toast.makeText(this, "Request ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupStatusBar();
        initializeDataSync();
        
        findViewById(R.id.ipLogoImg).setOnClickListener(v -> finish());
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeDataSync() {
        requestRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    updateUI(snapshot);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUI(DataSnapshot snapshot) {
        String sro = snapshot.child("referenceNo").getValue(String.class);
        ((TextView)findViewById(R.id.ipTicketId)).setText(sro != null ? sro : ticketId);

        String confirmedTime = snapshot.child("startTime").getValue(String.class);
        String schedDate = snapshot.child("date").getValue(String.class);
        String session = "MORNING";
        if (confirmedTime != null && (confirmedTime.toUpperCase().contains("PM") || confirmedTime.startsWith("12"))) {
            session = "AFTERNOON";
        }
        
        String scheduleText = (schedDate != null ? schedDate : "TBC") + " • " + (confirmedTime != null ? confirmedTime.toUpperCase() : "TBC");
        if (confirmedTime != null && !confirmedTime.equalsIgnoreCase("To be confirmed")) {
            scheduleText += " (" + session + ")";
        } else {
            scheduleText = (schedDate != null ? schedDate : "Pending") + " • TBC (PENDING CONFIRMATION)";
        }
        ((TextView)findViewById(R.id.ipScheduleSummary)).setText(scheduleText);

        TextView statusBadge = findViewById(R.id.ipStatusBadge);
        statusBadge.setText("IN PROGRESS");
        statusBadge.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
        statusBadge.setTextColor(Color.parseColor("#2563EB"));
        TextViewCompat.setCompoundDrawableTintList(statusBadge, ColorStateList.valueOf(Color.parseColor("#2563EB")));

        String techName = snapshot.child("assignedTechName").getValue(String.class);
        ((TextView)findViewById(R.id.ipTechName)).setText(techName != null ? techName : "Assigning Technician...");

        String unitModel = snapshot.child("unitName").getValue(String.class);
        String unitNo = snapshot.child("unitNumber").getValue(String.class);
        if (unitNo == null) unitNo = snapshot.child("itemUnitNumber").getValue(String.class);
        
        String product = "STANDING WATER PURIFIER";
        String template = "ST-FXCU1-M-HCA-WT-**-***";
        
        if (unitModel != null) {
            String upper = unitModel.toUpperCase();
            if (upper.contains("CUBE")) { product = "WL CUBE FIREWALL"; template = "F-FXCU1-M-HCA-TT-K1-**-***"; }
            else if (upper.contains("SLIM")) { product = "SMART SLIM"; template = "S-FXCU1-M-HCA-AA-B2-**-***"; }
            else if (upper.contains("COUNTER")) { product = "COUNTER TOP WATER PURIFIER"; template = "CT-FXCU1-M-HCA-WT-**-***"; }
        }
        
        ((TextView)findViewById(R.id.ipUnitName)).setText(product);
        ((TextView)findViewById(R.id.ipUnitCode)).setText(template.replace("**-***", unitNo != null ? unitNo : "0000"));

        Object total = snapshot.child("totalAmount").getValue();
        if (total instanceof Number) {
            ((TextView)findViewById(R.id.ipTotalAmount)).setText(String.format(Locale.getDefault(), "₱%,.2f", ((Number) total).doubleValue()));
        }
    }
}