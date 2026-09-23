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
        TextView tvTicketId = findViewById(R.id.compTicketId);
        if (tvTicketId != null) tvTicketId.setText(sro != null ? sro : ticketId);

        Long completionTs = snapshot.child("completionTimestamp").getValue(Long.class);
        if (completionTs == null) completionTs = snapshot.child("submissionTimestamp").getValue(Long.class);
        
        if (completionTs != null) {
            String dateStr = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(completionTs));
            String timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(completionTs));
            TextView tvSubDate = findViewById(R.id.compSubDate);
            if (tvSubDate != null) tvSubDate.setText(dateStr);
            TextView tvSubTime = findViewById(R.id.compSubTime);
            if (tvSubTime != null) tvSubTime.setText(timeStr);
        }

        TextView statusBadge = findViewById(R.id.compStatusBadge);
        if (statusBadge != null) {
            statusBadge.setText("COMPLETED");
            statusBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
            statusBadge.setTextColor(Color.parseColor("#166534"));
            TextViewCompat.setCompoundDrawableTintList(statusBadge, ColorStateList.valueOf(Color.parseColor("#166534")));
        }

        TextView tvCustName = findViewById(R.id.compCustomerName);
        if (tvCustName != null) tvCustName.setText(snapshot.child("customerName").getValue(String.class));

        TextView tvContact = findViewById(R.id.compContact);
        if (tvContact != null) tvContact.setText(snapshot.child("contactNumber").getValue(String.class));

        TextView tvAddress = findViewById(R.id.compAddress);
        if (tvAddress != null) tvAddress.setText(snapshot.child("address").getValue(String.class));

        String unitModel = snapshot.child("unitName").getValue(String.class);
        if (unitModel == null) unitModel = snapshot.child("unitModel").getValue(String.class);
        if (unitModel == null) unitModel = "";

        String unitNumber = snapshot.child("unitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = snapshot.child("itemUnitNumber").getValue(String.class);
        if (unitNumber == null) unitNumber = "0000";

        String friendlyProductName = "STANDING WATER PURIFIER";
        String baseUnitTemplate = "ST-FXCU1-M-HCA-WT-**-***";

        if (unitModel.toUpperCase().contains("CUBE")) {
            friendlyProductName = "WL CUBE FIREWALL";
            baseUnitTemplate = "F-FXCU1-M-HCA-TT-K1-**-***";
        } else if (unitModel.toUpperCase().contains("SLIM")) {
            friendlyProductName = "SMART SLIM";
            baseUnitTemplate = "S-FXCU1-M-HCA-AA-B2-**-***";
        } else if (unitModel.toUpperCase().contains("COUNTER")) {
            friendlyProductName = "COUNTER TOP WATER PURIFIER";
            baseUnitTemplate = "CT-FXCU1-M-HCA-WT-**-***";
        } else if (unitModel.toUpperCase().contains("STANDING") || unitModel.toUpperCase().contains("ST")) {
            friendlyProductName = "STANDING WATER PURIFIER";
            baseUnitTemplate = "ST-FXCU1-M-HCA-WT-**-***";
        }

        String finalUnitCode = baseUnitTemplate.replace("**-***", unitNumber);

        TextView tvUnitName = findViewById(R.id.compUnitName);
        if (tvUnitName != null) tvUnitName.setText(friendlyProductName);

        TextView tvUnitCode = findViewById(R.id.compUnitCode);
        if (tvUnitCode != null) tvUnitCode.setText(finalUnitCode);

        String purchaseType = snapshot.child("purchaseType").getValue(String.class);
        if (purchaseType == null) purchaseType = snapshot.child("typeOfPurchase").getValue(String.class);
        if (purchaseType == null) purchaseType = snapshot.child("installationLocation").getValue(String.class);

        if (purchaseType != null) {
            String upper = purchaseType.toUpperCase();
            if (upper.contains("OCULAR") || upper.contains("OCUL")) {
                purchaseType = "OCULAR";
            } else if (upper.contains("OUTRIGHT") || upper.contains("BUY") || upper.contains("PURCHASE")) {
                purchaseType = "OUTRIGHT";
            } else if (upper.contains("SUB") || upper.contains("RENT")) {
                purchaseType = "SUBSCRIPTION";
            }
        }

        TextView tvLocation = findViewById(R.id.compLocation);
        if (tvLocation != null) tvLocation.setText(purchaseType != null ? purchaseType : "SUBSCRIPTION");

        String serviceType = snapshot.child("serviceType").getValue(String.class);
        TextView tvServiceType = findViewById(R.id.compServiceType);
        if (tvServiceType != null) tvServiceType.setText(serviceType != null ? serviceType : "General Service");
        
        String techRemarks = snapshot.child("technicianRemarks").getValue(String.class);
        TextView tvRemarks = findViewById(R.id.compRemarks);
        if (tvRemarks != null) tvRemarks.setText(techRemarks != null ? techRemarks : "Service completed successfully.");

        String pm = snapshot.child("paymentMethod").getValue(String.class);
        TextView tvPaymentMethod = findViewById(R.id.compPaymentMethod);
        if (tvPaymentMethod != null) tvPaymentMethod.setText(pm != null ? pm : "COD");

        Object total = snapshot.child("totalAmount").getValue();
        TextView tvTotal = findViewById(R.id.compTotalAmount);
        if (tvTotal != null && total instanceof Number) {
            tvTotal.setText(String.format(Locale.getDefault(), "₱ %,.2f", ((Number)total).doubleValue()));
        }

        TextView payBadge = findViewById(R.id.compPaymentBadge);
        if (payBadge != null) {
            payBadge.setText("PAID");
            payBadge.setBackgroundResource(R.drawable.bg_pdf_status_green_pill);
            payBadge.setTextColor(Color.parseColor("#166534"));
            TextViewCompat.setCompoundDrawableTintList(payBadge, ColorStateList.valueOf(Color.parseColor("#166534")));
        }

        ImageView ivTechSign = findViewById(R.id.compTechSign);

        String techSig = snapshot.child("technicianSignature").getValue(String.class);
        if (ivTechSign != null && techSig != null && !techSig.isEmpty()) {
            int resId = getResources().getIdentifier(techSig, "drawable", getPackageName());
            if (resId != 0) {
                ivTechSign.setImageResource(resId);
            } else {
                ivTechSign.setImageResource(R.drawable.signature1_png);
            }
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