package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CustomerDetailActivity extends AppCompatActivity {

    private ImageView ivCustAvatar;
    private TextView tvCustName, tvCustPhone, tvCustAddress, tvCustTicketId, tvCustUnit, tvCustSchedule;
    private RecyclerView rvSharedMedia;
    private TextView tvEmptySharedMedia;
    private SharedMediaAdapter sharedMediaAdapter;
    private List<ChatMessage> sharedMediaList;
    private String customerId, techId;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        customerId = getIntent().getStringExtra("CUSTOMER_ID");
        techId = getIntent().getStringExtra("TECH_ID");

        setupStatusBar();
        initializeViews();
        loadCustomerProfileData();
        loadActiveTicketData();
        setupSharedMedia();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackCustDetail);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                finish();
            });
        }

        ivCustAvatar = findViewById(R.id.ivCustDetailAvatar);
        tvCustName = findViewById(R.id.tvCustDetailName);
        tvCustPhone = findViewById(R.id.tvCustDetailPhone);
        tvCustAddress = findViewById(R.id.tvCustDetailAddress);
        tvCustTicketId = findViewById(R.id.tvCustDetailTicketId);
        tvCustUnit = findViewById(R.id.tvCustDetailUnit);
        tvCustSchedule = findViewById(R.id.tvCustDetailSchedule);

        String intentName = getIntent().getStringExtra("CUSTOMER_NAME");
        if (intentName != null && !intentName.isEmpty()) {
            if (tvCustName != null) {
                tvCustName.setText(intentName);
            }
        }
    }

    private void loadCustomerProfileData() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(DB_URL).getReference("Users").child(customerId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name == null || name.isEmpty()) {
                        name = snapshot.child("name").getValue(String.class);
                    }

                    if (name != null && !name.isEmpty() && tvCustName != null) {
                        tvCustName.setText(name);
                    }

                    String phone = snapshot.child("phone").getValue(String.class);
                    if (phone == null || phone.isEmpty()) {
                        phone = snapshot.child("phoneNumber").getValue(String.class);
                    }

                    if (phone != null && !phone.isEmpty() && tvCustPhone != null) {
                        tvCustPhone.setText(phone);
                    }

                    String address = snapshot.child("address").getValue(String.class);
                    if (address != null && !address.isEmpty() && tvCustAddress != null) {
                        tvCustAddress.setText(address);
                    }

                    int defaultAvatar = getCustomerAvatarDrawable(customerId);
                    String avatarUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    if (avatarUrl != null && !avatarUrl.isEmpty() && ivCustAvatar != null) {
                        Glide.with(CustomerDetailActivity.this).load(avatarUrl).placeholder(defaultAvatar).into(ivCustAvatar);
                    } else if (ivCustAvatar != null) {
                        ivCustAvatar.setImageResource(defaultAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadActiveTicketData() {
        if (customerId == null || customerId.isEmpty()) {
            return;
        }

        DatabaseReference reqRef = FirebaseDatabase.getInstance(DB_URL).getReference("ServiceRequests");
        Query query = reqRef.orderByChild("userId").equalTo(customerId).limitToLast(1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String refNo = ds.child("referenceNo").getValue(String.class);
                        if (refNo == null || refNo.isEmpty()) {
                            refNo = ds.getKey();
                        }

                        if (refNo != null && tvCustTicketId != null) {
                            String cleanRef = refNo.startsWith("SRO-") ? refNo.substring(4) : refNo;
                            tvCustTicketId.setText(cleanRef);
                        }

                        String unitModel = ds.child("unitModel").getValue(String.class);
                        if (unitModel != null && !unitModel.isEmpty() && tvCustUnit != null) {
                            tvCustUnit.setText(unitModel);
                        }

                        String date = ds.child("date").getValue(String.class);
                        String timeRange = ds.child("timeRange").getValue(String.class);

                        if (tvCustSchedule != null) {
                            String displaySched = (date != null ? date : "") + (timeRange != null ? " | " + timeRange : "");
                            tvCustSchedule.setText(displaySched.isEmpty() ? "Scheduled Appointment" : displaySched);
                        }

                        String reqAddress = ds.child("address").getValue(String.class);
                        if (reqAddress != null && !reqAddress.isEmpty() && tvCustAddress != null) {
                            tvCustAddress.setText(reqAddress);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupSharedMedia() {
        rvSharedMedia = findViewById(R.id.rvCustSharedMedia);
        tvEmptySharedMedia = findViewById(R.id.tvEmptyCustSharedMedia);

        View chipMedia = findViewById(R.id.chipCustMedia);
        View chipFiles = findViewById(R.id.chipCustFiles);
        View chipLinks = findViewById(R.id.chipCustLinks);

        View.OnClickListener openFullSharedMedia = v -> {
            String myUid = FirebaseAuth.getInstance().getUid();
            String chatId = (myUid != null && customerId != null) ? myUid + "_" + customerId : null;

            Intent intent = new Intent(CustomerDetailActivity.this, GroupSharedMediaActivity.class);
            if (chatId != null) {
                intent.putExtra("CHAT_ID", chatId);
            }
            startActivity(intent);
        };

        if (chipMedia != null) {
            chipMedia.setOnClickListener(openFullSharedMedia);
        }
        if (chipFiles != null) {
            chipFiles.setOnClickListener(openFullSharedMedia);
        }
        if (chipLinks != null) {
            chipLinks.setOnClickListener(openFullSharedMedia);
        }

        if (rvSharedMedia != null) {
            sharedMediaList = new ArrayList<>();
            sharedMediaAdapter = new SharedMediaAdapter(sharedMediaList);
            rvSharedMedia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvSharedMedia.setAdapter(sharedMediaAdapter);

            loadSharedMediaData();
        }
    }

    private void loadSharedMediaData() {
        String myUid = FirebaseAuth.getInstance().getUid();
        if (customerId == null || myUid == null) {
            return;
        }

        String chatId1 = myUid + "_" + customerId;
        String chatId2 = customerId + "_" + myUid;

        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId1).child("messages");
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (sharedMediaList != null) {
                    sharedMediaList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ChatMessage msg = ds.getValue(ChatMessage.class);
                        if (msg != null && (msg.getFileUri() != null || msg.getFileUrl() != null)) {
                            sharedMediaList.add(msg);
                        }
                    }
                }

                if (sharedMediaList != null && sharedMediaList.isEmpty()) {
                    DatabaseReference chatRef2 = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats").child(chatId2).child("messages");
                    chatRef2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap2) {
                            if (snap2.exists() && sharedMediaList != null) {
                                for (DataSnapshot ds2 : snap2.getChildren()) {
                                    ChatMessage msg2 = ds2.getValue(ChatMessage.class);
                                    if (msg2 != null && (msg2.getFileUri() != null || msg2.getFileUrl() != null)) {
                                        sharedMediaList.add(msg2);
                                    }
                                }
                            }

                            if (tvEmptySharedMedia != null && sharedMediaList != null) {
                                tvEmptySharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.VISIBLE : View.GONE);
                            }

                            if (rvSharedMedia != null && sharedMediaList != null) {
                                rvSharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.GONE : View.VISIBLE);
                            }

                            if (sharedMediaAdapter != null) {
                                sharedMediaAdapter.notifyDataSetChanged();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
                } else {
                    if (tvEmptySharedMedia != null && sharedMediaList != null) {
                        tvEmptySharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    if (rvSharedMedia != null && sharedMediaList != null) {
                        rvSharedMedia.setVisibility(sharedMediaList.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    if (sharedMediaAdapter != null) {
                        sharedMediaAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
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
}
