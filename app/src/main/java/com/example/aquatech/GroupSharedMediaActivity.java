package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupSharedMediaActivity extends AppCompatActivity {

    private RecyclerView rvMedia;
    private TextView tvEmpty;
    private LinearLayout tabMedia, tabFiles, tabLinks;
    private ImageView ivTabMediaIcon, ivTabFilesIcon, ivTabLinksIcon;
    private TextView tvTabMediaLabel, tvTabFilesLabel, tvTabLinksLabel;

    private SharedMediaAdapter mediaAdapter;
    private final List<ChatMessage> mediaList = new ArrayList<>();

    private int currentTab = 1; // 1: Media, 2: Files, 3: Links
    private final String DB_URL = "https://aquatech-8da99c74-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_shared_media);

        setupStatusBar();
        initializeViews();
        selectTab(1);
        loadGroupMediaData();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackSharedMedia);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        rvMedia = findViewById(R.id.rvSharedMediaGrid);
        tvEmpty = findViewById(R.id.tvEmptySharedGallery);

        tabMedia = findViewById(R.id.tabMedia);
        tabFiles = findViewById(R.id.tabFiles);
        tabLinks = findViewById(R.id.tabLinks);

        ivTabMediaIcon = findViewById(R.id.ivTabMediaIcon);
        ivTabFilesIcon = findViewById(R.id.ivTabFilesIcon);
        ivTabLinksIcon = findViewById(R.id.ivTabLinksIcon);

        tvTabMediaLabel = findViewById(R.id.tvTabMediaLabel);
        tvTabFilesLabel = findViewById(R.id.tvTabFilesLabel);
        tvTabLinksLabel = findViewById(R.id.tvTabLinksLabel);

        if (tabMedia != null) tabMedia.setOnClickListener(v -> selectTab(1));
        if (tabFiles != null) tabFiles.setOnClickListener(v -> selectTab(2));
        if (tabLinks != null) tabLinks.setOnClickListener(v -> selectTab(3));

        mediaAdapter = new SharedMediaAdapter(mediaList);
        if (rvMedia != null) {
            rvMedia.setLayoutManager(new GridLayoutManager(this, 3));
            rvMedia.setAdapter(mediaAdapter);
        }
    }

    private void selectTab(int tab) {
        currentTab = tab;
        int activeColor = Color.parseColor("#2196F3");
        int inactiveColor = Color.parseColor("#64748B");

        if (ivTabMediaIcon != null) ivTabMediaIcon.setColorFilter(tab == 1 ? activeColor : inactiveColor);
        if (tvTabMediaLabel != null) tvTabMediaLabel.setTextColor(tab == 1 ? activeColor : inactiveColor);

        if (ivTabFilesIcon != null) ivTabFilesIcon.setColorFilter(tab == 2 ? activeColor : inactiveColor);
        if (tvTabFilesLabel != null) tvTabFilesLabel.setTextColor(tab == 2 ? activeColor : inactiveColor);

        if (ivTabLinksIcon != null) ivTabLinksIcon.setColorFilter(tab == 3 ? activeColor : inactiveColor);
        if (tvTabLinksLabel != null) tvTabLinksLabel.setTextColor(tab == 3 ? activeColor : inactiveColor);

        if (rvMedia != null) {
            if (tab == 1) {
                rvMedia.setLayoutManager(new GridLayoutManager(this, 3));
            } else {
                rvMedia.setLayoutManager(new LinearLayoutManager(this));
            }
        }
    }

    private void loadGroupMediaData() {
        DatabaseReference chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("GroupChats").child("TechniciansTeam").child("messages");
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mediaList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null && (msg.getFileUri() != null || msg.getFileUrl() != null || (msg.getText() != null && msg.getText().contains("http")))) {
                        mediaList.add(msg);
                    }
                }

                if (tvEmpty != null) {
                    tvEmpty.setVisibility(mediaList.isEmpty() ? View.VISIBLE : View.GONE);
                }
                if (rvMedia != null) {
                    rvMedia.setVisibility(mediaList.isEmpty() ? View.GONE : View.VISIBLE);
                }
                if (mediaAdapter != null) {
                    mediaAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
