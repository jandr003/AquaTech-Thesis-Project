package com.example.aquatech;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TechnicianDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private CardView cardInProg;
    private Button btnActionInProg;
    private TextView tvInProgLabel, tvInProgCount, tvUpcomingLabel, tvUpcomingCount, tvInProgName;
    private TextView tvInProgTicket, tvInProgService, tvInProgTime, tvInProgAddr, tvInProgSro;
    private TextView statusInProgDate;
    private View underlineInProg, upcomingProgress;
    private LinearLayout noTicketsLayout;
    private View dashboardScroll;
    private ImageView notifIcon, settingIcon;
    private FrameLayout notificationContainer;

    private RecyclerView rvUpcoming, rvMessageThreads;
    private UpcomingTaskAdapter upcomingAdapter;
    private ChatThreadAdapter threadAdapter;
    private List<DataSnapshot> chatThreadsList = new ArrayList<>();

    private DataSnapshot activeTaskSnapshot;
    private List<DataSnapshot> upcomingSnapshotsList = new ArrayList<>();

    private String currentTechName = "";
    private String currentTechUid = "";
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private DatabaseReference notifRef, requestsRef, userChatsRef;
    private FirebaseAuth mAuth;
    private boolean isInitialLoad = true;
    private boolean isActivityInForeground = false;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Dialog reportDialog;
    private Uri proofUri;
    private String currentRemarks = "";

    private ChildEventListener notifListener;
    private ValueEventListener tasksListener;
    private ValueEventListener messagesListener;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
        if (uri != null) {
            proofUri = uri;
            showSubmitReportDialog();
        }
    });
    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
        if (success) showSubmitReportDialog();
    });
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) launchCamera();
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        try {
            storage = FirebaseStorage.getInstance();
            storageRef = storage.getReference();
        } catch (Exception e) {
        }

        initializeViews();
        setupUpcomingRecyclerView();
        updateMenuVisibility();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentTechUid = user.getUid();
            fetchTechNameAndInitFirebase(user.getUid());
        } else {
            String currentEmail = getStoredEmail();
            if (!currentEmail.isEmpty()) {
                fetchUidFromEmailAndInit(currentEmail);
            }
        }

        setupClickListeners();
        handleIntent(getIntent());
        setupStatusBar();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void fetchTechNameAndInitFirebase(String uid) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(uid);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null && !name.isEmpty()) {
                        currentTechName = name.trim();
                    } else {
                        name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            currentTechName = name.trim();
                        }
                    }
                    setupNavHeader();
                    initializeFirebaseListeners(uid);
                } else {
                    DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(uid);
                    techRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String name = snapshot.child("fullName").getValue(String.class);
                                if (name == null)
                                    name = snapshot.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    currentTechName = name.trim();
                                }
                            } else {
                                setupNavHeader();
                            }
                            if (currentTechName == null || currentTechName.isEmpty())
                                currentTechName = "Technician";
                            setupNavHeader();
                            initializeFirebaseListeners(uid);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            setupNavHeader();
                            if (currentTechName == null || currentTechName.isEmpty())
                                currentTechName = "Technician";
                            setupNavHeader();
                            initializeFirebaseListeners(uid);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians").child(uid);
                techRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("fullName").getValue(String.class);
                            if (name == null) name = snapshot.child("name").getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                currentTechName = name.trim();
                            }
                        }
                        if (currentTechName == null || currentTechName.isEmpty())
                            currentTechName = "Technician";
                        setupNavHeader();
                        initializeFirebaseListeners(uid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        currentTechName = "Technician";
                        setupNavHeader();
                        initializeFirebaseListeners(uid);
                    }
                });
            }
        });
    }

    private void fetchUidFromEmailAndInit(String email) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users");
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        currentTechUid = child.getKey();
                        String name = child.child("fullName").getValue(String.class);
                        if (name == null) name = child.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            currentTechName = name.trim();
                        }
                        break;
                    }
                    setupNavHeader();
                    initializeFirebaseListeners(currentTechUid);
                } else {
                    DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
                    techRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot child : snapshot.getChildren()) {
                                    currentTechUid = child.getKey();
                                    String name = child.child("fullName").getValue(String.class);
                                    if (name == null)
                                        name = child.child("name").getValue(String.class);
                                    if (name != null && !name.isEmpty()) {
                                        currentTechName = name.trim();
                                    }
                                    break;
                                }
                            } else {
                                setupNavHeader();
                                currentTechUid = email;
                            }
                            setupNavHeader();
                            initializeFirebaseListeners(currentTechUid);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            setupNavHeader();
                            currentTechUid = email;
                            initializeFirebaseListeners(currentTechUid);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL).getReference("Technicians");
                techRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                currentTechUid = child.getKey();
                                String name = child.child("fullName").getValue(String.class);
                                if (name == null) name = child.child("name").getValue(String.class);
                                if (name != null && !name.isEmpty()) {
                                    currentTechName = name.trim();
                                }
                                break;
                            }
                        } else {
                            setupNavHeader();
                            currentTechUid = email;
                        }
                        setupNavHeader();
                        initializeFirebaseListeners(currentTechUid);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        setupNavHeader();
                        currentTechUid = email;
                        initializeFirebaseListeners(currentTechUid);
                    }
                });
            }
        });
    }

    private void initializeFirebaseListeners(String techIdentifier) {
        if (techIdentifier == null || techIdentifier.isEmpty()) return;

        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(techIdentifier);
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        userChatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");

        setupFirebaseTasksListener(techIdentifier);
        setupFirebaseMessagesListener(techIdentifier);
        setupDashboardNotificationPopup();
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

    private void setupFirebaseTasksListener(String techIdentifier) {
        if (requestsRef == null) return;
        tasksListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeTaskSnapshot = null;
                upcomingSnapshotsList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String assignedId = ds.child("assignedTechId").getValue(String.class);
                    String assignedName = ds.child("assignedTechName").getValue(String.class);
                    String status = ds.child("status").getValue(String.class);

                    boolean isMine = (assignedId != null && (assignedId.equals(currentTechUid) || assignedId.equalsIgnoreCase(techIdentifier))) ||
                            (assignedName != null && !currentTechName.isEmpty() && assignedName.equalsIgnoreCase(currentTechName));

                    if (isMine) {
                        if (currentTechUid != null && !currentTechUid.isEmpty() && (assignedId == null || !assignedId.equals(currentTechUid))) {
                            ds.getRef().child("assignedTechId").setValue(currentTechUid);
                        }

                        if ("In Progress".equalsIgnoreCase(status) || "Ongoing".equalsIgnoreCase(status) ||
                                "Arrived".equalsIgnoreCase(status) || "Submission".equalsIgnoreCase(status) ||
                                "Submitted".equalsIgnoreCase(status) || "Rejected".equalsIgnoreCase(status)) {

                            activeTaskSnapshot = ds;

                            if ("Rejected".equalsIgnoreCase(status) && isActivityInForeground) {
                                showResubmitPopup("Report Rejected", ds.getKey());
                            }

                        } else if ("Completed".equalsIgnoreCase(status)) {

                        } else if ("Open".equalsIgnoreCase(status) || "Assigned".equalsIgnoreCase(status)) {
                            upcomingSnapshotsList.add(ds);
                        }
                    }
                }
                updateUI();
                if (upcomingAdapter != null) upcomingAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        requestsRef.addValueEventListener(tasksListener);
    }

    private void handleAcceptFirebaseBySnapshot(DataSnapshot task) {
        if (activeTaskSnapshot != null) {
            Toast.makeText(this, "Finish your active ticket first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTechName == null || currentTechName.isEmpty()) {
            setupNavHeader();
            if (currentTechName.isEmpty()) {
                Toast.makeText(this, "Loading data...", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String dbKey = task.getKey();
        String ticketId = task.child("ticketId").getValue(String.class);
        String customerId = task.child("userId").getValue(String.class);
        String customerName = task.child("customerName").getValue(String.class);

        Log.d("ACCEPT_DEBUG", "Accepting ticket - customerId: " + customerId + ", customerName: " + customerName + ", ticketId: " + ticketId);

        if (dbKey == null) {
            Log.e("ACCEPT_ERROR", "dbKey is null");
            return;
        }
        if (requestsRef == null) {
            Log.e("ACCEPT_ERROR", "requestsRef is null");
            return;
        }

        DatabaseReference techRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Technicians").child(currentTechUid).child("signature");

        techRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String signature = snapshot.getValue(String.class);
                if (signature == null || signature.isEmpty()) {
                    signature = "signature1_png";
                    Log.d("SIGNATURE", "No signature found, using default: " + signature);
                } else {
                    Log.d("SIGNATURE", "Found signature: " + signature);
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "In Progress");
                updates.put("assignedTechId", currentTechUid);
                updates.put("assignedTechName", currentTechName);
                updates.put("assignedTimestamp", ServerValue.TIMESTAMP);
                updates.put("technicianSignature", signature); // ← signature idaragdag dito

                requestsRef.child(dbKey).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " accepted ticket #" + ticketId);
                            if (customerId != null && !customerId.isEmpty()) {
                                String htmlMsg = "<b>Technician Accepted!</b><br>Technician " + currentTechName + " has accepted your ticket #" + ticketId;
                                NotificationActivity.addNotification(customerId.trim(), htmlMsg, "CHAT", ticketId);

                                sendAcceptanceChatMessage(customerId.trim(), ticketId, customerName, htmlMsg);
                            } else {
                                Log.e("ACCEPT_ERROR", "customerId is null or empty, cannot send chat message");
                            }

                            Toast.makeText(TechnicianDashboardActivity.this, "Ticket Accepted!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ACCEPT_ERROR", "Failed to update ticket: " + e.getMessage());
                            Toast.makeText(TechnicianDashboardActivity.this, "Failed to accept ticket", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SIGNATURE", "Failed to get signature: " + error.getMessage());
                // Fallback: gamitin ang default signature
                String signature = "signature1_png";

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "In Progress");
                updates.put("assignedTechId", currentTechUid);
                updates.put("assignedTechName", currentTechName);
                updates.put("assignedTimestamp", ServerValue.TIMESTAMP);
                updates.put("technicianSignature", signature);

                requestsRef.child(dbKey).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> {
                            AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " accepted ticket #" + ticketId);
                            if (customerId != null && !customerId.isEmpty()) {
                                String htmlMsg = "<b>Technician Accepted!</b><br>Technician " + currentTechName + " has accepted your ticket #" + ticketId;
                                NotificationActivity.addNotification(customerId.trim(), htmlMsg, "CHAT", ticketId);
                                sendAcceptanceChatMessage(customerId.trim(), ticketId, customerName, htmlMsg);
                            }
                            Toast.makeText(TechnicianDashboardActivity.this, "Ticket Accepted!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ACCEPT_ERROR", "Failed to update ticket: " + e.getMessage());
                            Toast.makeText(TechnicianDashboardActivity.this, "Failed to accept ticket", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void sendAcceptanceChatMessage(String customerId, String ticketId, String customerName, String htmlMsg) {
        if (currentTechUid == null || currentTechUid.isEmpty()) {
            Log.e("CHAT_ERROR", "sendAcceptanceChatMessage: currentTechUid is null or empty");
            return;
        }
        if (customerId == null || customerId.isEmpty()) {
            Log.e("CHAT_ERROR", "sendAcceptanceChatMessage: customerId is null or empty");
            return;
        }

        String chatId = currentTechUid + "_" + customerId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId);

        Log.d("CHAT_DEBUG", "Creating chat with ID: " + chatId);

        DatabaseReference systemMsgRef = chatRef.child("messages").push();
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("text", htmlMsg);
        systemMessage.put("senderId", currentTechUid);
        systemMessage.put("timestamp", ServerValue.TIMESTAMP);
        systemMessage.put("system", true);
        systemMessage.put("isUser", false);

        systemMsgRef.setValue(systemMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("CHAT_DEBUG", "System message sent successfully");
            } else {
                Log.e("CHAT_ERROR", "Failed to send system message: " +
                        (task.getException() != null ? task.getException().getMessage() : "unknown error"));
            }
        });

        String welcomeText = "Hello! I'm Technician " + currentTechName + ". I've accepted your ticket #" + ticketId;
        DatabaseReference welcomeMsgRef = chatRef.child("messages").push();
        Map<String, Object> welcomeMessage = new HashMap<>();
        welcomeMessage.put("text", welcomeText);
        welcomeMessage.put("senderId", currentTechUid);
        welcomeMessage.put("timestamp", ServerValue.TIMESTAMP);
        welcomeMessage.put("system", false);
        welcomeMessage.put("isUser", false);
        welcomeMsgRef.setValue(welcomeMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("CHAT_DEBUG", "Welcome message sent successfully");
            } else {
                Log.e("CHAT_ERROR", "Failed to send welcome message: " +
                        (task.getException() != null ? task.getException().getMessage() : "unknown error"));
            }
        });

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerName", customerName != null ? customerName : "Customer");
        metadata.put("techName", currentTechName);
        metadata.put("techId", currentTechUid);
        metadata.put("customerId", customerId);
        metadata.put("ticketId", ticketId);
        metadata.put("lastMessage", welcomeText);
        metadata.put("lastSenderId", currentTechUid);
        metadata.put("timestamp", ServerValue.TIMESTAMP);

        chatRef.updateChildren(metadata).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("CHAT_DEBUG", "Chat metadata updated successfully for chatId: " + chatId);
            } else {
                Log.e("CHAT_ERROR", "Failed to update chat metadata: " +
                        (task.getException() != null ? task.getException().getMessage() : "unknown error"));
            }
        });
    }

    private String getStoredEmail() {
        if (getIntent() != null && getIntent().hasExtra("USER_EMAIL"))
            return getIntent().getStringExtra("USER_EMAIL").toLowerCase().trim();
        FirebaseUser user = mAuth.getCurrentUser();
        return (user != null && user.getEmail() != null) ? user.getEmail().toLowerCase().trim() : "";
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvInProgLabel = findViewById(R.id.tvProgressLabel);
        tvInProgCount = findViewById(R.id.tvProgressCount);
        underlineInProg = findViewById(R.id.underlineProgress);
        statusInProgDate = findViewById(R.id.statusOpenBlue);
        cardInProg = findViewById(R.id.TechnicianReportOne);
        btnActionInProg = findViewById(R.id.submitButton);
        tvInProgName = findViewById(R.id.tvReportLabel);
        tvInProgTicket = findViewById(R.id.displayTicketID);
        tvInProgService = findViewById(R.id.tvReportSubLabel);
        tvInProgTime = findViewById(R.id.displayTimeRequest);
        tvInProgAddr = findViewById(R.id.tvcustomerAddress);
        tvInProgSro = findViewById(R.id.customerSroNum);
        tvUpcomingLabel = findViewById(R.id.tvUpcomingLabel);
        tvUpcomingCount = findViewById(R.id.tvUpcomingLabelCount);
        upcomingProgress = findViewById(R.id.UpcomingProgress);
        noTicketsLayout = findViewById(R.id.noTicketsLayout);
        dashboardScroll = findViewById(R.id.dashboardScroll);
        notifIcon = findViewById(R.id.notifIcon);
        settingIcon = findViewById(R.id.settingIcon);
        rvUpcoming = findViewById(R.id.rvUpcomingTasks);
        rvMessageThreads = findViewById(R.id.rvMessageThreads);
        notificationContainer = findViewById(R.id.notificationContainer);
        hideDashboardElements();
    }

    private void hideDashboardElements() {
        if (cardInProg != null) cardInProg.setVisibility(View.GONE);
        if (tvInProgLabel != null) tvInProgLabel.setVisibility(View.GONE);
        if (tvInProgCount != null) tvInProgCount.setVisibility(View.GONE);
        if (underlineInProg != null) underlineInProg.setVisibility(View.GONE);
        if (statusInProgDate != null) statusInProgDate.setVisibility(View.GONE);
        if (tvUpcomingLabel != null) tvUpcomingLabel.setVisibility(View.GONE);
        if (tvUpcomingCount != null) tvUpcomingCount.setVisibility(View.GONE);
        if (upcomingProgress != null) upcomingProgress.setVisibility(View.GONE);
        if (rvUpcoming != null) rvUpcoming.setVisibility(View.GONE);
        if (dashboardScroll != null) dashboardScroll.setVisibility(View.GONE);
        if (noTicketsLayout != null) noTicketsLayout.setVisibility(View.VISIBLE);
    }

    private void setupNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;
        TextView tvTechMode = headerView.findViewById(R.id.tvTechMode);
        TextView tvTechEmail = headerView.findViewById(R.id.tvTechEmail);
        String email = getStoredEmail();
        if (!email.isEmpty()) {
            if (tvTechEmail != null) tvTechEmail.setText(email);
            boolean isAdmin = email.equals("admin@aquasmartguard.ph") || email.equals("management@aquasmartguard.ph");

            if (currentTechName == null || currentTechName.isEmpty()) {
                if (email.equals("admin@aquasmartguard.ph")) currentTechName = "John Andrew";
                else if (email.equals("management@aquasmartguard.ph"))
                    currentTechName = "Glenn Jean";
                else if (email.equals("john1@aquasmartguard.ph")) currentTechName = "John";
                else if (email.equals("jhonny.s@aquasmartguard.ph"))
                    currentTechName = "Jhonny Sinder";
                else if (email.equals("rarcilla@aquasmartguard.ph"))
                    currentTechName = "Ricky Arcilla";
                else if (email.equals("r.trinidad@aquasmartguard.ph"))
                    currentTechName = "Ruel Trinidad";
                else if (email.equals("gdelantar@aquasmartguard.ph"))
                    currentTechName = "Glenda Delantar";
                else if (email.equals("duena@aquasmartguard.ph")) currentTechName = "Jerry Duena";
                else if (email.equals("ji@aquasmartguard.ph"))
                    currentTechName = "Jonnifer Iglesias";
                else if (email.equals("marlon.salvador@aquasmartguard.ph"))
                    currentTechName = "Marlon Salvador";
                else if (email.equals("smjr@aquasmartguard.ph"))
                    currentTechName = "Selvino Magora Jr.";
                else currentTechName = "Technician";
            }

            if (tvTechMode != null)
                tvTechMode.setText(isAdmin ? currentTechName + " (Admin)" : currentTechName);
        }
    }

    private void updateMenuVisibility() {
        String email = getStoredEmail();
        if (!email.isEmpty() && navigationView != null) {
            boolean isAdmin = email.equals("admin@aquasmartguard.ph") || email.equals("management@aquasmartguard.ph");
            Menu menu = navigationView.getMenu();
            MenuItem adminItem = menu.findItem(R.id.nav_admin_panel);
            if (adminItem != null) adminItem.setVisible(isAdmin);
        }
    }

    private void setupUpcomingRecyclerView() {
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        upcomingAdapter = new UpcomingTaskAdapter(upcomingSnapshotsList, new UpcomingTaskAdapter.OnUpcomingClickListener() {
            @Override
            public void onAcceptClick(DataSnapshot snapshot) {
                handleAcceptFirebaseBySnapshot(snapshot);
            }

            @Override
            public void onMessageClick(DataSnapshot snapshot) {
                String customerId = snapshot.child("userId").getValue(String.class);
                String cName = snapshot.child("customerName").getValue(String.class);
                if (customerId != null) {
                    Intent intent = new Intent(TechnicianDashboardActivity.this, CustomerChatActivity.class);
                    intent.putExtra("TECH_ID", currentTechUid);
                    intent.putExtra("TECH_NAME", currentTechName);
                    intent.putExtra("CUSTOMER_ID", customerId.trim());
                    intent.putExtra("CUSTOMER_NAME", cName != null ? cName : "Customer");
                    startActivity(intent);
                }
            }
        });
        rvUpcoming.setAdapter(upcomingAdapter);
    }

    private void updateUI() {
        if (isFinishing() || isDestroyed()) return;
        SimpleDateFormat df = new SimpleDateFormat("dd", Locale.getDefault()), mf = new SimpleDateFormat("MMM", Locale.getDefault());

        boolean hasActive = (activeTaskSnapshot != null && activeTaskSnapshot.exists());

        if (hasActive) {
            if (tvInProgLabel != null) tvInProgLabel.setVisibility(View.VISIBLE);
            if (tvInProgCount != null) tvInProgCount.setVisibility(View.VISIBLE);
            if (underlineInProg != null) underlineInProg.setVisibility(View.VISIBLE);
            if (statusInProgDate != null) statusInProgDate.setVisibility(View.VISIBLE);
            if (cardInProg != null) cardInProg.setVisibility(View.VISIBLE);

            try {
                String custName = activeTaskSnapshot.child("customerName").getValue(String.class);
                String ticketId = activeTaskSnapshot.child("ticketId").getValue(String.class);
                String time = activeTaskSnapshot.child("timeRange").getValue(String.class);
                String addr = activeTaskSnapshot.child("address").getValue(String.class);
                String sro = activeTaskSnapshot.child("referenceNo").getValue(String.class);
                String status = activeTaskSnapshot.child("status").getValue(String.class);

                if (sro == null) sro = activeTaskSnapshot.child("sroNumber").getValue(String.class);

                if (tvInProgName != null) tvInProgName.setText(custName != null ? custName : "N/A");
                if (tvInProgTicket != null)
                    tvInProgTicket.setText("#" + (ticketId != null ? ticketId : "---"));
                if (tvInProgTime != null) tvInProgTime.setText(time != null ? time : "---");
                if (tvInProgAddr != null) tvInProgAddr.setText(addr != null ? addr : "---");
                if (tvInProgSro != null) tvInProgSro.setText(sro != null ? sro : "---");

                if (btnActionInProg != null) {
                    if ("Submission".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status)) {
                        btnActionInProg.setText("REPORT");
                        btnActionInProg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFC107")));
                    } else if ("Rejected".equalsIgnoreCase(status)) {
                        btnActionInProg.setText("REPORT");
                        btnActionInProg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#28A745")));
                    } else {
                        btnActionInProg.setText("TRACK");
                        btnActionInProg.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4B91C6")));
                    }
                }

                if (tvInProgService != null) {
                    tvInProgService.setText(getServiceTypeLabel(activeTaskSnapshot));
                }

                Object tsObj = activeTaskSnapshot.child("assignedTimestamp").getValue();
                if (tsObj instanceof Number && statusInProgDate != null) {
                    Date d = new Date(((Number) tsObj).longValue());
                    statusInProgDate.setText(df.format(d) + "\n" + mf.format(d).toUpperCase());
                } else if (statusInProgDate != null) {
                    statusInProgDate.setText("NOW\n---");
                }
            } catch (Exception e) {
                Log.e("UI_UPDATE", "Error updating UI: " + e.getMessage());
            }
        } else {
            if (cardInProg != null) cardInProg.setVisibility(View.GONE);
            if (tvInProgLabel != null) tvInProgLabel.setVisibility(View.GONE);
            if (tvInProgCount != null) tvInProgCount.setVisibility(View.GONE);
            if (underlineInProg != null) underlineInProg.setVisibility(View.GONE);
            if (statusInProgDate != null) statusInProgDate.setVisibility(View.GONE);
        }

        int uC = (upcomingSnapshotsList != null) ? upcomingSnapshotsList.size() : 0;
        if (tvUpcomingCount != null) {
            tvUpcomingCount.setText("(" + uC + ")");
            tvUpcomingCount.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        }
        if (tvUpcomingLabel != null)
            tvUpcomingLabel.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        if (upcomingProgress != null)
            upcomingProgress.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        if (rvUpcoming != null) rvUpcoming.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);

        boolean isEmpty = (!hasActive && uC == 0);
        if (noTicketsLayout != null)
            noTicketsLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (dashboardScroll != null)
            dashboardScroll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String getServiceTypeLabel(DataSnapshot ds) {
        StringBuilder sb = new StringBuilder();

        if (hasQty(ds, "qty_cbc") || hasQty(ds, "qty_sediment") || hasQty(ds, "qty_smsf1") || hasQty(ds, "qty_smsf10")) {
            sb.append("Filter Preventive");
        }

        if (hasQty(ds, "qty_wayvalve")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Installation Kit");
        }

        if (hasQty(ds, "qty_aquatal") || hasQty(ds, "qty_inline") || hasQty(ds, "qty_uvlamp") ||
                hasQty(ds, "qty_touchpanel") || hasQty(ds, "qty_pbcboard")) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Other Parts");
        }

        String result = sb.toString();
        return result.isEmpty() ? "General Maintenance" : result;
    }

    private boolean hasQty(DataSnapshot ds, String key) {
        Object val = ds.child(key).getValue();
        return val instanceof Number && ((Number) val).intValue() > 0;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, SplashActivity.class));
            finishAffinity();
        } else if (id == R.id.nav_performance)
            startActivity(new Intent(this, WorkPerformanceActivity.class));
        else if (id == R.id.nav_tech_profile)
            startActivity(new Intent(this, EditProfileActivity.class));
        else if (id == R.id.nav_admin_panel) {
            String email = getStoredEmail();
            if (email.equals("admin@aquasmartguard.ph") || email.equals("management@aquasmartguard.ph")) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                intent.putExtra("USER_EMAIL", email);
                startActivity(intent);
            }
        } else if (id == R.id.nav_help) startActivity(new Intent(this, HelpCenterActivity.class));
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupClickListeners() {
        btnActionInProg.setOnClickListener(v -> {
            if (activeTaskSnapshot != null) {
                String status = activeTaskSnapshot.child("status").getValue(String.class);
                if ("Rejected".equalsIgnoreCase(status) || "Submission".equalsIgnoreCase(status) || "Submitted".equalsIgnoreCase(status)) {
                    showSubmitReportDialog();
                } else {
                    Intent i = new Intent(this, TechnicianTrackActivity.class);
                    i.putExtra("TICKET_ID", activeTaskSnapshot.getKey());
                    i.putExtra("CUSTOMER_NAME", activeTaskSnapshot.child("customerName").getValue(String.class));
                    startActivity(i);
                }
            }
        });
        if (notifIcon != null)
            notifIcon.setOnClickListener(v -> startActivity(new Intent(this, TechnicianNotificationActivity.class)));
        if (settingIcon != null)
            settingIcon.setOnClickListener(v -> startActivity(new Intent(this, TechnicianSettingsActivity.class)));
        findViewById(R.id.techProfileAvatar).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("TASK_DONE", false)) {
            intent.removeExtra("TASK_DONE");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                proofUri = null;
                currentRemarks = "";
                showSubmitReportDialog();
            }, 1500);
        }
    }

    private void setupFirebaseMessagesListener(String techIdentifier) {
        if (userChatsRef == null) return;
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatThreadsList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String dbTechId = ds.child("techId").getValue(String.class);
                    String dbTechName = ds.child("techName").getValue(String.class);
                    String chatId = ds.getKey();

                    boolean isMine = (dbTechId != null && (dbTechId.equalsIgnoreCase(techIdentifier) || dbTechId.equalsIgnoreCase(currentTechUid))) ||
                            (dbTechName != null && dbTechName.equalsIgnoreCase(currentTechName)) ||
                            (chatId != null && (chatId.contains(techIdentifier) || chatId.contains(currentTechUid)));

                    if (isMine) {
                        chatThreadsList.add(ds);
                    }
                }
                if (threadAdapter == null) {
                    threadAdapter = new ChatThreadAdapter(chatThreadsList, techIdentifier);
                    rvMessageThreads.setLayoutManager(new LinearLayoutManager(TechnicianDashboardActivity.this));
                    rvMessageThreads.setAdapter(threadAdapter);
                } else {
                    threadAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        userChatsRef.addValueEventListener(messagesListener);
    }

    private void setupDashboardNotificationPopup() {
        if (notifRef == null) return;
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot s, @Nullable String p) {
                if (!isInitialLoad) {
                    NotificationModel n = s.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null) {
                        if ("CALL".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground)
                                showIncomingCallPopup(n.getMessage(), n.getTicketId());
                            s.getRef().removeValue();
                        } else if ("REJECTED".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground)
                                showResubmitPopup(n.getMessage(), n.getTicketId());
                        } else if ("APPROVED".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground) {
                                new AlertDialog.Builder(TechnicianDashboardActivity.this)
                                        .setTitle("Report Approved!")
                                        .setMessage(Html.fromHtml(n.getMessage()))
                                        .setPositiveButton("OK", null)
                                        .show();
                            }

                        } else if (isActivityInForeground) {
                            new AlertDialog.Builder(TechnicianDashboardActivity.this)
                                    .setTitle("New Notification")
                                    .setMessage(Html.fromHtml(n.getMessage()))
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot s) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        notifRef.addChildEventListener(notifListener);
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                isInitialLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    private void showIncomingCallPopup(String message, String customerId) {
        if (isFinishing() || isDestroyed() || notificationContainer == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.layout_incoming_call_popup, notificationContainer, false);

        TextView tvName = layout.findViewById(R.id.tvCallerName);
        ImageView btnAnswer = layout.findViewById(R.id.btnAnswerCall);
        ImageView btnDecline = layout.findViewById(R.id.btnDeclineCall);
        ImageView ivAvatar = layout.findViewById(R.id.ivCallerAvatar);

        ivAvatar.setImageResource(R.drawable.man_customer_icon);
        tvName.setText("Customer Request");

        FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(customerId.trim()).child("fullName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null && tvName != null) tvName.setText(name);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        btnAnswer.setOnClickListener(v -> {
            notificationContainer.removeAllViews();
            String chatId = currentTechUid + "_" + customerId.trim();
            FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("active");
            FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStartTime").setValue(ServerValue.TIMESTAMP);

            Intent intent = new Intent(this, VoiceCallActivity.class);
            intent.putExtra("TECH_ID", currentTechUid);
            intent.putExtra("CUSTOMER_ID", customerId.trim());
            startActivity(intent);
        });

        btnDecline.setOnClickListener(v -> {
            notificationContainer.removeAllViews();
            String chatId = currentTechUid + "_" + customerId.trim();
            FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
        });

        notificationContainer.removeAllViews();
        notificationContainer.addView(layout);
    }

    private void showResubmitPopup(String message, String ticketId) {
        if (isFinishing() || isDestroyed() || notificationContainer == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        View layout = inflater.inflate(R.layout.layout_resubmit_report_popup, notificationContainer, false);
        TextView tvMsg = layout.findViewById(R.id.tvResubmitMessage);
        Button btnOpen = layout.findViewById(R.id.btnOpenReport);
        Button btnDismiss = layout.findViewById(R.id.btnDismiss);

        if (tvMsg != null) tvMsg.setText(message);
        btnOpen.setOnClickListener(v -> {
            notificationContainer.removeAllViews();
            showSubmitReportDialog();
        });
        btnDismiss.setOnClickListener(v -> notificationContainer.removeAllViews());

        notificationContainer.removeAllViews();
        notificationContainer.addView(layout);
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void launchCamera() {
        try {
            File tempFile = File.createTempFile("PROOF_" + System.currentTimeMillis(), ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            proofUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", tempFile);
            cameraLauncher.launch(proofUri);
        } catch (IOException e) {
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            launchCamera();
        }
    }

    private void showSubmitReportDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (reportDialog != null && reportDialog.isShowing()) {
            ImageView ivPreview = reportDialog.findViewById(R.id.imgProofPreview);
            if (ivPreview != null && proofUri != null) {
                ivPreview.setImageTintList(null);
                Glide.with(this).load(proofUri).signature(new ObjectKey(System.currentTimeMillis())).into(ivPreview);
            }
            return;
        }

        reportDialog = new Dialog(this);
        reportDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        reportDialog.setContentView(R.layout.dialog_submit_report);
        Window window = reportDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(android.view.Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivPreview = reportDialog.findViewById(R.id.imgProofPreview);
        EditText etRemarks = reportDialog.findViewById(R.id.etReportRemarks);

        if (etRemarks != null) etRemarks.setText(currentRemarks);

        if (ivPreview != null) {
            if (proofUri != null) {
                ivPreview.setImageTintList(null);
                Glide.with(this).load(proofUri).signature(new ObjectKey(System.currentTimeMillis())).into(ivPreview);
            } else {
                ivPreview.setImageResource(R.drawable.upload_file_png);
            }
        }

        reportDialog.findViewById(R.id.btnUploadProof).setOnClickListener(v -> {
            if (etRemarks != null) currentRemarks = etRemarks.getText().toString();
            new AlertDialog.Builder(this).setTitle("Add Proof").setItems(new String[]{"Take Photo", "Gallery"}, (d, which) -> {
                if (which == 0) checkCameraPermission();
                else galleryLauncher.launch("image/*");
            }).show();
        });

        reportDialog.findViewById(R.id.btnSubmitReport).setOnClickListener(v -> {
            if (proofUri == null) {
                Toast.makeText(this, "Add photo first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (activeTaskSnapshot == null) {
                Toast.makeText(this, "No active ticket found to submit report for", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Uploading...");
            pd.show();

            String fileName = "report_" + System.currentTimeMillis() + ".jpg";
            StorageReference fileRef = storageRef.child("Reports/" + fileName);
            fileRef.putFile(proofUri)
                    .addOnSuccessListener(ts -> {
                        if (isFinishing() || isDestroyed()) return;
                        ts.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                            if (isFinishing() || isDestroyed()) return;
                            saveReportToDatabase(uri.toString(), etRemarks.getText().toString(), pd);
                        });
                    })
                    .addOnFailureListener(e -> {
                        if (!isFinishing() && !isDestroyed()) {
                            try {
                                pd.dismiss();
                            } catch (Exception ignored) {
                            }
                            Toast.makeText(TechnicianDashboardActivity.this, "Report submitted. Waiting for admin acceptance.", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        reportDialog.findViewById(R.id.btnCloseReport).setOnClickListener(v -> reportDialog.dismiss());
        reportDialog.show();
    }

    private void saveReportToDatabase(String downloadUrl, String remarks, ProgressDialog pd) {
        if (activeTaskSnapshot == null) return;
        String ticketId = activeTaskSnapshot.getKey();
        String ticketNum = activeTaskSnapshot.child("ticketId").getValue(String.class);

        DatabaseReference dbReportRef = FirebaseDatabase.getInstance(DB_URL).getReference("Reports").push();
        HashMap<String, Object> data = new HashMap<>();
        data.put("techUid", currentTechUid);
        data.put("techName", currentTechName);
        data.put("customerName", activeTaskSnapshot.child("customerName").getValue(String.class));
        data.put("remarks", remarks);
        data.put("proofUrl", downloadUrl);
        data.put("ticketId", ticketId);
        data.put("ticketNum", ticketNum);
        data.put("timestamp", ServerValue.TIMESTAMP);

        dbReportRef.setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful() && ticketId != null) {
                if (requestsRef != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "Submitted");
                    updates.put("technicianRemarks", remarks);
                    updates.put("proofImageUrl", downloadUrl);
                    updates.put("submissionTimestamp", ServerValue.TIMESTAMP);

                    requestsRef.child(ticketId).updateChildren(updates).addOnCompleteListener(t -> {
                        if (isFinishing() || isDestroyed()) {
                            try { pd.dismiss(); } catch (Exception ignored) {}
                            return;
                        }
                        runOnUiThread(() -> {
                            try { pd.dismiss(); } catch (Exception ignored) {}
                            AdminDashboardActivity.addAdminLog("Technician " + currentTechName + " submitted a report for ticket #" + ticketNum);
                            try {
                                if (reportDialog != null && reportDialog.isShowing()) {
                                    reportDialog.dismiss();
                                }
                            } catch (Exception ignored) {}
                            proofUri = null;
                            currentRemarks = "";
                            activeTaskSnapshot = null;
                            updateUI();
                            Toast.makeText(TechnicianDashboardActivity.this, "Report Submitted Successfully!", Toast.LENGTH_LONG).show();
                        });
                    });
                } else { try { pd.dismiss(); } catch (Exception ignored) {} }
            } else { try { pd.dismiss(); } catch (Exception ignored) {} }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
        if (requestsRef != null && tasksListener != null) {
            requestsRef.removeEventListener(tasksListener);
        }
        if (userChatsRef != null && messagesListener != null) {
            userChatsRef.removeEventListener(messagesListener);
        }
        if (reportDialog != null && reportDialog.isShowing()) {
            reportDialog.dismiss();
        }
    }
}
