package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int MODE_BOT = 1;
    public static final int MODE_TECH_CHAT = 2; // User is Technician, partner is Customer
    public static final int MODE_CUSTOMER_CHAT = 3; // User is Customer, partner is Technician

    private static final int VIEW_TYPE_MESSAGE = 100;
    private static final int VIEW_TYPE_SYSTEM = 101;

    private List<ChatMessage> messageList;
    private int currentMode;
    private String otherPartyName = "";
    private String currentUserId; // UID ng kasalukuyang user

    public ChatAdapter(List<ChatMessage> messageList, int mode) {
        this.messageList = messageList;
        this.currentMode = mode;

        // Kunin ang current user ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            this.currentUserId = "";
        }
    }

    public void setOtherPartyName(String name) {
        this.otherPartyName = name;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).isSystem()) return VIEW_TYPE_SYSTEM;
        return VIEW_TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SYSTEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_system, parent, false);
            return new SystemViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_techchat_message, parent, false);
            return new ChatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).tvSystemMessage.setText(Html.fromHtml(message.getText()));
        } else {
            ChatViewHolder chatHolder = (ChatViewHolder) holder;

            // ✅ Determine if message is from current user
            boolean isFromMe = message.getSenderId() != null && message.getSenderId().equals(currentUserId);

            // For AquaBuddy mode (special case)
            if (currentMode == MODE_BOT) {
                isFromMe = "user".equals(message.getSenderId());
            }

            if (message.getText() != null) {
                chatHolder.tvMessage.setText(Html.fromHtml(message.getText()));
            }
            chatHolder.tvMessage.setVisibility(View.VISIBLE);

            // DATE HEADER
            boolean showDateHeader = false;
            if (position == 0) showDateHeader = true;
            else {
                ChatMessage prevMsg = messageList.get(position - 1);
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                if (!fmt.format(new Date(message.getTimestamp())).equals(fmt.format(new Date(prevMsg.getTimestamp())))) {
                    showDateHeader = true;
                }
            }
            chatHolder.tvDateHeader.setVisibility(showDateHeader ? View.VISIBLE : View.GONE);
            if (showDateHeader) {
                SimpleDateFormat displayFmt = new SimpleDateFormat("MMMM dd", Locale.getDefault());
                chatHolder.tvDateHeader.setText(displayFmt.format(new Date(message.getTimestamp())));
            }

            // GROUPING LOGIC
            boolean isSameAsPrevious = false;
            boolean isSameAsNext = false;
            if (position > 0) {
                ChatMessage prev = messageList.get(position - 1);
                // Same sender and not system
                if (prev.getSenderId() != null && prev.getSenderId().equals(message.getSenderId()) && !prev.isSystem()) {
                    isSameAsPrevious = true;
                }
            }
            if (position < getItemCount() - 1) {
                ChatMessage next = messageList.get(position + 1);
                if (next.getSenderId() != null && next.getSenderId().equals(message.getSenderId()) && !next.isSystem()) {
                    isSameAsNext = true;
                }
            }
            if (showDateHeader) isSameAsPrevious = false;

            // AVATAR & LABEL ASSIGNMENT
            int avatarRes;
            String labelText;
            if (currentMode == MODE_BOT) {
                avatarRes = R.drawable.aquatech_iconbot;
                labelText = "AI Model";
            } else if (currentMode == MODE_TECH_CHAT) {
                // I am the Technician, so the OTHER person is the CUSTOMER
                avatarRes = R.drawable.man_customer_icon;
                labelText = (otherPartyName != null && !otherPartyName.isEmpty()) ? otherPartyName : "Customer";
            } else {
                // I am the Customer, so the OTHER person is the TECHNICIAN
                avatarRes = R.drawable.new_technician;
                labelText = (otherPartyName != null && !otherPartyName.isEmpty()) ? otherPartyName : "Technician";
            }

            // Show sender label only for the first message in a group from the other party
            if (!isFromMe && !isSameAsPrevious) {
                chatHolder.tvSenderLabel.setVisibility(View.VISIBLE);
                chatHolder.tvSenderLabel.setText(labelText);
            } else {
                chatHolder.tvSenderLabel.setVisibility(View.GONE);
            }

            // FILE / RECEIPT ATTACHMENT
            if (message.isFile() || message.isReceipt()) {
                chatHolder.fileAttachmentLayout.setVisibility(View.VISIBLE);
                chatHolder.tvFileName.setText(message.getFileName() != null ? message.getFileName() : "OfficialServiceForm.pdf");
                chatHolder.tvFileSize.setText(message.getFileSize() != null ? message.getFileSize() : "2 MB");
                chatHolder.tvMessage.setVisibility((message.getText() == null || message.getText().isEmpty()) ? View.GONE : View.VISIBLE);

                chatHolder.fileAttachmentLayout.setOnClickListener(v -> {
                    String ticketId = message.getTicketId();
                    if (ticketId == null && message.getFileName() != null) {
                        if (message.getFileName().contains("_")) {
                            String[] parts = message.getFileName().split("_");
                            ticketId = parts[parts.length - 1].replace(".pdf", "");
                        }
                    }

                    if (ticketId != null) {
                        Intent intent = new Intent(v.getContext(), ServiceReceiptActivity.class);
                        intent.putExtra("TICKET_ID", ticketId);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        v.getContext().startActivity(intent);
                    }
                });
            } else {
                chatHolder.fileAttachmentLayout.setVisibility(View.GONE);
            }

            // BUBBLE ALIGNMENT
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) chatHolder.messageContainer.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            params.removeRule(RelativeLayout.ALIGN_PARENT_START);
            params.removeRule(RelativeLayout.END_OF);

            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            chatHolder.tvTime.setText(sdfTime.format(new Date(message.getTimestamp())));
            chatHolder.tvTime.setVisibility(isSameAsNext ? View.GONE : View.VISIBLE);

            if (isFromMe) {
                // RIGHT SIDE (ME)
                chatHolder.tvMessage.setTextColor(Color.WHITE);
                chatHolder.tvTime.setTextColor(Color.WHITE);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(120, isSameAsPrevious ? 1 : 8, 16, isSameAsNext ? 1 : 8);
                chatHolder.techAvatarIcon.setVisibility(View.GONE);

                if (!isSameAsPrevious && !isSameAsNext) chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_user);
                else if (!isSameAsPrevious) chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_user_top);
                else if (!isSameAsNext) chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_user_bottom);
                else chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_user_middle);
            } else {
                // LEFT SIDE (THEM)
                chatHolder.tvMessage.setTextColor(Color.BLACK);
                chatHolder.tvTime.setTextColor(Color.BLACK);
                params.addRule(RelativeLayout.END_OF, R.id.techAvatarIcon);
                params.setMargins(8, isSameAsPrevious ? 1 : 8, 120, isSameAsNext ? 1 : 8);

                // AVATAR: Show only for the LAST message in a sequence (bottom of the group)
                if (!isSameAsNext) {
                    chatHolder.techAvatarIcon.setVisibility(View.VISIBLE);
                    Glide.with(chatHolder.itemView.getContext()).load(avatarRes).circleCrop().into(chatHolder.techAvatarIcon);
                } else {
                    chatHolder.techAvatarIcon.setVisibility(View.INVISIBLE);
                }

                // BUBBLE BACKGROUNDS: Correctly join consecutive messages
                if (!isSameAsNext) {
                    if (isSameAsPrevious) chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_ai_bottom);
                    else chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_ai);
                } else {
                    if (!isSameAsPrevious) chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_ai_top);
                    else chatHolder.messageContainer.setBackgroundResource(R.drawable.bubble_ai_middle);
                }
            }
            chatHolder.messageContainer.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvDateHeader, tvSenderLabel, tvFileName, tvFileSize;
        View messageContainer, fileAttachmentLayout;
        ImageView techAvatarIcon;
        ChatViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            tvDateHeader = v.findViewById(R.id.tvDateHeader);
            tvSenderLabel = v.findViewById(R.id.tvSenderLabel);
            messageContainer = v.findViewById(R.id.messageContainer);
            techAvatarIcon = v.findViewById(R.id.techAvatarIcon);
            fileAttachmentLayout = v.findViewById(R.id.fileAttachmentLayout);
            tvFileName = v.findViewById(R.id.tvFileName);
            tvFileSize = v.findViewById(R.id.tvFileSize);
        }
    }

    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvSystemMessage;
        SystemViewHolder(View v) {
            super(v);
            tvSystemMessage = v.findViewById(R.id.tvSystemMessage);
        }
    }
}