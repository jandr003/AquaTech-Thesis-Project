package com.example.aquatech;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Import External Modular Helper Classes
import com.example.aquatech.TechnicianIdentityHelper;
import com.example.aquatech.TechnicianChatAutomation;
import com.example.aquatech.TechnicianReportDialogHelper;
import com.example.aquatech.TechnicianCallHandler;
import com.example.aquatech.TechnicianJobAcceptor;
import com.example.aquatech.TechnicianNotificationPopupHelper;

/**
 * TechnicianDashboardActivity
 * Clean, Lean, High-Performance Field Technician Dashboard Activity.
 * Strict 600-700 lines target maintained (~630 lines).
 * Delegated to external helpers:
 * - TechnicianIdentityHelper
 * - TechnicianChatAutomation
 * - TechnicianReportDialogHelper
 * - TechnicianCallHandler
 * - TechnicianJobAcceptor
 * - TechnicianNotificationPopupHelper
 */
public class TechnicianDashboardActivity extends AppCompatActivity {

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

    private RecyclerView rvUpcoming;
    private UpcomingTaskAdapter upcomingAdapter;

    private DataSnapshot activeTaskSnapshot;
    private final List<DataSnapshot> upcomingSnapshotsList = new ArrayList<>();

    private String currentTechName = "";
    private String currentTechUid = "";
    private DatabaseReference notifRef, requestsRef, userChatsRef;
    private FirebaseAuth mAuth;
    private boolean isInitialLoad = true;
    private boolean isActivityInForeground = false;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private Uri proofUri;
    private String currentRemarks = "";

    private ChildEventListener notifListener;
    private ValueEventListener tasksListener;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    proofUri = uri;
                    showSubmitReportDialog();
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success) showSubmitReportDialog();
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) launchCamera();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_dashboard);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        setupUpcomingRecyclerView();

        FirebaseUser user = mAuth.getCurrentUser();
        String currentEmail = TechnicianIdentityHelper.getStoredEmail(getIntent(), mAuth);

        TechnicianIdentityHelper.fetchTechnicianIdentity(
                DB_URL,
                user != null ? user.getUid() : "",
                currentEmail,
                (resolvedUid, resolvedName) -> {
                    currentTechUid = resolvedUid;
                    currentTechName = resolvedName;
                    TechnicianIdentityHelper.attachLiveAvatarListener(DB_URL, resolvedUid, findViewById(R.id.techProfileAvatar));
                    initializeFirebaseListeners(resolvedUid);
                }
        );

        setupClickListeners();
        handleIntent(getIntent());
        setupStatusBar();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsRef != null && tasksListener != null) requestsRef.removeEventListener(tasksListener);
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
    }

    private void initializeViews() {
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

    private void setupClickListeners() {
        if (btnActionInProg != null) {
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
        }
        if (notifIcon != null) {
            notifIcon.setOnClickListener(v -> startActivity(new Intent(this, TechnicianNotificationActivity.class)));
        }
        if (settingIcon != null) {
            settingIcon.setOnClickListener(v -> startActivity(new Intent(this, TechnicianSettingsActivity.class)));
        }
        View avatarView = findViewById(R.id.techProfileAvatar);
        if (avatarView != null) {
            avatarView.setOnClickListener(v -> startActivity(new Intent(this, TechnicianProfileActivity.class)));
        }

        BottomNavigationView techBottomNav = findViewById(R.id.techBottomNav);
        if (techBottomNav != null) {
            techBottomNav.setSelectedItemId(R.id.nav_tech_dashboard);
            techBottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_tech_performance) {
                    Intent intent = new Intent(this, WorkPerformanceActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.nav_tech_messages) {
                    startActivity(new Intent(this, CustomerChatListActivity.class));
                    return true;
                } else if (id == R.id.nav_tech_profile) {
                    startActivity(new Intent(this, TechnicianProfileActivity.class));
                    return true;
                }
                return true;
            });
        }
    }

    private void initializeFirebaseListeners(String techIdentifier) {
        if (techIdentifier == null || techIdentifier.isEmpty()) return;

        if (requestsRef != null && tasksListener != null) requestsRef.removeEventListener(tasksListener);
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);

        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications").child(techIdentifier);
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        userChatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");

        setupFirebaseTasksListener(techIdentifier);
        setupDashboardNotificationPopup();
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
                                TechnicianNotificationPopupHelper.showResubmitPopup(TechnicianDashboardActivity.this, notificationContainer, "Report Rejected - Tap to resubmit", TechnicianDashboardActivity.this::showSubmitReportDialog);
                            }

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
        TechnicianJobAcceptor.acceptTask(
                this,
                DB_URL,
                activeTaskSnapshot,
                task,
                currentTechUid,
                currentTechName,
                this::updateUI
        );
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
                if (tvInProgTicket != null) tvInProgTicket.setText(String.format("#%s", ticketId != null ? ticketId : "---"));
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
                    statusInProgDate.setText(toVerticalDateString(d));
                } else if (statusInProgDate != null) {
                    statusInProgDate.setText("N\nO\nW");
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
            tvUpcomingCount.setText(String.format(Locale.getDefault(), "(%d)", uC));
            tvUpcomingCount.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        }
        if (tvUpcomingLabel != null) tvUpcomingLabel.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        if (upcomingProgress != null) upcomingProgress.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);
        if (rvUpcoming != null) rvUpcoming.setVisibility(uC > 0 ? View.VISIBLE : View.GONE);

        boolean isEmpty = (!hasActive && uC == 0);
        if (noTicketsLayout != null) noTicketsLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (dashboardScroll != null) dashboardScroll.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private CharSequence toVerticalDateString(Date d) {
        String day = "12";
        String month = "DECEMBER";
        if (d != null) {
            SimpleDateFormat df = new SimpleDateFormat("dd", Locale.getDefault());
            SimpleDateFormat mf = new SimpleDateFormat("MMMM", Locale.getDefault());
            day = df.format(d);
            month = mf.format(d).toUpperCase(Locale.ROOT);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(day);
        sb.append("\n");
        for (int i = 0; i < month.length(); i++) {
            sb.append("\n");
            sb.append(month.charAt(i));
        }

        String fullText = sb.toString();
        SpannableString spannable = new SpannableString(fullText);

        spannable.setSpan(new AbsoluteSizeSpan(20, true), 0, day.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, day.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (fullText.length() > day.length()) {
            spannable.setSpan(new AbsoluteSizeSpan(11, true), day.length() + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), day.length() + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannable;
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

    private void setupDashboardNotificationPopup() {
        if (notifRef == null) return;
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot s, @Nullable String p) {
                if (!isInitialLoad) {
                    NotificationModel n = s.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null) {
                        if ("CALL".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground) {
                                TechnicianCallHandler.showIncomingCallPopup(TechnicianDashboardActivity.this, DB_URL, n.getTicketId(), currentTechUid);
                            }
                            s.getRef().removeValue();
                        } else if ("REJECTED".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground) {
                                TechnicianNotificationPopupHelper.showResubmitPopup(TechnicianDashboardActivity.this, notificationContainer, n.getMessage(), TechnicianDashboardActivity.this::showSubmitReportDialog);
                            }
                        } else if ("APPROVED".equalsIgnoreCase(n.getType())) {
                            if (isActivityInForeground) {
                                new AlertDialog.Builder(TechnicianDashboardActivity.this)
                                        .setTitle("Report Approved!")
                                        .setMessage(Html.fromHtml(n.getMessage(), Html.FROM_HTML_MODE_LEGACY))
                                        .setPositiveButton("OK", null)
                                        .show();
                            }

                        } else if (isActivityInForeground) {
                            new AlertDialog.Builder(TechnicianDashboardActivity.this)
                                    .setTitle("New Notification")
                                    .setMessage(Html.fromHtml(n.getMessage(), Html.FROM_HTML_MODE_LEGACY))
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
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
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                isInitialLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        });
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra("TASK_DONE", false)) {
            intent.removeExtra("TASK_DONE");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isFinishing() || isDestroyed()) return;
                proofUri = null;
                currentRemarks = "";
                showSubmitReportDialog();
            }, 1200);
        } else if (intent.hasExtra("RESUBMIT_TICKET_ID")) {
            String resubmitTicketId = intent.getStringExtra("RESUBMIT_TICKET_ID");
            intent.removeExtra("RESUBMIT_TICKET_ID");
            if (resubmitTicketId != null && !resubmitTicketId.isEmpty()) {
                TechnicianNotificationPopupHelper.showResubmitPopup(this, notificationContainer, "Report Rejected - Tap to resubmit", this::showSubmitReportDialog);
            }
        }
    }

    private void launchCamera() {
        try {
            File tempFile = File.createTempFile("PROOF_" + System.currentTimeMillis(), ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            proofUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", tempFile);
            cameraLauncher.launch(proofUri);
        } catch (IOException ignored) {
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
        TechnicianReportDialogHelper.showSubmitReportDialog(
                this,
                DB_URL,
                activeTaskSnapshot,
                currentTechUid,
                currentTechName,
                proofUri,
                currentRemarks,
                () -> new AlertDialog.Builder(this).setTitle("Add Proof").setItems(new String[]{"Take Photo", "Gallery"}, (d, which) -> {
                    if (which == 0) checkCameraPermission();
                    else galleryLauncher.launch("image/*");
                }).show(),
                () -> {
                    proofUri = null;
                    currentRemarks = "";
                    activeTaskSnapshot = null;
                    updateUI();
                }
        );
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
