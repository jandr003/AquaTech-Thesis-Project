package com.example.aquatech;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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

public class CustomerChatListActivity extends AppCompatActivity {

    private RecyclerView rvChatThreads;
    private TextView tvEmpty;
    private DatabaseReference userChatsRef;
    private List<DataSnapshot> chatThreadsList = new ArrayList<>();
    private ChatThreadAdapter adapter;
    private String myUid;
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        myUid = user.getUid();

        rvChatThreads = findViewById(R.id.rvChatThreads);
        tvEmpty = findViewById(R.id.tvEmptyChats);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvChatThreads.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatThreadAdapter(chatThreadsList, myUid);
        rvChatThreads.setAdapter(adapter);

        userChatsRef = FirebaseDatabase.getInstance(DB_URL).getReference("UserChats");
        loadChatThreads();
    }

    private void loadChatThreads() {
        userChatsRef.orderByChild("customerId").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatThreadsList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            chatThreadsList.add(ds);
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(chatThreadsList.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvEmpty.setText("Error loading chats");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }
}