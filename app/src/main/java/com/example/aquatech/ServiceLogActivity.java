package com.example.aquatech;


import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceLogActivity extends AppCompatActivity {

    private TextView SroNumber, tvTechName, tvDateTime;
    private RecyclerView rvOrders;
    private ImageView greaterThanIcon, btnBack, techProfileAvatar;
    private CardView mapTrackCard;

    private String assignedTechId = null;
    private String currentUid = null;
    private String currentStatus = null;
    private String currentTicketId = null;
    private boolean hasRated = false;
    private List<OrderItem> orderList = new ArrayList<>();
    private OrderAdapter orderAdapter;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_log);

        currentUid = FirebaseAuth.getInstance().getUid();
        setupStatusBar();
        initializeViews();
        setupFirebaseRequestListener();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        SroNumber = findViewById(R.id.SroNumber);
        tvTechName = findViewById(R.id.tvTechName);
        tvDateTime = findViewById(R.id.tvDateTime);
        greaterThanIcon = findViewById(R.id.greaterThanIcon);
        techProfileAvatar = findViewById(R.id.techProfileAvatar);
        mapTrackCard = findViewById(R.id.mapTrackCard);
        rvOrders = findViewById(R.id.rvOrders);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(orderList);
        rvOrders.setAdapter(orderAdapter);

        if (tvTechName != null) tvTechName.setText("Loading...");
        if (SroNumber != null) SroNumber.setText("Fetching SRO...");
    }

    private void setupFirebaseRequestListener() {
        if (currentUid == null) return;

        DatabaseReference requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        Query query = requestsRef.orderByChild("userId").equalTo(currentUid).limitToLast(1);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        displayRequestData(ds);
                    }
                } else {
                    if (tvTechName != null) tvTechName.setText("No active request");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SERVICE_LOG", "Error: " + error.getMessage());
            }
        });
    }

    private void displayRequestData(DataSnapshot ds) {
        currentTicketId = ds.getKey();
        String sro = ds.child("referenceNo").getValue(String.class);
        if (sro == null) sro = ds.child("sroNumber").getValue(String.class);
        if (sro == null) sro = ds.child("refNo").getValue(String.class);

        if (SroNumber != null && sro != null) {
            SroNumber.setText(sro);
        }

        currentStatus = ds.child("status").getValue(String.class);
        assignedTechId = ds.child("assignedTechId").getValue(String.class);
        String assignedTechName = ds.child("assignedTechName").getValue(String.class);
        String avatarUrl = ds.child("techAvatarUrl").getValue(String.class);

        // 📍 ACCESS CONTROL: If finished, hide tech info
        boolean isFinished = "Completed".equalsIgnoreCase(currentStatus) || 
                            "Submission".equalsIgnoreCase(currentStatus) ||
                            "Cancelled".equalsIgnoreCase(currentStatus);

        if (tvTechName != null) {
            if (isFinished) {
                tvTechName.setText("Service Finished");
            } else {
                tvTechName.setText((assignedTechName != null && !assignedTechName.isEmpty()) ? assignedTechName : "Waiting for technician...");
            }
        }

        if (techProfileAvatar != null) {
            if (isFinished) {
                techProfileAvatar.setImageResource(R.drawable.new_technician); // Generic placeholder
            } else if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(ServiceLogActivity.this).load(avatarUrl).placeholder(R.drawable.new_technician).circleCrop().into(techProfileAvatar);
            }
        }

        // 📍 RATING DIALOG: Show if completed and not yet rated
        if ("Completed".equalsIgnoreCase(currentStatus)) {
            Boolean rated = ds.child("rated").getValue(Boolean.class);
            if (rated == null || !rated) {
                showRatingDialog(currentTicketId, assignedTechName, assignedTechId);
            }
        }

        String dateStr = ds.child("date").getValue(String.class);
        String timeRange = ds.child("timeRange").getValue(String.class);
        if (tvDateTime != null) {
            String display = (dateStr != null ? dateStr : "") + (timeRange != null ? " | " + timeRange : "");
            tvDateTime.setText(display.isEmpty() ? "Not scheduled" : display);
        }

        updateOrdersList(ds);
        
        if (mapTrackCard != null) {
            boolean canTrack = assignedTechId != null && !isFinished;
            mapTrackCard.setAlpha(canTrack ? 1.0f : 0.4f);
        }
    }

    private void showRatingDialog(String ticketId, String techName, String techId) {
        if (hasRated || isFinishing()) return;
        hasRated = true;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rate_technician);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivTechProfile = dialog.findViewById(R.id.ivTechProfile);
        TextView tvName = dialog.findViewById(R.id.tvTechName);
        RatingBar ratingBar = dialog.findViewById(R.id.ratingBar);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitRating);

        tvName.setText(techName != null ? techName : "Technician");

        if (techId != null) {
            FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(techId)
                .child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String url = snapshot.getValue(String.class);
                        if (url != null && !url.isEmpty() && !isFinishing()) {
                            Glide.with(ServiceLogActivity.this).load(url).circleCrop().into(ivTechProfile);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
        }

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating < 1) {
                Toast.makeText(this, "Please rate at least 1 star", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> update = new HashMap<>();
            update.put("rating", rating);
            update.put("rated", true);

            FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests").child(ticketId)
                .updateChildren(update).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
        });

        dialog.setOnDismissListener(d -> hasRated = false);
        dialog.show();
    }

    private void updateOrdersList(DataSnapshot ds) {
        orderList.clear();
        for (DataSnapshot child : ds.getChildren()) {
            String key = child.getKey();
            if (key != null && key.startsWith("qty_")) {
                Object val = child.getValue();
                int qty = 0;
                if (val instanceof Number) qty = ((Number) val).intValue();

                if (qty > 0) {
                    orderList.add(new OrderItem(
                            getItemNameFromKey(key),
                            getItemSubFromKey(key),
                            qty,
                            getItemImageFromKey(key)
                    ));
                }
            }
        }
        orderAdapter.notifyDataSetChanged();
    }

    private String getItemNameFromKey(String key) {
        switch (key) {
            case "qty_cbc": return "Filter 0064-CBC";
            case "qty_sediment": return "Filter 0055-SEDIMENT";
            case "qty_uvlamp": return "UV Lamp";
            case "qty_aquatal": return "Aquatal Filter";
            case "qty_inline": return "Inline Filter";
            case "qty_touchpanel": return "Touch Panel";
            case "qty_pbcboard": return "PBC Board";
            case "qty_smsf1": return "SMSF 1µ CBC";
            case "qty_smsf10": return "SMSF 10µ SED";
            case "qty_wayvalve": return "3-Way Valve";
            default: return "Service Item";
        }
    }

    private String getItemSubFromKey(String key) {
        if (key.equals("qty_wayvalve")) return "Installation Kit";
        if (key.contains("smsf")) return "Filter Part";
        return "Replacement Part";
    }

    private int getItemImageFromKey(String key) {
        switch (key) {
            case "qty_cbc": return R.drawable.cbc0064_carbon_black_filter;
            case "qty_sediment": return R.drawable.sediment0055_sediment_filter;
            case "qty_uvlamp": return R.drawable.uvlamp_png;
            case "qty_aquatal": return R.drawable.uvlamp_png;
            case "qty_inline": return R.drawable.sediment0055_sediment_filter;
            case "qty_touchpanel": return R.drawable.sediment0055_sediment_filter;
            case "qty_pbcboard": return R.drawable.sediment0055_sediment_filter;
            case "qty_smsf1": return R.drawable.cbc0064_carbon_black_filter;
            case "qty_smsf10": return R.drawable.cbc0064_carbon_black_filter;
            case "qty_wayvalve": return R.drawable.uvlamp_png;
            default: return R.drawable.cbc0064_carbon_black_filter;
        }
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
        private List<OrderItem> items;
        OrderAdapter(List<OrderItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrderItem item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvSub.setText(item.subName);
            holder.tvQty.setText("x" + item.quantity);
            holder.ivImage.setImageResource(item.imgRes);
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSub, tvQty;
            ImageView ivImage;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvItemName);
                tvSub = v.findViewById(R.id.tvItemSub);
                tvQty = v.findViewById(R.id.tvItemQty);
                ivImage = v.findViewById(R.id.ivItemImage);
            }
        }
    }

    private static class OrderItem {
        String name, subName;
        int quantity, imgRes;
        OrderItem(String n, String s, int q, int img) {
            this.name = n; this.subName = s; this.quantity = q; this.imgRes = img;
        }
    }

    private void setupListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        View.OnClickListener trackClick = v -> {
            if (assignedTechId == null) {
                Toast.makeText(this, "Technician not yet assigned.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            boolean isFinished = "Completed".equalsIgnoreCase(currentStatus) || 
                                "Submission".equalsIgnoreCase(currentStatus) ||
                                "Cancelled".equalsIgnoreCase(currentStatus);

            if (isFinished) {
                Toast.makeText(this, "Ticket finished. Tracking is no longer available.", Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(this, TrackServiceActivity.class);
            intent.putExtra("TECH_ID", assignedTechId);
            startActivity(intent);
        };
        if (mapTrackCard != null) mapTrackCard.setOnClickListener(trackClick);
        if (greaterThanIcon != null) greaterThanIcon.setOnClickListener(trackClick);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
