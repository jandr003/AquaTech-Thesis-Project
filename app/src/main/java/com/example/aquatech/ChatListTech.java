package com.example.aquatech;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListTech extends AppCompatActivity {

    private RecyclerView rvChatThreads;
    private View emptyContainer;
    private DatabaseReference userChatsRef;
    private final List<DataSnapshot> chatThreadsList = new ArrayList<>();
    private ChatThreadAdapter adapter;
    private String myUid;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tech_chat_list);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        myUid = user.getUid();

        rvChatThreads = findViewById(R.id.rvChatThreads);
        emptyContainer = findViewById(R.id.emptyContainer);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        CustomerDashboardNavHelper.setupBottomNavigation(this);

        if (rvChatThreads != null) {
            rvChatThreads.setLayoutManager(new LinearLayoutManager(this));
            adapter = new ChatThreadAdapter(chatThreadsList, myUid);
            rvChatThreads.setAdapter(adapter);
        }

        userChatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");
        loadChatThreads();
    }

    private void loadChatThreads() {
        if (userChatsRef == null) {
            return;
        }

        userChatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatThreadsList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String cId = ds.child("customerId").getValue(String.class);
                    String tId = ds.child("techId").getValue(String.class);
                    if (myUid != null && (myUid.equals(cId) || myUid.equals(tId))) {
                        chatThreadsList.add(ds);
                    }
                }

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                if (emptyContainer != null) {
                    emptyContainer.setVisibility(chatThreadsList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatListTech.this, "Error loading chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
