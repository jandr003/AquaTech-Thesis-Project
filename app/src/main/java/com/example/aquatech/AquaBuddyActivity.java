package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AquaBuddyActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private DatabaseReference botChatRef, requestsRef, notifRef;
    private FirebaseAuth mAuth;
    private String currentUid;
    private ChildEventListener notifListener;
    private boolean isInitialNotifLoad = true;
    private boolean isActivityInForeground = false;

    private String assignedTechId = null;
    private String assignedTechName = "Technician";

    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private final String uuid = UUID.randomUUID().toString();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Handler autoEndHandler = new Handler(Looper.getMainLooper());
    private Runnable autoEndRunnable = () -> {
        saveMessage("Conversation ended due to inactivity, buddy. Feel free to chat me again later!", false, true);
    };
    private static final long INACTIVITY_LIMIT = 300000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aqua_buddy);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
            botChatRef = FirebaseDatabase.getInstance(DB_URL).getReference("ChatBotHistory").child(currentUid);
            requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        }

        setupStatusBar();
        initializeViews();
        setupChat();
        initDialogflow();

        if (botChatRef != null) {
            loadChatHistory();
            startTrackingTechnician();
            handleIncomingFiles(); 
            setupNotificationMonitor();

            checkAndSendInitialGreeting();
        } else {
            addMessageToUI("Hello there, buddy! I'm AquaBuddy, your friendly assistant. How's your day going? I'm here to help you with your water dispenser needs!", false, false);
        }
        
        resetInactivityTimer();
    }

    private void checkAndSendInitialGreeting() {
        botChatRef.limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    sendBotMessage("Hi buddy! I noticed you clicked for assistance. How are you doing today? I'm ready to help you with any problems or requests you have! 😊");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityInForeground = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityInForeground = false;
    }

    private void setupNotificationMonitor() {
        if (currentUid == null) return;
        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications").child(currentUid);
        
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialNotifLoad) {
                    NotificationModel n = snapshot.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null && "CALL".equalsIgnoreCase(n.getType())) {
                        if (isActivityInForeground) {
                            showIncomingCallDialog(n.getMessage(), n.getTicketId());
                        }
                        snapshot.getRef().removeValue();
                    }
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        notifRef.addChildEventListener(notifListener);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { isInitialNotifLoad = false; }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showIncomingCallDialog(String message, String callerTechId) {
        new AlertDialog.Builder(this)
            .setTitle("Incoming Call")
            .setMessage(Html.fromHtml(message))
            .setCancelable(false)
            .setPositiveButton("Answer", (dialog, which) -> {
                String cTid = (callerTechId != null && !callerTechId.isEmpty()) ? callerTechId : assignedTechId;
                String cTname = (assignedTechName != null) ? assignedTechName : "Technician";

                if (cTid != null) {
                    String chatId = cTid + "_" + currentUid;
                    FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("active");
                }

                Intent intent = new Intent(this, CustomerVoiceCallActivity.class);
                intent.putExtra("NAME", cTname);
                intent.putExtra("TECH_ID", cTid);
                intent.putExtra("CUSTOMER_ID", currentUid);
                startActivity(intent);
            })
            .setNegativeButton("Decline", (dialog, which) -> {
                String cTid = (callerTechId != null && !callerTechId.isEmpty()) ? callerTechId : assignedTechId;
                if (cTid != null) {
                    String chatId = cTid + "_" + currentUid;
                    FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
                }
            })
            .show();
    }

    private void handleIncomingFiles() {
        if (getIntent().getBooleanExtra("SEND_FILE_MESSAGE", false)) {
            String fileName = getIntent().getStringExtra("FILE_NAME");
            String fileUri = getIntent().getStringExtra("FILE_URI");
            String ticketId = getIntent().getStringExtra("TICKET_ID");
            if (fileName != null) {
                ChatMessage combinedMsg = new ChatMessage(
                    "Here's your receipt, buddy! I've sent it to your history as well. Take care!",
                    fileName, 
                    "2 MB", 
                    fileUri
                );
                botChatRef.push().setValue(combinedMsg);
                if (currentUid != null) {
                    NotificationActivity.addNotification(currentUid, "<b>AquaBuddy</b> sent you a PDF receipt.", "PDF", ticketId != null ? ticketId : "N/A");
                }
            }
        }
    }

    private void resetInactivityTimer() {
        autoEndHandler.removeCallbacks(autoEndRunnable);
        autoEndHandler.postDelayed(autoEndRunnable, INACTIVITY_LIMIT);
    }

    private void startTrackingTechnician() {
        if (currentUid == null) return;
        Query latestRequest = requestsRef.orderByChild("userId").equalTo(currentUid).limitToLast(1);
        latestRequest.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String techName = ds.child("assignedTechName").getValue(String.class);
                    if (techName == null) techName = ds.child("technicianName").getValue(String.class);
                    String techId = ds.child("assignedTechId").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    if (techId != null) assignedTechId = techId;
                    if (techName != null) assignedTechName = techName;


                    if ("Assigned".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status)) {
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void initDialogflow() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.aquatech_bot_key);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();
            SessionsSettings sessionsSettings = SessionsSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            Log.e("AquaBuddy", "Dialogflow Error: " + e.getMessage());
        }
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        findViewById(R.id.lessThanIcon).setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                saveMessage(text, true, false);
                handleInternalLogic(text);
                etMessage.setText("");
                resetInactivityTimer();
            }
        });
    }

    private void handleInternalLogic(String text) {
        String input = text.toLowerCase();

        if (input.contains("installation") || input.contains("kit")) {
            sendBotMessage("Oh, about the <b>Installation Kit</b>, buddy! It usually includes the way valve and connectors. The price is around <b>₱500 - ₱800</b> depending on your unit. Need me to assist with a request for this?");
            return;
        }

        if (input.contains("filter") || input.contains("preventive") || input.contains("maintenance")) {
            sendBotMessage("Maintenance is important, buddy! Our <b>Filter Preventive Maintenance</b> package (CBC, Sediment, SMSF) usually costs <b>₱1,200 - ₱2,500</b>. This keeps your water fresh and safe!");
            return;
        }

        if (input.contains("parts") || input.contains("board") || input.contains("uv") || input.contains("panel")) {
            sendBotMessage("Looking for <b>Other Parts</b>, buddy? UV Lamps, PBC Boards, and Touch Panels range from <b>₱800 to ₱3,500</b>. Just let me know the specific part so I can help you better!");
            return;
        }

        if (input.contains("ang tagal") || input.contains("super late") || input.contains("tagal ng technician") || input.contains("waiting")) {
            if (assignedTechId != null) {
                sendBotMessage("I'm sorry if it's taking a while, buddy. You can actually <b>call your technician</b> (" + assignedTechName + ") directly through the 'Voice Call' button in your active request screen to check their location!");
            } else {
                sendBotMessage("Don't worry, buddy! A technician will be assigned to you soon. Once assigned, you can track them in real-time!");
            }
            return;
        }

        sendMessageToBot(text);
    }

    private void setupChat() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, ChatAdapter.MODE_BOT);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void loadChatHistory() {
        botChatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    messageList.add(msg);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveMessage(String text, boolean isUser, boolean isSystem) {
        if (botChatRef != null) {
            ChatMessage message = new ChatMessage(text, isUser, isSystem);
            botChatRef.push().setValue(message);
        } else {
            addMessageToUI(text, isUser, isSystem);
        }
    }

    private void sendBotMessage(String text) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            saveMessage(text, false, false);
            if (currentUid != null) {
                NotificationActivity.addNotification(currentUid, "<b>AquaBuddy</b>: " + text, "MESSAGE", "BOT_REPLY");
            }
        }, 1000);
    }

    private void sendMessageToBot(String text) {
        if (sessionName == null || sessionsClient == null) return;
        QueryInput queryInput = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(text).setLanguageCode("en-US")).build();

        new Thread(() -> {
            try {
                DetectIntentRequest request = DetectIntentRequest.newBuilder()
                        .setSession(sessionName.toString()).setQueryInput(queryInput).build();
                DetectIntentResponse response = sessionsClient.detectIntent(request);
                String botReply = response.getQueryResult().getFulfillmentText();
                
                // Personality Injection: ensure "buddy" is there if missing
                if (botReply != null && !botReply.toLowerCase().contains("buddy")) {
                    botReply += ", buddy!";
                }

                String finalBotReply = botReply;
                runOnUiThread(() -> {
                    if (finalBotReply != null && !finalBotReply.isEmpty()) {
                        saveMessage(finalBotReply, false, false);
                        resetInactivityTimer();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> addMessageToUI("I'm having a little trouble connecting, buddy. But I'm still here for you!", false, false));
            }
        }).start();
    }

    private void addMessageToUI(String text, boolean isUser, boolean isSystem) {
        ChatMessage message = new ChatMessage(text, isUser, isSystem);
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoEndHandler.removeCallbacks(autoEndRunnable);
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
    }
}
