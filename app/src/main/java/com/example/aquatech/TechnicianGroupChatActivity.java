package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TechnicianGroupChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, btnBack;
    private LinearLayout emptyLayout;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private DatabaseReference groupChatRef;
    private String currentTechUid = "";
    private String currentTechName = "";
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private ChildEventListener messageListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_group_chat);

        setupStatusBar();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Access Denied: Technicians Only", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentTechUid = user.getUid();
        String email = TechnicianIdentityHelper.getStoredEmail(getIntent(), FirebaseAuth.getInstance());
        currentTechName = TechnicianIdentityHelper.resolveTechNameFromEmail(email);

        initializeViews();

        groupChatRef = FirebaseDatabase.getInstance(DB_URL).getReference("GroupChats").child("TechniciansTeam").child("messages");
        loadGroupMessages();
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAndSendAttachment(uri, "image");
            });

    private final ActivityResultLauncher<String[]> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) uploadAndSendAttachment(uri, "document");
            });

    private void initializeViews() {
        rvMessages = findViewById(R.id.rvTeamGroupMessages);
        etMessage = findViewById(R.id.etTeamMessage);
        btnSend = findViewById(R.id.btnSendTeamMessage);
        btnBack = findViewById(R.id.btnBackTeamChat);
        emptyLayout = findViewById(R.id.emptyTeamChatLayout);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendTeamMessage());

        View btnFile = findViewById(R.id.btnAttachFile);
        if (btnFile != null) btnFile.setOnClickListener(v -> fileLauncher.launch(new String[]{"*/*"}));

        View btnGallery = findViewById(R.id.btnAttachGallery);
        if (btnGallery != null) btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(messageList, ChatAdapter.MODE_TECH_CHAT);
        rvMessages.setAdapter(chatAdapter);
    }

    private void loadGroupMessages() {
        messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage msg = snapshot.getValue(ChatMessage.class);
                if (msg != null) {
                    messageList.add(msg);
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
                if (emptyLayout != null) emptyLayout.setVisibility(messageList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        groupChatRef.addChildEventListener(messageListener);
    }

    private void sendTeamMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessage.setText("");

        DatabaseReference newMsgRef = groupChatRef.push();
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("senderId", currentTechUid);
        msgMap.put("senderName", currentTechName);
        msgMap.put("message", text);
        msgMap.put("timestamp", ServerValue.TIMESTAMP);

        newMsgRef.setValue(msgMap);
    }

    private void uploadAndSendAttachment(Uri uri, String type) {
        if (uri == null) return;
        Toast.makeText(this, "Uploading attachment...", Toast.LENGTH_SHORT).show();

        String fileName = "team_file_" + System.currentTimeMillis();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("GroupChats/TechniciansTeam/" + fileName);

        ref.putFile(uri).addOnSuccessListener(taskSnapshot -> taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(downloadUrl -> {
            DatabaseReference newMsgRef = groupChatRef.push();
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("senderId", currentTechUid);
            msgMap.put("senderName", currentTechName);
            msgMap.put("message", "Attached " + (type.contains("image") ? "Image" : "Document") + ": " + downloadUrl.toString());
            msgMap.put("timestamp", ServerValue.TIMESTAMP);
            newMsgRef.setValue(msgMap);
            Toast.makeText(TechnicianGroupChatActivity.this, "Attachment Sent!", Toast.LENGTH_SHORT).show();
        })).addOnFailureListener(e -> Toast.makeText(TechnicianGroupChatActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupChatRef != null && messageListener != null) {
            groupChatRef.removeEventListener(messageListener);
        }
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
