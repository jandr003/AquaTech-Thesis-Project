package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PinnedMessagesActivity extends AppCompatActivity {

    private RecyclerView rvPinned;
    private TextView tvEmpty;
    private PinnedMessagesAdapter adapter;
    private final List<ChatMessage> pinnedList = new ArrayList<>();
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pinned_messages);

        setupStatusBar();
        initializeViews();
        loadPinnedMessagesData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackPinnedMessages);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvPinned = findViewById(R.id.rvPinnedMessagesFull);
        tvEmpty = findViewById(R.id.tvEmptyPinned);

        String currentUid = FirebaseAuth.getInstance().getUid();

        if (rvPinned != null) {
            rvPinned.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PinnedMessagesAdapter(pinnedList, currentUid);
            rvPinned.setAdapter(adapter);
        }
    }

    private void loadPinnedMessagesData() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("GroupChats").child("TechniciansTeam").child("messages");
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pinnedList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    Boolean isPinned = ds.child("isPinned").getValue(Boolean.class);
                    if (msg != null) {
                        if (Boolean.TRUE.equals(isPinned) || msg.isSystem()) {
                            pinnedList.add(msg);
                        }
                    }
                }

                if (tvEmpty != null) {
                    tvEmpty.setVisibility(pinnedList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                if (rvPinned != null) {
                    rvPinned.setVisibility(pinnedList.isEmpty() ? View.GONE : View.VISIBLE);
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
