package com.example.aquatech;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
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

import java.text.SimpleDateFormat;
import java.util.Date;
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
        if (sro == null) sro = snapshot.child("sroNumber").getValue(String.class);
        ((TextView)findViewById(R.id.ipTicketId)).setText(sro != null ? sro : ticketId);

        Long subTs = snapshot.child("timestamp").getValue(Long.class);
        if (subTs != null) {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(new Date(subTs));
            ((TextView)findViewById(R.id.ipSubmittedTime)).setText(dateStr);
        }

        Long accTs = snapshot.child("assignedTimestamp").getValue(Long.class);
        if (accTs != null) {
            String accStr = new SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(new Date(accTs));
            ((TextView)findViewById(R.id.ipAcceptedTime)).setText(accStr);
            ((TextView)findViewById(R.id.ipAssignedOn)).setText(accStr);
        } else {
            ((TextView)findViewById(R.id.ipAcceptedTime)).setText("Awaiting Tech...");
            ((TextView)findViewById(R.id.ipAssignedOn)).setText("Pending");
        }

        TextView statusBadge = findViewById(R.id.ipStatusBadge);
        statusBadge.setText("IN PROGRESS");
        statusBadge.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
        statusBadge.setTextColor(Color.parseColor("#2563EB"));
        TextViewCompat.setCompoundDrawableTintList(statusBadge, ColorStateList.valueOf(Color.parseColor("#2563EB")));

        ((TextView)findViewById(R.id.ipCustomerName)).setText(snapshot.child("customerName").getValue(String.class));
        ((TextView)findViewById(R.id.ipContact)).setText(snapshot.child("contactNumber").getValue(String.class));
        ((TextView)findViewById(R.id.ipAddress)).setText(snapshot.child("address").getValue(String.class));

        String unitModel = snapshot.child("unitName").getValue(String.class);
        if (unitModel == null) unitModel = snapshot.child("unitModel").getValue(String.class);
        ((TextView)findViewById(R.id.ipUnitName)).setText(unitModel != null ? unitModel : "---");
        
        String unitNo = snapshot.child("unitNumber").getValue(String.class);
        if (unitNo == null) unitNo = snapshot.child("itemUnitNumber").getValue(String.class);
        ((TextView)findViewById(R.id.ipUnitCode)).setText(unitNo != null ? unitNo : "---");
        
        String loc = snapshot.child("installationLocation").getValue(String.class);
        ((TextView)findViewById(R.id.ipLocation)).setText(loc != null ? loc : "---");

        String type = snapshot.child("serviceType").getValue(String.class);
        ((TextView)findViewById(R.id.ipServiceType)).setText(type != null ? type : "General Service");
        
        String remarks = snapshot.child("remarks").getValue(String.class);
        ((TextView)findViewById(R.id.ipRemarks)).setText(remarks != null ? remarks : "N/A");
        ((TextView)findViewById(R.id.ipConcern)).setText(remarks != null ? remarks : "System Maintenance");

        StringBuilder items = new StringBuilder();
        int totalQty = 0;
        for (DataSnapshot child : snapshot.getChildren()) {
            if (child.getKey().startsWith("qty_")) {
                int q = 0;
                try { q = child.getValue(Integer.class); } catch (Exception e) {}
                if (q > 0) {
                    items.append(getItemName(child.getKey())).append(" x ").append(q).append("\n");
                    totalQty += q;
                }
            }
        }
        ((TextView)findViewById(R.id.ipItemName)).setText(items.length() > 0 ? items.toString().trim() : "Standard Parts");
        ((TextView)findViewById(R.id.ipItemQty)).setText(String.valueOf(totalQty));

        ((TextView)findViewById(R.id.ipPaymentMethod)).setText(snapshot.child("paymentMethod").getValue(String.class));
        Object totalAmount = snapshot.child("totalAmount").getValue();
        if (totalAmount instanceof Number) {
            ((TextView)findViewById(R.id.ipTotalAmount)).setText(String.format(Locale.getDefault(), "₱ %,.2f", ((Number)totalAmount).doubleValue()));
        }

        String techName = snapshot.child("assignedTechName").getValue(String.class);
        ((TextView)findViewById(R.id.ipAssignedTech)).setText(techName != null ? techName : "Assigning...");
        
        String techContact = snapshot.child("techContact").getValue(String.class);
        ((TextView)findViewById(R.id.ipTechContact)).setText(techContact != null ? techContact : "---");

        String date = snapshot.child("date").getValue(String.class);
        String startTime = snapshot.child("startTime").getValue(String.class);
        if (date != null && startTime != null) {
            ((TextView)findViewById(R.id.ipScheduleSummary)).setText(date + " • " + startTime);
        }
    }

    private String getItemName(String key) {
        switch (key) {
            case "qty_wayvalve": return "3-Way Valve";
            case "qty_cbc": return "Filter 0064-CBC";
            case "qty_sediment": return "Filter 0055-SEDIMENT";
            case "qty_aquatal": return "Aquatal Replacement";
            case "qty_inline": return "Inline Filter";
            case "qty_uvlamp": return "UV Lamp";
            case "qty_touchpanel": return "Touch Panel";
            case "qty_pbcboard": return "PBC Board";
            case "qty_smsf1": return "SMSF 1u CBC";
            case "qty_smsf10": return "SMSF 10u SED";
            default: return "Service Item";
        }
    }
}