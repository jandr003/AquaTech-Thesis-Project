package com.example.aquatech;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class GroupChatCustomiseActivity extends AppCompatActivity {

    private View previewOutgoingBubble;
    private MaterialCardView selectedColorCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat_customise);

        setupStatusBar();
        initializeViews();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        View btnBack = findViewById(R.id.btnBackCustomiseChat);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        previewOutgoingBubble = findViewById(R.id.previewOutgoingBubble);

        setupColorOption(R.id.colorOptionBlue, "#2196F3");
        setupColorOption(R.id.colorOptionPurple, "#7C3AED");
        setupColorOption(R.id.colorOptionTeal, "#0D9488");
        setupColorOption(R.id.colorOptionRed, "#EF4444");
        setupColorOption(R.id.colorOptionAmber, "#F59E0B");
        setupColorOption(R.id.colorOptionGreen, "#059669");
        setupColorOption(R.id.colorOptionSlate, "#0F172A");
        setupColorOption(R.id.colorOptionNavy, "#1E3A8A");

        selectedColorCard = findViewById(R.id.colorOptionBlue);

        View btnSave = findViewById(R.id.btnSaveCustomiseTheme);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                Toast.makeText(this, "Chat theme updated!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }
    }

    private void setupColorOption(int cardId, String hexColor) {
        MaterialCardView card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                if (selectedColorCard != null) {
                    selectedColorCard.setStrokeWidth(0);
                }
                card.setStrokeColor(Color.parseColor("#1E293B"));
                card.setStrokeWidth(6);
                selectedColorCard = card;

                if (previewOutgoingBubble != null) {
                    previewOutgoingBubble.getBackground().setTint(Color.parseColor(hexColor));
                }
            });
        }
    }
}
