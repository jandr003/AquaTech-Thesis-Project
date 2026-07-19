package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageView btnBack, btnCall, headerAvatar;
    private TextView tvOtherPartyName;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private DatabaseReference chatRef, notifRef, metaRef, serviceRequestRef;
    private String customerId, techId, otherPartyName, myId;
    private ChildEventListener chatListener, notifListener;
    private ValueEventListener serviceRequestListener;
    private boolean isInitialNotifLoad = true;
    private boolean isChatFunctionalityEnabled = false;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_customer_chat);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(0, 0, 0, keyboardHeight);
            return insets;
        });

        String authUid = FirebaseAuth.getInstance().getUid();
        String intentCustId = getIntent().getStringExtra("CUSTOMER_ID");
        String intentTechId = getIntent().getStringExtra("TECH_ID");
        String intentCustName = getIntent().getStringExtra("CUSTOMER_NAME");
        String intentTechName = getIntent().getStringExtra("TECH_NAME");

        if (authUid != null) {
            myId = authUid;
            if (authUid.equals(intentTechId)) {
                techId = authUid;
                customerId = intentCustId;
                otherPartyName = intentCustName != null ? intentCustName : "Customer";
            } else {
                customerId = authUid;
                techId = intentTechId;
                otherPartyName = intentTechName != null ? intentTechName : "Technician";
            }
        } else {
            myId = intentTechId;
            techId = intentTechId;
            customerId = intentCustId;
            otherPartyName = intentCustName != null ? intentCustName : "Customer";
        }

        if (customerId == null && authUid != null) customerId = authUid;

        initializeViews();
        setupChat();

        if (customerId != null && techId != null && !customerId.isEmpty() && !techId.isEmpty()) {
            setupFirebaseChatListener();
            setupNotificationMonitor();
            fetchMetaData();
            fetchOtherPartyRealName(); // 🛠️ Fetch real name from Users node
            markAsRead();
        } else {
            Toast.makeText(this, "Connection error. Please try again.", Toast.LENGTH_LONG).show();
        }

        setupClickListeners();
        updateChatFunctionality(false);
    }

    private void fetchOtherPartyRealName() {
        String otherId = myId.equals(techId) ? customerId : techId;
        if (otherId == null) return;

        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(otherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null) name = snapshot.child("name").getValue(String.class);
                    
                    if (name != null && !name.isEmpty()) {
                        otherPartyName = name;
                        if (tvOtherPartyName != null) tvOtherPartyName.setText(otherPartyName);
                        if (chatAdapter != null) chatAdapter.setOtherPartyName(otherPartyName);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchMetaData() {
        String chatId = techId + "_" + customerId;
        metaRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        metaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String tName = snapshot.child("techName").getValue(String.class);
                    String cName = snapshot.child("customerName").getValue(String.class);

                    if (myId != null && myId.equals(techId)) {
                        if (cName != null) {
                            otherPartyName = cName;
                            tvOtherPartyName.setText(otherPartyName);
                            chatAdapter.setOtherPartyName(otherPartyName);
                        }
                    } else {
                        if (tName != null && !tName.equalsIgnoreCase("Technician")) {
                            otherPartyName = tName;
                            tvOtherPartyName.setText(otherPartyName);
                            chatAdapter.setOtherPartyName(otherPartyName);
                        }
                    }

                    String ticketId = snapshot.child("ticketId").getValue(String.class);
                    if (ticketId != null && !ticketId.isEmpty()) {
                        setupTicketStatusListener(ticketId);
                    } else {
                        findActiveRequestByUsers();
                    }
                } else {
                    findActiveRequestByUsers();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                updateChatFunctionality(false);
            }
        });
    }

    private void findActiveRequestByUsers() {
        DatabaseReference serviceRequests = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        serviceRequests.orderByChild("userId").equalTo(customerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String assignedId = ds.child("assignedTechId").getValue(String.class);
                        String status = ds.child("status").getValue(String.class);
                        if (techId.equals(assignedId) && !"Completed".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status)) {
                            setupTicketStatusListener(ds.child("ticketId").getValue(String.class));
                            return;
                        }
                    }
                }
                updateChatFunctionality(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { updateChatFunctionality(false); }
        });
    }

    private void setupTicketStatusListener(String ticketId) {
        DatabaseReference serviceRequests = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        serviceRequests.orderByChild("ticketId").equalTo(ticketId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                        String status = requestSnapshot.child("status").getValue(String.class);
                        boolean hasTimestamp = requestSnapshot.hasChild("acceptedTimestamp");
                        boolean isAccepted = "In Progress".equalsIgnoreCase(status) || hasTimestamp;
                        updateChatFunctionality(isAccepted);
                        return;
                    }
                } else {
                    updateChatFunctionality(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { updateChatFunctionality(false); }
        });
    }

    private void updateChatFunctionality(boolean enabled) {
        isChatFunctionalityEnabled = enabled;
        if (enabled) {
            etMessage.setEnabled(true);
            btnSend.setEnabled(true);
            btnCall.setEnabled(true);
            btnCall.setAlpha(1.0f);
            etMessage.setHint("Type a message");
        } else {
            etMessage.setEnabled(false);
            btnSend.setEnabled(false);
            btnCall.setEnabled(false);
            btnCall.setAlpha(0.5f);

            if (myId != null && myId.equals(techId)) {
                etMessage.setHint("Accept the ticket to chat");
            } else {
                etMessage.setHint("Waiting for technician to accept...");
            }
        }
    }

    private void markAsRead() {
        if (techId == null || customerId == null) return;
        String chatId = techId + "_" + customerId;
        FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("lastSenderId").setValue(myId);
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnCall = findViewById(R.id.btnCall);
        tvOtherPartyName = findViewById(R.id.tvCustomerName);
        headerAvatar = findViewById(R.id.customerAvatar);

        if (otherPartyName != null) tvOtherPartyName.setText(otherPartyName);

        if (myId != null && techId != null && myId.equals(techId)) {
            headerAvatar.setImageResource(R.drawable.man_customer_icon);
        } else {
            headerAvatar.setImageResource(R.drawable.new_technician);
        }
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        int mode = (myId != null && myId.equals(techId)) ? ChatAdapter.MODE_TECH_CHAT : ChatAdapter.MODE_CUSTOMER_CHAT;
        chatAdapter = new ChatAdapter(messageList, mode);
        chatAdapter.setOtherPartyName(otherPartyName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupFirebaseChatListener() {
        String chatId = techId + "_" + customerId;
        chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("messages");

        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    if (msg.getSenderId() != null) {
                        msg.setUser(msg.getSenderId().equals(myId));
                    } else {
                        msg.setUser(false);
                    }
                    messageList.add(msg);
                    int newPosition = messageList.size() - 1;
                    chatAdapter.notifyItemInserted(newPosition);
                    if (newPosition > 0) {
                        chatAdapter.notifyItemChanged(newPosition - 1);
                    }
                    chatRecyclerView.scrollToPosition(newPosition);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatRef.addChildEventListener(chatListener);
    }

    private void setupNotificationMonitor() {
        if (myId == null) return;
        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications").child(myId);
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialNotifLoad) {
                    NotificationModel n = snapshot.getValue(NotificationModel.class);
                    if (n != null && "CALL".equalsIgnoreCase(n.getType())) {
                        showIncomingCallPopup(n.getMessage(), n.getTicketId(), n.getSenderName());
                        snapshot.getRef().removeValue();
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        notifRef.addChildEventListener(notifListener);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { isInitialNotifLoad = false; }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showIncomingCallPopup(String message, String callerTechId, String callerName) {
        IncomingCallPopup popup = new IncomingCallPopup(this, callerName, new IncomingCallPopup.OnCallActionListener() {
            @Override
            public void onAnswer() {
                String chatId = (callerTechId != null ? callerTechId : techId) + "_" + customerId;
                DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);

                Map<String, Object> updates = new HashMap<>();
                updates.put("callStatus", "active");
                updates.put("callStartTime", ServerValue.TIMESTAMP);
                chatRef.updateChildren(updates);

                Intent intent = new Intent(CustomerChatActivity.this, CustomerVoiceCallActivity.class);
                intent.putExtra("NAME", otherPartyName);
                intent.putExtra("TECH_ID", callerTechId != null ? callerTechId : techId);
                intent.putExtra("CUSTOMER_ID", customerId);
                startActivity(intent);
            }

            @Override
            public void onDecline() {
                String chatId = (callerTechId != null ? callerTechId : techId) + "_" + customerId;
                FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
            }
        });
        popup.show();
    }

    private void sendMessage(String text) {
        if (customerId == null || techId == null) return;

        String chatId = techId + "_" + customerId;
        DatabaseReference rootChatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        DatabaseReference messageRef = rootChatRef.child("messages");

        String msgId = messageRef.push().getKey();
        if (msgId == null) return;

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("text", text);
        msgData.put("senderId", myId);
        msgData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("techId", techId);
        chatMeta.put("customerId", customerId);
        chatMeta.put("lastMessage", text);
        chatMeta.put("timestamp", System.currentTimeMillis());
        chatMeta.put("lastSenderId", myId);

        messageRef.child(msgId).setValue(msgData);
        rootChatRef.updateChildren(chatMeta);

        String otherUserId = myId.equals(techId) ? customerId : techId;
        String notifType = myId.equals(techId) ? "CHAT_FROM_TECH" : "CHAT_FROM_CUST";
        NotificationActivity.addNotification(otherUserId, "You have a new message", notifType, customerId);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> {
            if (!isChatFunctionalityEnabled) {
                String errorMsg = (myId != null && myId.equals(techId)) ? "Accept the ticket first." : "Waiting for technician to accept.";
                Toast.makeText(CustomerChatActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                return;
            }
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                etMessage.setText("");
            }
        });

        btnCall.setOnClickListener(v -> {
            if (!isChatFunctionalityEnabled) {
                Toast.makeText(CustomerChatActivity.this, "Calling is disabled until the ticket is accepted.", Toast.LENGTH_SHORT).show();
                return;
            }
            Dialog d = new Dialog(this);
            d.setContentView(R.layout.dialog_call_confirm);
            if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            TextView title = d.findViewById(R.id.tvRequestTitle);
            if (title != null) title.setText("Call " + otherPartyName + "?");

            View btnConfirm = d.findViewById(R.id.btnConfirmCall);
            if (btnConfirm != null) btnConfirm.setOnClickListener(view -> {
                initiateCall();
                d.dismiss();
            });

            View btnClose = d.findViewById(R.id.btnCloseCallDialog);
            if (btnClose != null) btnClose.setOnClickListener(view -> d.dismiss());

            d.show();
        });
    }

    private void initiateCall() {
        String callerId, receiverId, callerName;

        if (myId.equals(techId)) {
            callerId = techId;
            receiverId = customerId;
            callerName = "Technician";
        } else {
            callerId = customerId;
            receiverId = techId;
            callerName = "Customer";
        }

        String chatId = techId + "_" + customerId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("callStatus", "ringing");
        updates.put("callStartTime", null);
        chatRef.updateChildren(updates);

        String notifMessage = "<b>Incoming Call</b><br>" + (myId.equals(techId) ? "Technician" : "Customer") + " is calling.";
        String notifTarget = myId.equals(techId) ? "CustomerNotifications" : "Notifications";
        DatabaseReference notifPushRef = FirebaseDatabase.getInstance(DB_URL).getReference(notifTarget).child(receiverId).push();

        // Use local variable for current user name if available, else generic
        String myName = "User";
        // Attempt to find current user name from meta or static resolution (or fetch it)
        // For simplicity, we'll use "Customer" or "Technician" or better yet, fetch it.
        
        NotificationModel notification = new NotificationModel(
                notifPushRef.getKey(),
                notifMessage,
                System.currentTimeMillis(),
                "CALL",
                callerId,
                myId.equals(techId) ? "Technician" : "Customer" 
        );
        notifPushRef.setValue(notification);

        Intent intent;
        if (myId.equals(techId)) {
            intent = new Intent(this, VoiceCallActivity.class);
        } else {
            intent = new Intent(this, CustomerVoiceCallActivity.class);
        }
        intent.putExtra("TECH_ID", techId);
        intent.putExtra("CUSTOMER_ID", customerId);
        intent.putExtra("NAME", otherPartyName);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatListener != null) chatRef.removeEventListener(chatListener);
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
        if (serviceRequestRef != null && serviceRequestListener != null) {
            serviceRequestRef.removeEventListener(serviceRequestListener);
        }
    }
}