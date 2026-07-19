package com.example.aquatech;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerDashboardActivity extends AppCompatActivity {

    private TextView customerUserName, refValue, unitValue, unitName, messageSubtitle;
    private ImageView imgDispenser;
    private ImageView editPen, settingIcon, notificationIcon, customerIcon;
    private ImageView iconMessage;
    private View msgNotificationDot;
    private FrameLayout notificationContainer;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef, requestsRef, notifRef, chatsRef;
    private Query monitorQuery;
    private ValueEventListener monitorListener, messageDotListener, userListener;
    private ChildEventListener notifListener;

    private String assignedTechName = "Technician";
    private String assignedTechId = null;
    private String currentRequestStatus = null;
    private String currentTicketId = null;
    private boolean hasActiveRequest = false;
    private boolean isInitialRequestLoad = true;
    private boolean isInitialNotifLoad = true;
    private boolean isActivityInForeground = false;
    private boolean hasRated = false; // para maiwasan ang multiple rating dialogs

    // For chat threads list
    private RecyclerView rvChatThreads;
    private ChatThreadAdapter chatThreadAdapter;
    private List<DataSnapshot> chatThreadsList = new ArrayList<>();

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    reloadUserProfile();
                }
            }
    );

    private final ActivityResultLauncher<Intent> unitSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String uName = data.getStringExtra("unit_name");
                    String serial = data.getStringExtra("serial_no");
                    int unitImg = data.getIntExtra("image_res", R.drawable.titled_design1);
                    if (unitValue != null) unitValue.setText(uName);
                    if (unitName != null) unitName.setText(serial);
                    if (imgDispenser != null) imgDispenser.setImageResource(unitImg);
                    saveUnitToFirebase(uName, serial);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        chatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");

        setupStatusBar();
        initializeViews();
        clearSampleData();
        setupFirebaseListener();
        handleIncomingSetupData();
        setupClickListeners();
        setupServiceRequestMonitor();
        setupNotificationMonitor();
        setupMessageNotificationListener();
        setupCustomerChatThreadsListener(); 
        setupMessageClickListener();         
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityInForeground = true;
        if (messageDotListener != null) chatsRef.addListenerForSingleValueEvent(messageDotListener);
        reloadUserProfile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityInForeground = false;
    }

    private void reloadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        DatabaseReference profileRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users").child(user.getUid());

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null) name = snapshot.child("name").getValue(String.class);
                    if (name != null && customerUserName != null) {
                        customerUserName.setText(name);
                        Log.d("PROFILE", "Name updated to: " + name);
                    }

                    String rootUnit = snapshot.child("unitName").getValue(String.class);
                    String rootSerial = snapshot.child("serialNo").getValue(String.class);
                    String rootRef = snapshot.child("referenceNo").getValue(String.class);

                    if (rootUnit != null) {
                        if (unitValue != null) unitValue.setText(rootUnit);
                        if (unitName != null) unitName.setText(rootSerial != null ? rootSerial : "---");
                        if (refValue != null) refValue.setText(rootRef != null ? rootRef : "---");
                        if (imgDispenser != null) imgDispenser.setImageResource(getImageForUnit(rootUnit));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PROFILE", "Failed to reload: " + error.getMessage());
            }
        });
    }

    private void initializeViews() {
        customerUserName = findViewById(R.id.CustomerName);
        refValue = findViewById(R.id.refValue);
        unitValue = findViewById(R.id.unitValue);
        unitName = findViewById(R.id.unitName);
        imgDispenser = findViewById(R.id.imgDispenser);
        editPen = findViewById(R.id.EditPen1);
        settingIcon = findViewById(R.id.SettingIcon);
        notificationIcon = findViewById(R.id.notificationIcon);
        customerIcon = findViewById(R.id.CustomerIcon);
        msgNotificationDot = findViewById(R.id.msgNotificationDot);
        messageSubtitle = findViewById(R.id.messageSubtitle);
        notificationContainer = findViewById(R.id.notificationContainer);
        iconMessage = findViewById(R.id.iconMessage);

        CardView messageCard = findViewById(R.id.messageRequestButton);
        if (messageCard != null) {
            messageCard.setCardBackgroundColor(Color.parseColor("#3EB5E8"));
        }
    }

    private void setupMessageNotificationListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String myUid = user.getUid();
        messageDotListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasUnread = false;
                if (currentRequestStatus != null && currentRequestStatus.equalsIgnoreCase("In Progress")) {
                    for (DataSnapshot chat : snapshot.getChildren()) {
                        String chatId = chat.getKey();
                        if (chatId != null && chatId.contains(myUid)) {
                            String lastSenderId = chat.child("lastSenderId").getValue(String.class);
                            if (lastSenderId != null && !lastSenderId.equals(myUid)) {
                                hasUnread = true;
                                String lastMsg = chat.child("lastMessage").getValue(String.class);
                                String tName = chat.child("techName").getValue(String.class);
                                if (lastMsg != null && messageSubtitle != null) {
                                    messageSubtitle.setText(Html.fromHtml("<b>" + (tName != null ? tName : "Tech") + "</b>: " + lastMsg));
                                    messageSubtitle.setTextColor(Color.WHITE);
                                }
                                break;
                            }
                        }
                    }
                } else {
                    if (messageSubtitle != null) {
                        messageSubtitle.setText("Waiting for technician to accept...");
                        messageSubtitle.setTextColor(Color.parseColor("#B0BEC5"));
                    }
                }
                if (msgNotificationDot != null) msgNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatsRef.addValueEventListener(messageDotListener);
    }

    private void setupCustomerChatThreadsListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String myUid = user.getUid();

        DatabaseReference userChatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");

        userChatsRef.orderByChild("customerId").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatThreadsList.clear();
                        boolean hasUnread = false;
                        String latestMessage = "";
                        String latestSender = "";

                        if (snapshot.getChildrenCount() > 0) {
                            hasUnread = true;
                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                String lastMessage = chatSnapshot.child("lastMessage").getValue(String.class);
                                String techName = chatSnapshot.child("techName").getValue(String.class);
                                if (lastMessage != null) {
                                    latestMessage = lastMessage;
                                    latestSender = techName != null ? techName : "Technician";
                                }
                                break;
                            }
                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                chatThreadsList.add(chatSnapshot);
                            }
                        }

                        if (chatThreadAdapter != null) {
                            chatThreadAdapter.notifyDataSetChanged();
                        }

                        if (msgNotificationDot != null) {
                            msgNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
                        }

                        if (messageSubtitle != null) {
                            if (hasUnread && !latestMessage.isEmpty()) {
                                messageSubtitle.setText(Html.fromHtml("<b>" + latestSender + "</b>: " + latestMessage));
                                messageSubtitle.setTextColor(Color.WHITE);
                            } else if (currentRequestStatus != null && currentRequestStatus.equalsIgnoreCase("In Progress")) {
                                messageSubtitle.setText("Chat with your technician");
                                messageSubtitle.setTextColor(Color.parseColor("#B0BEC5"));
                            } else {
                                messageSubtitle.setText("Waiting for technician to accept...");
                                messageSubtitle.setTextColor(Color.parseColor("#B0BEC5"));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("CHAT_THREADS", "Error loading chats: " + error.getMessage());
                    }
                });
    }

    private void setupMessageClickListener() {
        View messageCard = findViewById(R.id.messageRequestButton);
        if (messageCard != null) {
            messageCard.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerDashboardActivity.this, CustomerChatListActivity.class);
                startActivity(intent);
            });
        } else if (iconMessage != null) {
            iconMessage.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerDashboardActivity.this, CustomerChatListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupFirebaseListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(user.getUid());
            userListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null) name = snapshot.child("name").getValue(String.class);
                        if (name != null && customerUserName != null) customerUserName.setText(name);

                        String rootUnit = snapshot.child("unitName").getValue(String.class);
                        String rootSerial = snapshot.child("serialNo").getValue(String.class);
                        String rootRef = snapshot.child("referenceNo").getValue(String.class);

                        if (rootUnit != null) {
                            if (unitValue != null) unitValue.setText(rootUnit);
                            if (unitName != null) unitName.setText(rootSerial != null ? rootSerial : "---");
                            if (refValue != null) refValue.setText(rootRef != null ? rootRef : "---");
                            if (imgDispenser != null) imgDispenser.setImageResource(getImageForUnit(rootUnit));
                        } else {
                            DataSnapshot unitsFolder = snapshot.child("units");
                            if (unitsFolder.exists() && unitsFolder.hasChildren()) {
                                DataSnapshot lastUnit = null;
                                for (DataSnapshot u : unitsFolder.getChildren()) { lastUnit = u; }
                                if (lastUnit != null) {
                                    String uLabel = lastUnit.child("unitName").getValue(String.class);
                                    String uSerial = lastUnit.child("serialNo").getValue(String.class);
                                    String sro = lastUnit.child("referenceNo").getValue(String.class);
                                    if (unitValue != null) unitValue.setText(uLabel != null ? uLabel : "---");
                                    if (unitName != null) unitName.setText(uSerial != null ? uSerial : "---");
                                    if (refValue != null) refValue.setText(sro != null ? sro : "---");
                                    if (imgDispenser != null && uLabel != null) imgDispenser.setImageResource(getImageForUnit(uLabel));
                                }
                            }
                        }
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
            userRef.addValueEventListener(userListener);
        }
    }

    private void clearSampleData() {
        if (customerUserName != null) customerUserName.setText("Loading...");
        if (refValue != null) refValue.setText("---");
        if (unitValue != null) unitValue.setText("---");
        if (unitName != null) unitName.setText("---");
        if (imgDispenser != null) imgDispenser.setImageDrawable(null);
    }

    private void handleIncomingSetupData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("UNIT_NO")) {
            String uName = intent.getStringExtra("UNIT_NAME");
            String uNo = intent.getStringExtra("UNIT_NO");
            String serialTemplate = getTemplateForUnit(uName);
            String finalSerial = serialTemplate.replace("**-***", uNo);
            if (unitValue != null) unitValue.setText(uName);
            if (unitName != null) unitName.setText(finalSerial);
            int imgRes = getImageForUnit(uName);
            if (imgDispenser != null) imgDispenser.setImageResource(imgRes);
            saveUnitToFirebase(uName, finalSerial);
        }
    }

    private void setupServiceRequestMonitor() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        // Listen to ALL requests from this user to find any active ones
        monitorQuery = requestsRef.orderByChild("userId").equalTo(user.getUid());
        monitorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                String oldStatus = currentRequestStatus;
                hasActiveRequest = false;
                assignedTechId = null;
                assignedTechName = "Technician";
                currentRequestStatus = null;
                currentTicketId = null;

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String status = ds.child("status").getValue(String.class);
                        if (status == null) continue;

                        boolean isFinished = status.equalsIgnoreCase("Completed") || 
                                           status.equalsIgnoreCase("Cancelled") || 
                                           status.equalsIgnoreCase("Rejected");

                        if (!isFinished) {
                            // Any status other than finished means an active request
                            hasActiveRequest = true;
                            currentRequestStatus = status;
                            currentTicketId = ds.getKey();
                            
                            String tName = ds.child("assignedTechName").getValue(String.class);
                            String tId = ds.child("assignedTechId").getValue(String.class);
                            if (tId != null && !tId.isEmpty()) {
                                assignedTechId = tId;
                                assignedTechName = (tName != null && !tName.isEmpty()) ? tName : "Technician";
                            }

                            if (!isInitialRequestLoad && "In Progress".equalsIgnoreCase(status)) {
                                if (oldStatus == null || !oldStatus.equalsIgnoreCase("In Progress")) {
                                    showTopNotificationPopup(assignedTechName);
                                }
                            }
                        } else if ("Completed".equalsIgnoreCase(status)) {
                            Boolean rated = ds.child("rated").getValue(Boolean.class);
                            if (rated == null || !rated) {
                                // Potentially the last one that needs rating
                                currentTicketId = ds.getKey();
                                String tName = ds.child("assignedTechName").getValue(String.class);
                                String tId = ds.child("assignedTechId").getValue(String.class);
                                showRatingDialog(currentTicketId, tName, tId);
                            }
                        }
                    }
                }

                View msgBtn = findViewById(R.id.messageRequestButton);
                boolean isAccepted = "In Progress".equalsIgnoreCase(currentRequestStatus);
                if (msgBtn != null) msgBtn.setAlpha((isAccepted) ? 1.0f : 0.6f);

                if (!isAccepted && messageSubtitle != null) {
                    messageSubtitle.setText("Waiting for technician to accept...");
                }

                isInitialRequestLoad = false;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        monitorQuery.addValueEventListener(monitorListener);
    }

    private void showTopNotificationPopup(String techName) {
        if (isFinishing() || isDestroyed() || notificationContainer == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.dialog_technician_accepted, notificationContainer, false);

        TextView tvTechName = layout.findViewById(R.id.tvAcceptedTechName);
        TextView btnView = layout.findViewById(R.id.btnAcceptedView);
        ImageView btnClose = layout.findViewById(R.id.btnCloseAccepted);

        if (tvTechName != null) tvTechName.setText("Technician " + techName + " is on the way");

        if (btnView != null) {
            btnView.setOnClickListener(v -> {
                notificationContainer.removeAllViews();
                if (hasActiveRequest && currentTicketId != null) {
                    Intent intent = new Intent(CustomerDashboardActivity.this, ServiceReceiptActivity.class);
                    intent.putExtra("TICKET_ID", currentTicketId);
                    startActivity(intent);
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> notificationContainer.removeAllViews());
        }

        notificationContainer.removeAllViews();
        notificationContainer.addView(layout);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (notificationContainer != null) notificationContainer.removeAllViews();
        }, 15000);
    }

    private void showRatingDialog(String ticketId, String techName, String techId) {
        if (hasRated) return;
        hasRated = true;

        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rate_technician); 
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivTechProfile = dialog.findViewById(R.id.ivTechProfile);
        TextView tvTechName = dialog.findViewById(R.id.tvTechName);
        RatingBar ratingBar = dialog.findViewById(R.id.ratingBar);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitRating);

        tvTechName.setText(techName != null ? techName : "Technician");

        if (techId != null) {
            DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(techId);
            techRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String url = snapshot.getValue(String.class);
                    if (url != null && !url.isEmpty()) {
                        Glide.with(CustomerDashboardActivity.this).load(url).circleCrop().into(ivTechProfile);
                    } else {
                        ivTechProfile.setImageResource(R.drawable.new_technician);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            ivTechProfile.setImageResource(R.drawable.new_technician);
        }

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating < 1) {
                Toast.makeText(this, "Please rate at least 1 star", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> ratingUpdate = new HashMap<>();
            ratingUpdate.put("rating", rating);
            ratingUpdate.put("rated", true);

            requestsRef.child(ticketId).updateChildren(ratingUpdate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        hasRated = false;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                        hasRated = false;
                    });
        });

        dialog.setOnDismissListener(d -> hasRated = false);
        dialog.show();
    }

    private void setupNotificationMonitor() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications").child(user.getUid());
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialNotifLoad) {
                    NotificationModel n = snapshot.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null) {
                        String type = n.getType();
                        if ("CALL".equalsIgnoreCase(type)) {
                            if (isActivityInForeground) showIncomingCallPopup(n.getMessage(), n.getTicketId());
                            snapshot.getRef().removeValue();
                        } else if ("CHAT".equalsIgnoreCase(type) || "ACCEPTED".equalsIgnoreCase(type)) {
                            if ("In Progress".equalsIgnoreCase(currentRequestStatus)) {
                                if (messageSubtitle != null) {
                                    messageSubtitle.setText(Html.fromHtml(n.getMessage()));
                                    messageSubtitle.setTextColor(Color.parseColor("#FFD700"));
                                }
                            }
                        }
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

    private void showIncomingCallPopup(String message, String callerTechId) {
        if (isFinishing() || isDestroyed() || notificationContainer == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.layout_incoming_call_popup, notificationContainer, false);

        TextView tvName = layout.findViewById(R.id.tvCallerName);
        ImageView btnAnswer = layout.findViewById(R.id.btnAnswerCall);
        ImageView btnDecline = layout.findViewById(R.id.btnDeclineCall);
        ImageView ivAvatar = layout.findViewById(R.id.ivCallerAvatar);

        ivAvatar.setImageResource(R.drawable.new_technician);
        tvName.setText(assignedTechName != null ? assignedTechName : "Technician");

        btnAnswer.setOnClickListener(v -> {
            notificationContainer.removeAllViews();
            String currentTechId = callerTechId != null ? callerTechId : assignedTechId;
            String chatId = currentTechId + "_" + mAuth.getUid();

            // 📍 REAL-TIME CALL FIX: Set status AND start time immediately
            Map<String, Object> callUpdates = new HashMap<>();
            callUpdates.put("callStatus", "active");
            callUpdates.put("callStartTime", ServerValue.TIMESTAMP);

            FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).updateChildren(callUpdates);

            Intent intent = new Intent(this, CustomerVoiceCallActivity.class);
            intent.putExtra("NAME", assignedTechName);
            intent.putExtra("TECH_ID", currentTechId);
            intent.putExtra("CUSTOMER_ID", mAuth.getUid());
            startActivity(intent);
        });

        btnDecline.setOnClickListener(v -> {
            notificationContainer.removeAllViews();
            String currentTechId = callerTechId != null ? callerTechId : assignedTechId;
            String chatId = currentTechId + "_" + mAuth.getUid();
            FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
        });

        notificationContainer.removeAllViews();
        notificationContainer.addView(layout);
    }

    private String getTemplateForUnit(String name) {
        if (name == null) return "F-FXCU1-M-HCA-TT-**-***";
        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) return "S-FXCU1-M-HCA-AA-**-***";
        if (upper.contains("COUNTER")) return "CT-FXCU1-M-HCA-WT-**-***";
        if (upper.contains("STANDING")) return "ST-FXCU1-M-HCA-WT-**-***";
        return "F-FXCU1-M-HCA-TT-**-***";
    }

    private int getImageForUnit(String name) {
        if (name == null) return R.drawable.titled_design1;
        String upper = name.toUpperCase();
        if (upper.contains("SLIM")) return R.drawable.titled_design2;
        if (upper.contains("COUNTER")) return R.drawable.titled_design3;
        if (upper.contains("STANDING")) return R.drawable.titled_design4;
        return R.drawable.titled_design1;
    }

    private void saveUnitToFirebase(String name, String serial) {
        if (userRef != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("unitName", name);
            map.put("serialNo", serial);
            userRef.updateChildren(map);
        }
    }

    private void setupClickListeners() {
        if (customerIcon != null) customerIcon.setOnClickListener(v -> showProfileMenu());
        if (settingIcon != null) settingIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        if (notificationIcon != null) notificationIcon.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        if (editPen != null) editPen.setOnClickListener(v -> unitSelectionLauncher.launch(new Intent(this, UnitSelectionActivity.class)));

        // 📍 FIX: Harang sa pag-submit kung may ongoing request
        findViewById(R.id.requestButton).setOnClickListener(v -> {
            if (hasActiveRequest) {
                showActiveRequestAlert();
            } else {
                startActivity(new Intent(this, ServiceRequestActivity.class));
            }
        });

        findViewById(R.id.trackRequestButton).setOnClickListener(v -> { if (hasActiveRequest) startActivity(new Intent(this, ServiceLogActivity.class)); else showNoActiveRequestAlert(); });
        findViewById(R.id.messageRequestButton).setOnClickListener(v -> {
            if (assignedTechId == null || assignedTechId.isEmpty()) {
                showNoTechAssignedAlert();
            } else if (!"In Progress".equalsIgnoreCase(currentRequestStatus)) {
                showTechNotAcceptedAlert();
            } else {
                if (msgNotificationDot != null) msgNotificationDot.setVisibility(View.GONE);
                Intent intent = new Intent(this, CustomerChatActivity.class);
                intent.putExtra("TECH_ID", assignedTechId);
                intent.putExtra("TECH_NAME", assignedTechName);
                intent.putExtra("CUSTOMER_ID", mAuth.getUid());
                startActivity(intent);
            }
        });
        findViewById(R.id.requestAssistanceButton).setOnClickListener(v -> startActivity(new Intent(this, AquaBuddyActivity.class)));
    }

    private void showActiveRequestAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Ongoing Request")
                .setMessage("You currently have an active service request. Please wait for it to be completed before submitting a new one.")
                .setPositiveButton("Track Progress", (dialog, which) -> {
                    startActivity(new Intent(this, ServiceLogActivity.class));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showNoTechAssignedAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Technician Not Assigned")
                .setMessage("A technician needs to be assigned to your request before you can chat.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTechNotAcceptedAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Request Pending")
                .setMessage("The technician has been assigned but hasn't accepted your request yet. Please wait for them to accept to start chatting.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNoActiveRequestAlert() {
        new AlertDialog.Builder(this)
                .setTitle("No Active Request")
                .setMessage("Would you like to submit one now?")
                .setPositiveButton("Submit", (dialog, which) -> startActivity(new Intent(this, ServiceRequestActivity.class)))
                .setNegativeButton("Not Now", null)
                .show();
    }

    private void showProfileMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_profile_bottom_sheet, null);
        dialog.setContentView(view);
        view.findViewById(R.id.bsEditProfileOption).setOnClickListener(v -> {
            dialog.dismiss();
            editProfileLauncher.launch(new Intent(this, EditProfileActivity.class));
        });
        view.findViewById(R.id.bsHistoryOption).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ServiceHistoryActivity.class));
        });
        view.findViewById(R.id.bsLogoutOption).setOnClickListener(v -> {
            dialog.dismiss();
            showLogoutDialog();
        });
        dialog.show();
    }

    private void showLogoutDialog() {
        Dialog d = new Dialog(this);
        d.setContentView(R.layout.dialog_logout);
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        d.findViewById(R.id.btnCancelLogout).setOnClickListener(v -> d.dismiss());
        d.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> { d.dismiss(); logoutUser(); });
        d.show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (monitorQuery != null && monitorListener != null) monitorQuery.removeEventListener(monitorListener);
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
        if (chatsRef != null && messageDotListener != null) chatsRef.removeEventListener(messageDotListener);
        if (userRef != null && userListener != null) userRef.removeEventListener(userListener);
    }
}