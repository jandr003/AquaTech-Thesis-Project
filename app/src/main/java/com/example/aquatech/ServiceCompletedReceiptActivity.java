package com.example.aquatech;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServiceCompletedReceiptActivity extends AppCompatActivity {

    private String ticketId;
    private DatabaseReference requestRef;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_completed_receipt_template);

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) {
            Toast.makeText(this, "Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupStatusBar();
        initializeDataSync();
        findViewById(R.id.compLogoImg).setOnClickListener(v -> finish());
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
        ((TextView)findViewById(R.id.compTicketId)).setText(sro != null ? sro : ticketId);

        Long completionTs = snapshot.child("completionTimestamp").getValue(Long.class);
        if (completionTs == null) completionTs = snapshot.child("submissionTimestamp").getValue(Long.class);
        
        if (completionTs != null) {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(completionTs));
            String timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(completionTs));
            ((TextView)findViewById(R.id.compServiceDate)).setText(dateStr);
            ((TextView)findViewById(R.id.compServiceTime)).setText(timeStr);
        }

        TextView statusBadge = findViewById(R.id.compStatusBadge);
        statusBadge.setText("COMPLETED");
        statusBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
        statusBadge.setTextColor(Color.parseColor("#166534"));
        TextViewCompat.setCompoundDrawableTintList(statusBadge, ColorStateList.valueOf(Color.parseColor("#166534")));

        ((TextView)findViewById(R.id.compCustomerName)).setText(snapshot.child("customerName").getValue(String.class));
        ((TextView)findViewById(R.id.compContact)).setText(snapshot.child("contactNumber").getValue(String.class));
        ((TextView)findViewById(R.id.compAddress)).setText(snapshot.child("address").getValue(String.class));

        String serviceType = snapshot.child("serviceType").getValue(String.class);
        ((TextView)findViewById(R.id.compServiceDesc)).setText(serviceType != null ? serviceType : "General Service");
        
        String techRemarks = snapshot.child("technicianRemarks").getValue(String.class);
        ((TextView)findViewById(R.id.compTechRemarks)).setText(techRemarks != null ? techRemarks : "Service completed successfully.");

        LinearLayout ordersContainer = findViewById(R.id.compOrdersContainer);
        ordersContainer.removeAllViews();
        addOrderRow(snapshot, ordersContainer, "qty_wayvalve", "Installation Kit (3-Way Valve)", 350);
        addOrderRow(snapshot, ordersContainer, "qty_cbc", "Filter 0064-CBC", 2000);
        addOrderRow(snapshot, ordersContainer, "qty_sediment", "Filter 0055-SEDIMENT", 1000);
        addOrderRow(snapshot, ordersContainer, "qty_aquatal", "Aquatal Replacement", 2000);
        addOrderRow(snapshot, ordersContainer, "qty_inline", "Inline Filter", 3000);
        addOrderRow(snapshot, ordersContainer, "qty_uvlamp", "UV Lamp", 1500);
        addOrderRow(snapshot, ordersContainer, "qty_touchpanel", "Touch Panel", 750);
        addOrderRow(snapshot, ordersContainer, "qty_pbcboard", "PBC Board", 3000);
        addOrderRow(snapshot, ordersContainer, "qty_smsf1", "SMSF 1u CBC", 2000);
        addOrderRow(snapshot, ordersContainer, "qty_smsf10", "SMSF 10u SED", 1000);

        String pm = snapshot.child("paymentMethod").getValue(String.class);
        ((TextView)findViewById(R.id.compPaymentMethod)).setText(pm != null ? pm : "COD");

        Object total = snapshot.child("totalAmount").getValue();
        if (total instanceof Number) {
            ((TextView)findViewById(R.id.compTotalAmount)).setText(String.format(Locale.getDefault(), "₱ %,.2f", ((Number)total).doubleValue()));
        }

        String collector = snapshot.child("paymentCollectedBy").getValue(String.class);
        if (collector == null) collector = snapshot.child("assignedTechName").getValue(String.class);
        ((TextView)findViewById(R.id.compCollectedBy)).setText(collector != null ? collector : "AquaTech Personnel");

        TextView payBadge = findViewById(R.id.compPaymentBadge);
        payBadge.setText("PAID");
        payBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
        payBadge.setTextColor(Color.parseColor("#166534"));
        TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.parseColor("#166534")));

        ImageView ivTechSign = findViewById(R.id.compTechSign);
        ImageView ivCustSign = findViewById(R.id.compCustSign);

        String techSig = snapshot.child("technicianSignature").getValue(String.class);
        if (techSig != null && !techSig.isEmpty()) {
            int resId = getResources().getIdentifier(techSig, "drawable", getPackageName());
            if (resId != 0) {
                ivTechSign.setImageResource(resId);
            } else {
                ivTechSign.setImageResource(R.drawable.signature1_png);
            }
        }

        String custSigUrl = snapshot.child("proofImageUrl").getValue(String.class);
        if (custSigUrl != null && !custSigUrl.isEmpty()) {
            Glide.with(this).load(custSigUrl).into(ivCustSign);
        }
    }

    private void addOrderRow(DataSnapshot snapshot, LinearLayout container, String key, String label, int price) {
        Object val = snapshot.child(key).getValue();
        int q = 0;
        if (val instanceof Number) q = ((Number) val).intValue();
        if (q > 0) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_pdf_billing_row, container, false);
            ((TextView)row.findViewById(R.id.pdfItemName)).setText(label);
            ((TextView)row.findViewById(R.id.pdfItemQty)).setText(String.valueOf(q));
            ((TextView)row.findViewById(R.id.pdfItemPrice)).setText(String.format(Locale.getDefault(), "₱ %,d.00", q * price));
            container.addView(row);
        }
    }
}