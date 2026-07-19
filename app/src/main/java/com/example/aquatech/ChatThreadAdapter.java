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
import com.google.firebase.database.DataSnapshot;
import java.util.List;

public class ChatThreadAdapter extends RecyclerView.Adapter<ChatThreadAdapter.ViewHolder> {

    private List<DataSnapshot> chatThreads;
    private String myId; 

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

        // 📍 DYNAMIC NAME & AVATAR DISPLAY LOGIC
        String displayName = "User";
        int avatarRes = R.drawable.man_customer_icon; // Default avatar
        
        if (myId != null) {
            if (myId.equals(customerId)) {
                // I am the CUSTOMER, show the TECHNICIAN'S name and technician avatar
                displayName = (techName != null && !techName.isEmpty()) ? techName : "Technician";
                avatarRes = R.drawable.new_technician;
            } else {
                // I am the TECHNICIAN, show the CUSTOMER'S name and customer avatar
                displayName = (customerName != null && !customerName.isEmpty()) ? customerName : "Customer";
                avatarRes = R.drawable.man_customer_icon;
            }
        }
        
        holder.tvCustomerName.setText(displayName);
        holder.ivAvatar.setImageResource(avatarRes);

        if (lastMsg != null) {
            // Show "You: " prefix if I sent the last message
            if (lastSenderId != null && lastSenderId.equals(myId)) {
                holder.tvLastMsg.setText(Html.fromHtml("<b>You:</b> " + lastMsg));
            } else {
                holder.tvLastMsg.setText(Html.fromHtml(lastMsg));
            }
        } else {
            holder.tvLastMsg.setText("No messages yet");
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
        return chatThreads.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvLastMsg;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.msgName);
            tvLastMsg = itemView.findViewById(R.id.msgText);
            ivAvatar = itemView.findViewById(R.id.msgAvatar);
        }
    }
}
