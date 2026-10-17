package com.example.aquatech;

import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ViewHolder> {

    private List<DataSnapshot> chatThreads;
    private String myId;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    public ChatThreadAdapter(List<DataSnapshot> chatThreads, String myId) {
        this.chatThreads = chatThreads;
        this.myId = myId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_thread, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataSnapshot ds = chatThreads.get(position);

        String techId = ds.child("techId").getValue(String.class);
        String customerId = ds.child("customerId").getValue(String.class);
        String techName = ds.child("techName").getValue(String.class);
        String customerName = ds.child("customerName").getValue(String.class);
        String lastMsg = ds.child("lastMessage").getValue(String.class);
        String lastSenderId = ds.child("lastSenderId").getValue(String.class);

        String otherUserId = (myId != null && myId.equals(customerId)) ? techId : customerId;
        String fallbackName = (myId != null && myId.equals(customerId)) ?
                ((techName != null && !techName.isEmpty()) ? techName : "Technician") :
                ((customerName != null && !customerName.isEmpty()) ? customerName : "Customer");

        holder.tvCustomerName.setText(fallbackName);
        holder.ivAvatar.setImageResource((myId != null && myId.equals(customerId)) ? R.drawable.new_technician : R.drawable.customer_avatar1);

        if (otherUserId != null && !otherUserId.isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(otherUserId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String realName = snapshot.child("fullName").getValue(String.class);
                        if (realName == null) realName = snapshot.child("name").getValue(String.class);
                        if (realName != null && !realName.trim().isEmpty()) {
                            holder.tvCustomerName.setText(realName.trim());
                        }

                        String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        Integer resId = snapshot.child("avatarResId").getValue(Integer.class);

                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(imgUrl)
                                    .placeholder(R.drawable.customer_avatar1)
                                    .circleCrop()
                                    .into(holder.ivAvatar);
                        } else if (resId != null && resId > 0) {
                            holder.ivAvatar.setImageResource(resId);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        if (lastMsg != null) {
            if (lastSenderId != null && lastSenderId.equals(myId)) {
                holder.tvLastMsg.setText(Html.fromHtml("<b>You:</b> " + lastMsg, Html.FROM_HTML_MODE_LEGACY));
            } else {
                holder.tvLastMsg.setText(Html.fromHtml(lastMsg, Html.FROM_HTML_MODE_LEGACY));
            }
        } else {
            holder.tvLastMsg.setText("No messages yet");
        }

        Long ts = ds.child("timestamp").getValue(Long.class);
        if (holder.tvTime != null) {
            if (ts != null && ts > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                holder.tvTime.setText(sdf.format(new Date(ts)));
            } else {
                holder.tvTime.setText("---");
            }
        }

        final String finalTechId = techId;
        final String finalCustId = customerId;
        final String finalTechName = techName;
        final String finalCustName = customerName;

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CustomerChatActivity.class);
            intent.putExtra("TECH_ID", finalTechId);
            intent.putExtra("TECH_NAME", finalTechName);
            intent.putExtra("CUSTOMER_ID", finalCustId);
            intent.putExtra("CUSTOMER_NAME", finalCustName);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatThreads != null ? chatThreads.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvLastMsg, tvTime;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.msgName);
            tvLastMsg = itemView.findViewById(R.id.msgText);
            tvTime = itemView.findViewById(R.id.msgTime);
            ivAvatar = itemView.findViewById(R.id.msgAvatar);
        }
    }
}
