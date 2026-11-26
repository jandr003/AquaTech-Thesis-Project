package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListCustomerActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private View emptyView;
    private CustomerChatAdapter adapter;
    private final List<CustomerChatThread> chatList = new ArrayList<>();
    private String currentUid;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list_customer);

        currentUid = FirebaseAuth.getInstance().getUid();
        initializeViews();
        CustomerDashboardNavHelper.setupBottomNavigation(this);
        loadCustomerTechnicianChats();
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackCustChat);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvChats = findViewById(R.id.rvCustomerChatThreads);
        emptyView = findViewById(R.id.emptyCustomerChatContainer);

        if (rvChats != null) {
            rvChats.setLayoutManager(new LinearLayoutManager(this));
            adapter = new CustomerChatAdapter(chatList);
            rvChats.setAdapter(adapter);
        }
    }

    private void loadCustomerTechnicianChats() {
        if (currentUid == null) {
            return;
        }

        DatabaseReference requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        requestsRef.orderByChild("userId").equalTo(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                chatList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String techId = ds.child("assignedTechId").getValue(String.class);
                        String techName = ds.child("assignedTechName").getValue(String.class);
                        String ticketId = ds.child("referenceNo").getValue(String.class);
                        if (ticketId == null) {
                            ticketId = ds.getKey();
                        }

                        String unitModel = ds.child("unitModel").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);

                        if (techId != null && !techId.isEmpty()) {
                            CustomerChatThread thread = new CustomerChatThread(
                                    techId,
                                    techName != null && !techName.isEmpty() ? techName : "Assigned Technician",
                                    ticketId != null ? ticketId : "Ticket Request",
                                    unitModel != null ? unitModel : "Water Purifier",
                                    status != null ? status : "Active"
                            );
                            if (!containsTech(techId)) {
                                chatList.add(thread);
                            }
                        }
                    }
                }

                if (emptyView != null) {
                    emptyView.setVisibility(chatList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatListCustomerActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean containsTech(String techId) {
        for (CustomerChatThread item : chatList) {
            if (item.techId != null && item.techId.equals(techId)) {
                return true;
            }
        }
        return false;
    }

    private class CustomerChatAdapter extends RecyclerView.Adapter<CustomerChatAdapter.ViewHolder> {
        private final List<CustomerChatThread> items;

        CustomerChatAdapter(List<CustomerChatThread> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_thread, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (items == null || position >= items.size()) {
                return;
            }

            CustomerChatThread item = items.get(position);
            if (item == null) {
                return;
            }

            if (holder.tvName != null) {
                holder.tvName.setText(item.techName);
            }

            if (holder.tvSub != null) {
                String cleanSro = item.ticketCode.startsWith("SRO-") ? item.ticketCode.substring(4) : item.ticketCode;
                holder.tvSub.setText(cleanSro + " • " + item.unitModel);
            }

            if (holder.ivAvatar != null) {
                int avatarRes = getTechAvatarDrawable(item.techId);
                holder.ivAvatar.setImageResource(avatarRes);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ChatListCustomerActivity.this, CustomerChatActivity.class);
                intent.putExtra("TECH_ID", item.techId);
                intent.putExtra("TECH_NAME", item.techName);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSub;
            ImageView ivAvatar;

            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.msgName);
                tvSub = v.findViewById(R.id.msgText);
                ivAvatar = v.findViewById(R.id.msgAvatar);
            }
        }
    }

    private int getTechAvatarDrawable(String techId) {
        if (techId != null) {
            int hash = Math.abs(techId.hashCode()) % 8;
            switch (hash) {
                case 1: return R.drawable.technician_avatar2;
                case 2: return R.drawable.technician_avatar3;
                case 3: return R.drawable.technician_avatar4;
                case 4: return R.drawable.technician_avatar5;
                case 5: return R.drawable.technician_avatar6;
                case 6: return R.drawable.technician_avatar7;
                case 7: return R.drawable.technician_avatar8;
                default: return R.drawable.technician_avatar1;
            }
        }
        return R.drawable.technician_avatar1;
    }

    private static class CustomerChatThread {
        String techId, techName, ticketCode, unitModel, status;

        CustomerChatThread(String id, String name, String code, String unit, String st) {
            this.techId = id;
            this.techName = name;
            this.ticketCode = code;
            this.unitModel = unit;
            this.status = st;
        }
    }
}
