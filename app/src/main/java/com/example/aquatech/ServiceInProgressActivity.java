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
        if (sro == null) {
            sro = snapshot.child("sroNumber").getValue(String.class);
        }
        TextView tvTicket = findViewById(R.id.ipTicketId);
        if (tvTicket != null) {
            tvTicket.setText(sro != null ? sro : ticketId);
        }

        Long subTs = snapshot.child("timestamp").getValue(Long.class);
        if (subTs != null) {
            Date subDate = new Date(subTs);
            String d = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(subDate);
            String t = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(subDate);
            TextView tvSubDate = findViewById(R.id.ipSubDate);
            TextView tvSubTime = findViewById(R.id.ipSubTime);
            if (tvSubDate != null) tvSubDate.setText(d);
            if (tvSubTime != null) tvSubTime.setText(t);
        }

        Long accTs = snapshot.child("assignedTimestamp").getValue(Long.class);
        if (accTs != null) {
            Date accDate = new Date(accTs);
            String d = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(accDate);
            String t = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(accDate);
            TextView tvAccDate = findViewById(R.id.ipAccDate);
            TextView tvAccTime = findViewById(R.id.ipAccTime);
            TextView tvAssigned = findViewById(R.id.ipAssignedOn);
            if (tvAccDate != null) tvAccDate.setText(d);
            if (tvAccTime != null) tvAccTime.setText(t);
            if (tvAssigned != null) tvAssigned.setText(d + " • " + t);
        } else {
            TextView tvAccDate = findViewById(R.id.ipAccDate);
            TextView tvAccTime = findViewById(R.id.ipAccTime);
            TextView tvAssigned = findViewById(R.id.ipAssignedOn);
            if (tvAccDate != null) tvAccDate.setText("Awaiting Tech...");
            if (tvAccTime != null) tvAccTime.setText("");
            if (tvAssigned != null) tvAssigned.setText("Pending");
        }

        TextView statusBadge = findViewById(R.id.ipStatusBadge);
        if (statusBadge != null) {
            statusBadge.setText("IN PROGRESS");
            statusBadge.setBackgroundResource(R.drawable.bg_solid_blue_pill);
            statusBadge.setTextColor(Color.WHITE);
            TextViewCompat.setCompoundDrawableTintList(statusBadge, ColorStateList.valueOf(Color.WHITE));
        }

        TextView tvCust = findViewById(R.id.ipCustomerName);
        if (tvCust != null) tvCust.setText(snapshot.child("customerName").getValue(String.class));

        TextView tvContact = findViewById(R.id.ipContact);
        if (tvContact != null) tvContact.setText(snapshot.child("contactNumber").getValue(String.class));

        TextView tvAddress = findViewById(R.id.ipAddress);
        if (tvAddress != null) tvAddress.setText(snapshot.child("address").getValue(String.class));

        String unitModel = snapshot.child("unitName").getValue(String.class);
        if (unitModel == null) {
            unitModel = snapshot.child("unitModel").getValue(String.class);
        }

        String unitNumber = snapshot.child("unitNumber").getValue(String.class);
        if (unitNumber == null) {
            unitNumber = snapshot.child("itemUnitNumber").getValue(String.class);
        }
        if (unitNumber == null) {
            unitNumber = "0000";
        }

        String product = "STANDING WATER PURIFIER";
        String template = "ST-FXCU1-M-HCA-WT-**-***";

        if (unitModel != null) {
            String upper = unitModel.toUpperCase();
            if (upper.contains("CUBE")) {
                product = "WL CUBE FIREWALL";
                template = "F-FXCU1-M-HCA-TT-K1-**-***";
            } else if (upper.contains("SLIM")) {
                product = "SMART SLIM";
                template = "S-FXCU1-M-HCA-AA-B2-**-***";
            } else if (upper.contains("COUNTER")) {
                product = "COUNTER TOP WATER PURIFIER";
                template = "CT-FXCU1-M-HCA-WT-**-***";
            }
        }

        TextView tvUnitName = findViewById(R.id.ipUnitName);
        if (tvUnitName != null) tvUnitName.setText(product);

        TextView tvUnitCode = findViewById(R.id.ipUnitCode);
        if (tvUnitCode != null) tvUnitCode.setText(template.replace("**-***", unitNumber));

        String pType = snapshot.child("purchaseType").getValue(String.class);
        TextView tvLocation = findViewById(R.id.ipLocation);
        if (tvLocation != null) tvLocation.setText(pType != null ? pType.toUpperCase() : "SUBSCRIPTION");

        String type = snapshot.child("serviceType").getValue(String.class);
        TextView tvService = findViewById(R.id.ipServiceType);
        if (tvService != null) tvService.setText(type != null ? type : "General Service");

        String rawRemarks = snapshot.child("remarks").getValue(String.class);
        String p = (rawRemarks != null ? rawRemarks.toLowerCase() : "");

        String smartDesc = "Technical system inspection and evaluation.";
        if (p.contains("leak") || p.contains("tulo")) {
            smartDesc = "Onsite technical repair and parts verification.";
        } else if (p.contains("maintenance") || p.contains("filter")) {
            smartDesc = "Scheduled preventive maintenance and health check.";
        }
        TextView tvDesc = findViewById(R.id.ipDescription);
        if (tvDesc != null) tvDesc.setText(smartDesc);

        TextView tvConcern = findViewById(R.id.ipConcern);
        if (tvConcern != null) tvConcern.setText(rawRemarks != null ? rawRemarks : "N/A");

        TextView tvRemarks = findViewById(R.id.ipRemarks);
        if (tvRemarks != null) tvRemarks.setText(rawRemarks != null ? rawRemarks : "N/A");

        StringBuilder itemsList = new StringBuilder();
        int totalQty = 0;
        for (DataSnapshot child : snapshot.getChildren()) {
            String key = child.getKey();
            if (key != null && key.startsWith("qty_")) {
                int q = 0;
                try {
                    q = child.getValue(Integer.class);
                } catch (Exception ignored) {}
                if (q > 0) {
                    itemsList.append(getItemName(key)).append(" x ").append(q).append("\n");
                    totalQty += q;
                }
            }
        }

        TextView tvItemName = findViewById(R.id.ipItemName);
        if (tvItemName != null) {
            tvItemName.setText(itemsList.length() > 0 ? itemsList.toString().trim() : "Standard Parts");
        }

        TextView tvItemQty = findViewById(R.id.ipItemQty);
        if (tvItemQty != null) {
            tvItemQty.setText(String.valueOf(totalQty));
        }

        String pm = snapshot.child("paymentMethod").getValue(String.class);
        String pStatus = snapshot.child("paymentStatus").getValue(String.class);
        boolean isPaid = "PAID".equalsIgnoreCase(pStatus);

        TextView payBadge = findViewById(R.id.ipPaymentStatus);
        TextView tvPayMethod = findViewById(R.id.ipPaymentMethod);
        if (tvPayMethod != null) {
            tvPayMethod.setText(pm != null ? pm : "COD");
        }

        if (payBadge != null) {
            if (isPaid) {
                payBadge.setText("PAID");
                payBadge.setBackgroundResource(R.drawable.bg_solid_green_pill);
                payBadge.setTextColor(Color.WHITE);
                TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.WHITE));
            } else if (pm != null && pm.equalsIgnoreCase("COD")) {
                payBadge.setText("PAYMENT DUE");
                payBadge.setBackgroundResource(R.drawable.bg_solid_amber_pill);
                payBadge.setTextColor(Color.WHITE);
                TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.WHITE));
            } else {
                payBadge.setText("FOR VERIFICATION");
                payBadge.setBackgroundResource(R.drawable.bg_solid_blue_pill);
                payBadge.setTextColor(Color.WHITE);
                TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.WHITE));
            }
        }

        View layoutIpRef = findViewById(R.id.layoutIpReference);
        if (pm != null && !pm.equalsIgnoreCase("COD")) {
            String pRef = snapshot.child("paymentReference").getValue(String.class);
            if (pRef != null && !pRef.isEmpty()) {
                if (layoutIpRef != null) layoutIpRef.setVisibility(View.VISIBLE);
                TextView tvRef = findViewById(R.id.ipPaymentRef);
                if (tvRef != null) tvRef.setText(pRef);
            } else {
                if (layoutIpRef != null) layoutIpRef.setVisibility(View.GONE);
            }
        } else {
            if (layoutIpRef != null) layoutIpRef.setVisibility(View.GONE);
        }

        String techName = snapshot.child("assignedTechName").getValue(String.class);
        TextView tvTechName = findViewById(R.id.ipAssignedTech);
        if (tvTechName != null) {
            tvTechName.setText(techName != null ? techName : "Assigning...");
        }

        String techPhone = snapshot.child("techContact").getValue(String.class);
        TextView tvTechContact = findViewById(R.id.ipTechContact);
        if (tvTechContact != null) {
            tvTechContact.setText(techPhone != null ? techPhone : "---");
        }

        TextView techStatusBadge = findViewById(R.id.ipTechStatus);
        if (techStatusBadge != null) {
            techStatusBadge.setText("ACCEPTED");
            techStatusBadge.setBackgroundResource(R.drawable.bg_solid_blue_pill);
            techStatusBadge.setTextColor(Color.WHITE);
            TextViewCompat.setCompoundDrawableTintList(techStatusBadge, ColorStateList.valueOf(Color.WHITE));
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
