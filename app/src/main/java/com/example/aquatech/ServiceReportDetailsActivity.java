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

import java.util.ArrayList;
import java.util.List;

public class ServiceReportDetailsActivity extends AppCompatActivity {

    private ImageView ivProof;
    private TextView tvTicketID, tvCustomerName, tvCustomerPhone, tvUnitSro, tvAddress, tvTechName, tvServiceType, tvRemarks, tvStatus;
    private RatingBar rbRating;
    private String ticketId;
    private DatabaseReference dbRef;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_report_details);

        setupStatusBar();
        initializeViews();

        ticketId = getIntent().getStringExtra("TICKET_ID");
        if (ticketId == null) {
            Toast.makeText(this, "Error: Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvTicketID.setText("#" + ticketId);
        setupFirebase();
    }

    private void initializeViews() {
        ivProof = findViewById(R.id.ivProofOfService);
        tvTicketID = findViewById(R.id.tvTicketIDDetails);
        tvCustomerName = findViewById(R.id.tvCustomerNameDetails);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhoneDetails);
        tvUnitSro = findViewById(R.id.tvUnitSroDetails);
        tvAddress = findViewById(R.id.tvAddressDetails);
        tvTechName = findViewById(R.id.tvTechNameDetails);
        tvServiceType = findViewById(R.id.tvServiceTypeDetails);
        tvRemarks = findViewById(R.id.tvTechRemarks);
        rbRating = findViewById(R.id.rbCustomerRating);
        
        // Find status text from the layout (hardcoded in XML, but we'll bind it)
        // Since it's inside a nested layout, we find it if it has an ID, or just find by tag
        tvStatus = findViewById(R.id.tvHeaderTitle); 

        findViewById(R.id.btnBackDetails).setOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        dbRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // 📍 CUSTOMER DETAILS
                    String customer = snapshot.child("customerName").getValue(String.class);
                    String phone = snapshot.child("customerPhone").getValue(String.class);
                    if (phone == null) phone = snapshot.child("userPhone").getValue(String.class);
                    
                    String unitSro = snapshot.child("referenceNo").getValue(String.class);
                    if (unitSro == null) unitSro = snapshot.child("sroNumber").getValue(String.class);
                    
                    String address = snapshot.child("address").getValue(String.class);
                    
                    // 📍 TECHNICIAN INFO
                    String tech = snapshot.child("assignedTechName").getValue(String.class);
                    String remarks = snapshot.child("technicianRemarks").getValue(String.class);
                    String imageUrl = snapshot.child("proofImageUrl").getValue(String.class);
                    
                    // 📍 SERVICE TYPE (Multiple logic)
                    String service = getServiceTypeLabel(snapshot);
                    
                    // 📍 RATING
                    Float rating = snapshot.child("rating").getValue(Float.class);

                    // BIND TO UI
                    tvCustomerName.setText(customer != null ? customer : "N/A");
                    tvCustomerPhone.setText(phone != null ? phone : "Not Provided");
                    tvUnitSro.setText(unitSro != null ? unitSro : "N/A");
                    tvAddress.setText(address != null ? address : "N/A");
                    tvTechName.setText(tech != null ? tech : "Aqua Technician");
                    tvServiceType.setText(service);
                    tvRemarks.setText((remarks != null && !remarks.isEmpty()) ? remarks : "No technician remarks provided.");
                    
                    if (rating != null) rbRating.setRating(rating);
                    else rbRating.setRating(0f);

                    // 📍 PROOF OF VISIT IMAGE
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        ivProof.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 
                                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
                        Glide.with(ServiceReportDetailsActivity.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.img_place_holder)
                                .centerCrop()
                                .into(ivProof);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ServiceReportDetailsActivity.this, "Database Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getServiceTypeLabel(DataSnapshot ds) {
        List<String> types = new ArrayList<>();
        if (hasQty(ds, "qty_wayvalve")) types.add("Installation Kit");
        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) types.add("Filter Preventive");
        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp") || hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) types.add("Other Parts");

        if (types.isEmpty()) return "General Maintenance";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            sb.append(types.get(i));
            if (i < types.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
