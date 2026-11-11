package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupChatSearchActivity extends AppCompatActivity {

    private EditText etSearchQuery;
    private RecyclerView rvSearchResults;
    private TextView tvEmptySearch;

    private ChatAdapter chatAdapter;
    private final List<ChatMessage> allMessages = new ArrayList<>();
    private final List<ChatMessage> filteredMessages = new ArrayList<>();

    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_search);

        setupStatusBar();
        initializeViews();
        loadAllGroupMessages();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackSearchChat);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etSearchQuery = findViewById(R.id.etSearchQuery);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        tvEmptySearch = findViewById(R.id.tvEmptySearch);

        if (rvSearchResults != null) {
            rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
            chatAdapter = new ChatAdapter(filteredMessages, ChatAdapter.MODE_TECH_CHAT);
            rvSearchResults.setAdapter(chatAdapter);
        }

        if (etSearchQuery != null) {
            etSearchQuery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterQuery(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void loadAllGroupMessages() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("GroupChats").child("TechniciansTeam").child("messages");
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allMessages.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null && msg.getText() != null && !msg.getText().isEmpty()) {
                        allMessages.add(msg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterQuery(String query) {
        filteredMessages.clear();
        String lowerQuery = query.toLowerCase().trim();

        if (!lowerQuery.isEmpty()) {
            for (ChatMessage msg : allMessages) {
                if (msg.getText() != null && msg.getText().toLowerCase().contains(lowerQuery)) {
                    filteredMessages.add(msg);
                }
            }
        }

        if (tvEmptySearch != null) {
            if (lowerQuery.isEmpty()) {
                tvEmptySearch.setText("Type above to search messages in Technicians Team Chat.");
                tvEmptySearch.setVisibility(View.VISIBLE);
            } else if (filteredMessages.isEmpty()) {
                tvEmptySearch.setText("No matching messages found.");
                tvEmptySearch.setVisibility(View.VISIBLE);
            } else {
                tvEmptySearch.setVisibility(View.GONE);
            }
        }

        if (rvSearchResults != null) {
            rvSearchResults.setVisibility(filteredMessages.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }
}
