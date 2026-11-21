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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

    private TextView customerUserName, refValue, unitValue, unitName;
    private ImageView imgDispenser;
    private ImageView editPen, settingIcon, notificationIcon, customerIcon;
    private FrameLayout notificationContainer;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef, requestsRef, notifRef;
    private Query monitorQuery;
    private ValueEventListener monitorListener, userListener;
    private ChildEventListener notifListener;

    private String assignedTechName = "Technician";
    private String assignedTechId = null;
    private String currentRequestStatus = null;
    private String currentTicketId = null;
    private boolean hasActiveRequest = false;
    private boolean isInitialRequestLoad = true;
    private boolean isInitialNotifLoad = true;
    private boolean isActivityInForeground = false;
    private boolean hasRated = false;

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private final ActivityResultLauncher<Intent> unitSelectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String uName = data.getStringExtra("unit_name");
                    String serial = data.getStringExtra("serial_no");
                    int unitImg = data.getIntExtra("image_res", R.drawable.titled_design1);
                    if (unitValue != null) {
                        unitValue.setText(uName);
                    }
                    if (unitName != null) {
                        unitName.setText(serial);
                    }
                    if (imgDispenser != null) {
                        imgDispenser.setImageResource(unitImg);
                    }
                    saveUnitToFirebase(uName, serial);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        requestsRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");

        setupStatusBar();
        initializeViews();
        clearSampleData();
        setupFirebaseListener();
        handleIncomingSetupData();
        setupClickListeners();
        setupServiceRequestMonitor();
        setupNotificationMonitor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityInForeground = true;
        reloadUserProfile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityInForeground = false;
    }

    private void reloadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference profileRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Users").child(user.getUid());

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null) {
                        name = snapshot.child("name").getValue(String.class);
                    }
                    if (name != null && customerUserName != null) {
                        customerUserName.setText(name);
                    }

                    String rootUnit = snapshot.child("unitName").getValue(String.class);
                    String rootSerial = snapshot.child("serialNo").getValue(String.class);
                    String rootRef = snapshot.child("referenceNo").getValue(String.class);

                    if (rootUnit != null) {
                        if (unitValue != null) {
                            unitValue.setText(rootUnit);
                        }
                        if (unitName != null) {
                            unitName.setText(rootSerial != null ? rootSerial : "---");
                        }
                        if (refValue != null) {
                            refValue.setText(rootRef != null ? rootRef : "---");
                        }
                        if (imgDispenser != null) {
                            imgDispenser.setImageResource(getImageForUnit(rootUnit));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        notificationContainer = findViewById(R.id.notificationContainer);

        CustomerDashboardNavHelper.setupBottomNavigation(this);
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
                        if (name == null) {
                            name = snapshot.child("name").getValue(String.class);
                        }
                        if (name != null && customerUserName != null) {
                            customerUserName.setText(name);
                        }

                        String rootUnit = snapshot.child("unitName").getValue(String.class);
                        String rootSerial = snapshot.child("serialNo").getValue(String.class);
                        String rootRef = snapshot.child("referenceNo").getValue(String.class);

                        if (rootUnit != null) {
                            if (unitValue != null) {
                                unitValue.setText(rootUnit);
                            }
                            if (unitName != null) {
                                unitName.setText(rootSerial != null ? rootSerial : "---");
                            }
                            if (refValue != null) {
                                refValue.setText(rootRef != null ? rootRef : "---");
                            }
                            if (imgDispenser != null) {
                                imgDispenser.setImageResource(getImageForUnit(rootUnit));
                            }
                        } else {
                            DataSnapshot unitsFolder = snapshot.child("units");
                            if (unitsFolder.exists() && unitsFolder.hasChildren()) {
                                DataSnapshot lastUnit = null;
                                for (DataSnapshot u : unitsFolder.getChildren()) {
                                    lastUnit = u;
                                }
                                if (lastUnit != null) {
                                    String uLabel = lastUnit.child("unitName").getValue(String.class);
                                    String uSerial = lastUnit.child("serialNo").getValue(String.class);
                                    String sro = lastUnit.child("referenceNo").getValue(String.class);

                                    if (unitValue != null) {
                                        unitValue.setText(uLabel != null ? uLabel : "---");
                                    }
                                    if (unitName != null) {
                                        unitName.setText(uSerial != null ? uSerial : "---");
                                    }
                                    if (refValue != null) {
                                        refValue.setText(sro != null ? sro : "---");
                                    }
                                    if (imgDispenser != null && uLabel != null) {
                                        imgDispenser.setImageResource(getImageForUnit(uLabel));
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            userRef.addValueEventListener(userListener);
        }
    }

    private void clearSampleData() {
        if (customerUserName != null) {
            customerUserName.setText("Loading...");
        }
        if (refValue != null) {
            refValue.setText("---");
        }
        if (unitValue != null) {
            unitValue.setText("---");
        }
        if (unitName != null) {
            unitName.setText("---");
        }
        if (imgDispenser != null) {
            imgDispenser.setImageDrawable(null);
        }
    }

    private void handleIncomingSetupData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("UNIT_NO")) {
            String uName = intent.getStringExtra("UNIT_NAME");
            String uNo = intent.getStringExtra("UNIT_NO");
            String serialTemplate = getTemplateForUnit(uName);
            String finalSerial = serialTemplate.replace("**-***", uNo != null ? uNo : "");

            if (unitValue != null) {
                unitValue.setText(uName);
            }
            if (unitName != null) {
                unitName.setText(finalSerial);
            }

            int imgRes = getImageForUnit(uName);
            if (imgDispenser != null) {
                imgDispenser.setImageResource(imgRes);
            }

            saveUnitToFirebase(uName, finalSerial);
        }
    }

    private void setupServiceRequestMonitor() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || requestsRef == null) {
            return;
        }

        monitorQuery = requestsRef.orderByChild("userId").equalTo(user.getUid());
        monitorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                String oldStatus = currentRequestStatus;
                hasActiveRequest = false;
                assignedTechId = null;
                assignedTechName = "Technician";
                currentRequestStatus = null;
                currentTicketId = null;

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String status = ds.child("status").getValue(String.class);
                        if (status == null) {
                            continue;
                        }

                        boolean isFinished = status.equalsIgnoreCase("Completed") ||
                                status.equalsIgnoreCase("Cancelled") ||
                                status.equalsIgnoreCase("Rejected");

                        if (!isFinished) {
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
                                currentTicketId = ds.getKey();
                                String tName = ds.child("assignedTechName").getValue(String.class);
                                String tId = ds.child("assignedTechId").getValue(String.class);
                                showRatingDialog(currentTicketId, tName, tId);
                            }
                        }
                    }
                }

                isInitialRequestLoad = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        if (monitorQuery != null && monitorListener != null) {
            monitorQuery.addValueEventListener(monitorListener);
        }
    }

    private void showTopNotificationPopup(String techName) {
        CustomerDashboardNotificationPopupHelper.showTopNotificationPopup(
                this,
                notificationContainer,
                techName,
                hasActiveRequest,
                currentTicketId
        );
    }

    private void showRatingDialog(String ticketId, String techName, String techId) {
        if (hasRated || isFinishing() || isDestroyed()) {
            return;
        }
        hasRated = true;

        CustomerRatingDialogHelper.showRatingDialog(
                this,
                DB_URL,
                ticketId,
                techName,
                techId,
                () -> hasRated = false
        );
    }

    private void setupNotificationMonitor() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        notifRef = FirebaseDatabase.getInstance(DB_URL).getReference("CustomerNotifications").child(user.getUid());
        notifListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (!isInitialNotifLoad) {
                    NotificationModel n = snapshot.getValue(NotificationModel.class);
                    if (n != null && n.getMessage() != null) {
                        String type = n.getType();
                        if ("CALL".equalsIgnoreCase(type)) {
                            if (isActivityInForeground) {
                                showIncomingCallPopup(n.getMessage(), n.getTicketId());
                            }
                            snapshot.getRef().removeValue();
                        } else if ("CHAT".equalsIgnoreCase(type) || "ACCEPTED".equalsIgnoreCase(type)) {
                            snapshot.getRef().removeValue();
                        }
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

    private void showIncomingCallPopup(String message, String callerTechId) {
        CustomerDashboardNotificationPopupHelper.showIncomingCallPopup(
                this,
                DB_URL,
                message,
                callerTechId,
                assignedTechId,
                assignedTechName,
                mAuth != null ? mAuth.getUid() : null
        );
    }

    private String getTemplateForUnit(String name) {
        return CustomerDashboardAlertHelper.getTemplateForUnit(name);
    }

    private int getImageForUnit(String name) {
        return CustomerDashboardAlertHelper.getImageForUnit(name);
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
        if (customerIcon != null) {
            customerIcon.setOnClickListener(v -> showProfileMenu());
        }

        if (settingIcon != null) {
            settingIcon.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        }

        View btnChangeUnit = findViewById(R.id.changeUnitButton);
        if (btnChangeUnit != null) {
            btnChangeUnit.setOnClickListener(v -> unitSelectionLauncher.launch(new Intent(this, UnitSelectionActivity.class)));
        }

        View btnReq = findViewById(R.id.requestButton);
        if (btnReq != null) {
            btnReq.setOnClickListener(v -> {
                if (hasActiveRequest) {
                    CustomerDashboardAlertHelper.showActiveRequestAlert(this);
                } else {
                    startActivity(new Intent(this, ServiceRequestActivity.class));
                }
            });
        }

        View btnAssistance = findViewById(R.id.requestAssistanceButton);
        if (btnAssistance != null) {
            btnAssistance.setOnClickListener(v -> startActivity(new Intent(this, AquaBuddyActivity.class)));
        }
    }

    private void showProfileMenu() {
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void logoutUser() {
        if (mAuth != null) {
            mAuth.signOut();
        }
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
        if (monitorQuery != null && monitorListener != null) {
            monitorQuery.removeEventListener(monitorListener);
        }
        if (notifRef != null && notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
    }
}
