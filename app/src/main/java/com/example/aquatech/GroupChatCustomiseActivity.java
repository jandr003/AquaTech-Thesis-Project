package com.example.aquatech;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class GroupChatCustomiseActivity extends AppCompatActivity {

    private View previewOutgoingBubble;
    private MaterialCardView previewCard;
    private MaterialCardView selectedColorCard;
    private MaterialButton btnModeLight, btnModeDark, btnModeDevice;

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
        previewCard = findViewById(R.id.previewCard);

        btnModeLight = findViewById(R.id.btnModeLight);
        btnModeDark = findViewById(R.id.btnModeDark);
        btnModeDevice = findViewById(R.id.btnModeDevice);

        if (btnModeLight != null) {
            btnModeLight.setOnClickListener(v -> setPreviewMode(1));
        }

        if (btnModeDark != null) {
            btnModeDark.setOnClickListener(v -> setPreviewMode(2));
        }

        if (btnModeDevice != null) {
            btnModeDevice.setOnClickListener(v -> setPreviewMode(3));
        }

        setupColorOption(R.id.colorOptionBlue, "#2196F3");
        setupColorOption(R.id.colorOptionPurple, "#7C3AED");
        setupColorOption(R.id.colorOptionTeal, "#0D9488");
        setupColorOption(R.id.colorOptionRed, "#EF4444");
        setupColorOption(R.id.colorOptionAmber, "#F59E0B");
        setupColorOption(R.id.colorOptionGreen, "#059669");
        setupColorOption(R.id.colorOptionSlate, "#64748B");
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

    private void setPreviewMode(int mode) {
        int activeBg = Color.parseColor("#2196F3");
        int activeText = Color.WHITE;
        int inactiveText = Color.parseColor("#64748B");

        if (btnModeLight != null) {
            btnModeLight.setBackgroundTintList(ColorStateList.valueOf(mode == 1 ? activeBg : Color.TRANSPARENT));
            btnModeLight.setTextColor(mode == 1 ? activeText : inactiveText);
        }

        if (btnModeDark != null) {
            btnModeDark.setBackgroundTintList(ColorStateList.valueOf(mode == 2 ? activeBg : Color.TRANSPARENT));
            btnModeDark.setTextColor(mode == 2 ? activeText : inactiveText);
        }

        if (btnModeDevice != null) {
            btnModeDevice.setBackgroundTintList(ColorStateList.valueOf(mode == 3 ? activeBg : Color.TRANSPARENT));
            btnModeDevice.setTextColor(mode == 3 ? activeText : inactiveText);
        }

        if (previewCard != null) {
            if (mode == 1) {
                previewCard.setCardBackgroundColor(Color.parseColor("#F8FAFC"));
            } else if (mode == 2) {
                previewCard.setCardBackgroundColor(Color.parseColor("#0F172A"));
            } else {
                previewCard.setCardBackgroundColor(Color.parseColor("#1E293B"));
            }
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
