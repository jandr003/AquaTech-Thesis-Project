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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceInProgressActivity extends AppCompatActivity {

    private String ticketId;
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
        DatabaseReference requestRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
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
            Date subDate = new Date(subTs);
            String d = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(subDate);
            String t = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(subDate);
            ((TextView)findViewById(R.id.ipSubDate)).setText(d);
            ((TextView)findViewById(R.id.ipSubTime)).setText(t);
        }

        Long accTs = snapshot.child("assignedTimestamp").getValue(Long.class);
        if (accTs != null) {
            Date accDate = new Date(accTs);
            String d = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(accDate);
            String t = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(accDate);
            ((TextView)findViewById(R.id.ipAccDate)).setText(d);
            ((TextView)findViewById(R.id.ipAccTime)).setText(t);
            ((TextView)findViewById(R.id.ipAssignedOn)).setText(d + " • " + t);
        } else {
            ((TextView)findViewById(R.id.ipAccDate)).setText("Awaiting Tech...");
            ((TextView)findViewById(R.id.ipAccTime)).setText("");
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
        
        String unitNumber = snapshot.child("unitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = snapshot.child("itemUnitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = "0000";

        String product = "STANDING WATER PURIFIER";
        String template = "ST-FXCU1-M-HCA-WT-**-***";
        
        if (unitModel != null) {
            String upper = unitModel.toUpperCase();
            if (upper.contains("CUBE")) { product = "WL CUBE FIREWALL"; template = "F-FXCU1-M-HCA-TT-K1-**-***"; }
            else if (upper.contains("SLIM")) { product = "SMART SLIM"; template = "S-FXCU1-M-HCA-AA-B2-**-***"; }
            else if (upper.contains("COUNTER")) { product = "COUNTER TOP WATER PURIFIER"; template = "CT-FXCU1-M-HCA-WT-**-***"; }
        }

        ((TextView)findViewById(R.id.ipUnitName)).setText(product);
        ((TextView)findViewById(R.id.ipUnitCode)).setText(template.replace("**-***", unitNumber));
        
        String pType = snapshot.child("purchaseType").getValue(String.class);
        ((TextView)findViewById(R.id.ipLocation)).setText(pType != null ? pType.toUpperCase() : "SUBSCRIPTION");

        String type = snapshot.child("serviceType").getValue(String.class);
        ((TextView)findViewById(R.id.ipServiceType)).setText(type != null ? type : "General Service");
        
        String rawRemarks = snapshot.child("remarks").getValue(String.class);
        String p = (rawRemarks != null ? rawRemarks.toLowerCase() : "");

        String smartDesc = "Technical system inspection and evaluation.";
        if (p.contains("leak") || p.contains("tulo")) smartDesc = "Onsite technical repair and parts verification.";
        else if (p.contains("maintenance") || p.contains("filter")) smartDesc = "Scheduled preventive maintenance and health check.";
        ((TextView)findViewById(R.id.ipDescription)).setText(smartDesc);

        ((TextView)findViewById(R.id.ipConcern)).setText(rawRemarks != null ? rawRemarks : "N/A");
        ((TextView)findViewById(R.id.ipRemarks)).setText(rawRemarks != null ? rawRemarks : "N/A");

        StringBuilder itemsList = new StringBuilder();
        int totalQty = 0;
        for (DataSnapshot child : snapshot.getChildren()) {
            String key = child.getKey();
            if (key != null && key.startsWith("qty_")) {
                int q = 0;
                try { q = child.getValue(Integer.class); } catch (Exception ignored) {}
                if (q > 0) {
                    itemsList.append(getItemName(key)).append(" x ").append(q).append("\n");
                    totalQty += q;
                }
            }
        }
        ((TextView)findViewById(R.id.ipItemName)).setText(itemsList.length() > 0 ? itemsList.toString().trim() : "Standard Parts");
        ((TextView)findViewById(R.id.ipItemQty)).setText(String.valueOf(totalQty));

        String pm = snapshot.child("paymentMethod").getValue(String.class);
        String pStatus = snapshot.child("paymentStatus").getValue(String.class);
        boolean isPaid = "PAID".equalsIgnoreCase(pStatus);
        
        TextView payBadge = findViewById(R.id.ipPaymentStatus);
        ((TextView)findViewById(R.id.ipPaymentMethod)).setText(pm != null ? pm : "COD");

        if (isPaid) {
            payBadge.setText("PAID");
            payBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
            payBadge.setTextColor(Color.parseColor("#166534")); 
            TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.parseColor("#166534")));
        } else if (pm != null && pm.equalsIgnoreCase("COD")) {
            payBadge.setText("PAYMENT DUE");
            payBadge.setBackgroundResource(R.drawable.bg_pdf_status_pending_pill);
            payBadge.setTextColor(Color.parseColor("#D97706"));
            TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.parseColor("#D97706")));
        } else {
            payBadge.setText("FOR VERIFICATION");
            payBadge.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
            payBadge.setTextColor(Color.parseColor("#2563EB")); 
            TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.parseColor("#2563EB")));
        }
        
        View layoutIpRef = findViewById(R.id.layoutIpReference);
        if (pm != null && !pm.equalsIgnoreCase("COD")) {
            String pRef = snapshot.child("paymentReference").getValue(String.class);
            if (pRef != null && !pRef.isEmpty()) {
                layoutIpRef.setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.ipPaymentRef)).setText(pRef);
            } else {
                layoutIpRef.setVisibility(View.GONE);
            }
        } else {
            layoutIpRef.setVisibility(View.GONE);
        }

        String techName = snapshot.child("assignedTechName").getValue(String.class);
        ((TextView)findViewById(R.id.ipAssignedTech)).setText(techName != null ? techName : "Assigning...");
        
        String techPhone = snapshot.child("techContact").getValue(String.class);
        ((TextView)findViewById(R.id.ipTechContact)).setText(techPhone != null ? techPhone : "---");

        TextView techStatusBadge = findViewById(R.id.ipTechStatus);
        techStatusBadge.setText("ACCEPTED");
        techStatusBadge.setBackgroundResource(R.drawable.bg_pdf_status_blue_pill);
        techStatusBadge.setTextColor(Color.parseColor("#2563EB"));
        TextViewCompat.setCompoundDrawableTintList(techStatusBadge, ColorStateList.valueOf(Color.parseColor("#2563EB")));
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