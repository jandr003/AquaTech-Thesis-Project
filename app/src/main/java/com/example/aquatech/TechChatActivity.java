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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class TechChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageView btnBack, btnCall;
    private TextView customerNameDisplay;
    private ImageView avatarHeader;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private DatabaseReference chatRef, notifRef, myProfileRef;
    private String techId, customerId, customerName;
    private String myRealName = "";
    private ChildEventListener chatListener, notifListener;
    private ValueEventListener profileListener;
    private boolean isInitialNotifLoad = true;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_tech_chat);

        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(0, 0, 0, keyboardHeight);
                return insets;
            });
        }

        techId = getIntent().getStringExtra("TECH_ID");
        if (techId == null || techId.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                techId = user.getUid();
            }
        }

        myRealName = resolveTechNameFromEmail();
        fetchMyRealNameFromDB();

        customerId = getIntent().getStringExtra("CUSTOMER_ID");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");

        initializeViews();
        setupRecyclerView();

        if (techId != null && customerId != null) {
            setupFirebaseChatListener();
            setupNotificationMonitor();
        }

        setupClickListeners();
    }

    private void fetchMyRealNameFromDB() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            myProfileRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null) {
                            name = snapshot.child("name").getValue(String.class);
                        }

                        if (name != null && !name.isEmpty()) {
                            myRealName = name;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            myProfileRef.addValueEventListener(profileListener);
        }
    }

    private String resolveTechNameFromEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) {
            return "Technician";
        }

        String email = user.getEmail().toLowerCase().trim();
        int atIndex = email.indexOf("@");
        if (atIndex > 0) {
            String username = email.substring(0, atIndex);
            String[] parts = username.split("[._]");
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                if (!p.isEmpty()) {
                    sb.append(Character.toUpperCase(p.charAt(0)))
                            .append(p.substring(1))
                            .append(" ");
                }
            }
            return sb.toString().trim();
        }
        return "Technician";
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.lessThanIcon);
        btnCall = findViewById(R.id.btnCall);
        customerNameDisplay = findViewById(R.id.TechNameDisplay);
        avatarHeader = findViewById(R.id.techAvatar);

        if (avatarHeader != null) {
            int avatarRes = getCustomerAvatarDrawable(customerId);
            avatarHeader.setImageResource(avatarRes);
        }

        if (customerName != null && customerNameDisplay != null) {
            customerNameDisplay.setText(customerName);
        }
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, ChatAdapter.MODE_TECH_CHAT);
        chatAdapter.setOtherPartyName(customerName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        if (chatRecyclerView != null) {
            chatRecyclerView.setLayoutManager(layoutManager);
            chatRecyclerView.setAdapter(chatAdapter);
        }
    }

    private void setupFirebaseChatListener() {
        String chatId = techId + "_" + customerId;
        chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("messages");

        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    msg.setUser(msg.getSenderId() != null && msg.getSenderId().equals(techId));
                    messageList.add(msg);

                    int newPos = messageList.size() - 1;
                    if (chatAdapter != null) {
                        chatAdapter.notifyItemInserted(newPos);
                        if (newPos > 0) {
                            chatAdapter.notifyItemChanged(newPos - 1);
                        }
                    }

                    if (chatRecyclerView != null) {
                        chatRecyclerView.scrollToPosition(newPos);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        chatRef.addChildEventListener(chatListener);
    }

    private void setupNotificationMonitor() {
        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(techId);
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

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        notifRef.addChildEventListener(notifListener);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isInitialNotifLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showIncomingCallPopup(String message, String callerCustId, String callerName) {
        IncomingCallPopup popup = new IncomingCallPopup(
                this,
                callerName != null ? callerName : "Customer",
                "Customer",
                callerCustId != null ? callerCustId : customerId,
                techId,
                true,
                new IncomingCallPopup.OnCallActionListener() {
                    @Override
                    public void onAnswer() {
                        String chatId = techId + "_" + (callerCustId != null ? callerCustId : customerId);
                        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("callStatus", "active");
                        updates.put("callStartTime", ServerValue.TIMESTAMP);
                        chatRef.updateChildren(updates);

                        Intent intent = new Intent(TechChatActivity.this, VoiceCallActivity.class);
                        intent.putExtra("TECH_ID", techId);
                        intent.putExtra("CUSTOMER_ID", callerCustId != null ? callerCustId : customerId);
                        intent.putExtra("NAME", callerName);
                        startActivity(intent);
                    }

                    @Override
                    public void onDecline() {
                        String chatId = techId + "_" + (callerCustId != null ? callerCustId : customerId);
                        FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
                    }
                }
        );

        popup.show();
    }

    private void sendMessage(String text) {
        if (techId == null || customerId == null) {
            return;
        }

        String finalName = resolveTechNameFromEmail();
        if (finalName.isEmpty()) {
            finalName = (myRealName != null && !myRealName.isEmpty()) ? myRealName : "Technician";
        }

        String chatId = techId + "_" + customerId;
        DatabaseReference rootChatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);
        DatabaseReference messageRef = rootChatRef.child("messages");
        String msgId = messageRef.push().getKey();
        if (msgId == null) {
            return;
        }

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("text", text);
        msgData.put("senderId", techId);
        msgData.put("receiverId", customerId);
        msgData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> chatMeta = new HashMap<>();
        chatMeta.put("techId", techId);
        chatMeta.put("customerId", customerId);
        chatMeta.put("lastMessage", text);
        chatMeta.put("timestamp", System.currentTimeMillis());
        chatMeta.put("customerName", customerName);
        chatMeta.put("techName", finalName);
        chatMeta.put("lastSenderId", techId);

        rootChatRef.updateChildren(chatMeta);

        String notifMessage = "Technician sent you a message!";
        sendNotificationToUser(customerId, notifMessage, "CHAT", techId, true);

        messageRef.child(msgId).setValue(msgData).addOnCompleteListener(task -> {
            if (task.isSuccessful() && etMessage != null) {
                etMessage.setText("");
            }
        });
    }

    private void sendNotificationToUser(String userId, String message, String type, String ticketId, boolean isToCustomer) {
        if (userId == null) {
            return;
        }

        String target = isToCustomer ? "CustomerNotifications" : "Notifications";
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL).getReference(target).child(userId).push();

        String senderName = (myRealName != null && !myRealName.isEmpty()) ? myRealName : "Technician";
        NotificationModel notification = new NotificationModel(
                ref.getKey(),
                message,
                System.currentTimeMillis(),
                type,
                ticketId,
                senderName
        );

        ref.setValue(notification);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnAttach = findViewById(R.id.btnAttach);
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> openPdfPicker());
        }

        View btnAttachImg = findViewById(R.id.btnAttachImage);
        if (btnAttachImg != null) {
            btnAttachImg.setOnClickListener(v -> openImagePicker());
        }

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                String msg = etMessage != null ? etMessage.getText().toString().trim() : "";
                if (!msg.isEmpty()) {
                    sendMessage(msg);
                }
            });
        }

        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                Dialog d = new Dialog(this);
                d.setContentView(R.layout.dialog_call_confirm);

                if (d.getWindow() != null) {
                    d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                TextView title = d.findViewById(R.id.tvRequestTitle);
                if (title != null) {
                    title.setText("Call " + customerName + "?");
                }

                View btnConfirm = d.findViewById(R.id.btnConfirmCall);
                if (btnConfirm != null) {
                    btnConfirm.setOnClickListener(view -> {
                        initiateCall();
                        d.dismiss();
                    });
                }

                View btnClose = d.findViewById(R.id.btnCloseCallDialog);
                if (btnClose != null) {
                    btnClose.setOnClickListener(view -> d.dismiss());
                }

                d.show();
            });
        }
    }

    private static final int PICK_CHAT_FILE_REQUEST = 102;

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF Document"), PICK_CHAT_FILE_REQUEST);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Gallery Image"), PICK_CHAT_FILE_REQUEST);
    }

    private int getCustomerAvatarDrawable(String custId) {
        if (custId != null) {
            int hash = Math.abs(custId.hashCode()) % 8;
            switch (hash) {
                case 1:
                    return R.drawable.customer_avatar2;
                case 2:
                    return R.drawable.customer_avatar3;
                case 3:
                    return R.drawable.customer_avatar4;
                case 4:
                    return R.drawable.customer_avatar5;
                case 5:
                    return R.drawable.customer_avatar6;
                case 6:
                    return R.drawable.customer_avatar7;
                case 7:
                    return R.drawable.customer_avatar8;
                default:
                    return R.drawable.customer_avatar1;
            }
        }
        return R.drawable.customer_avatar1;
    }

    private void initiateCall() {
        if (techId == null || customerId == null) {
            return;
        }

        String callerName = (myRealName != null && !myRealName.isEmpty()) ? myRealName : "Technician";
        String chatId = techId + "_" + customerId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("callStatus", "ringing");
        updates.put("callStartTime", null);
        chatRef.updateChildren(updates);

        String notifMessage = "<b>Incoming Call</b><br>" + callerName + " is calling.";
        sendNotificationToUser(customerId, notifMessage, "CALL", techId, true);

        Intent intent = new Intent(this, VoiceCallActivity.class);
        intent.putExtra("TECH_ID", techId);
        intent.putExtra("CUSTOMER_ID", customerId);
        intent.putExtra("NAME", customerName);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }

        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }

        if (myProfileRef != null && profileListener != null) {
            myProfileRef.removeEventListener(profileListener);
        }
    }
}
