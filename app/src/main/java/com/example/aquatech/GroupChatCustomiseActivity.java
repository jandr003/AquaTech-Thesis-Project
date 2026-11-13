package com.example.aquatech;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
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
            int targetCardColor = (mode == 1) ? Color.parseColor("#F8FAFC") : (mode == 2 ? Color.parseColor("#020617") : Color.parseColor("#1E293B"));
            int fromCardColor = previewCard.getCardBackgroundColor().getDefaultColor();

            ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), fromCardColor, targetCardColor);
            anim.setDuration(300);
            anim.addUpdateListener(animation -> {
                if (previewCard != null) {
                    previewCard.setCardBackgroundColor((int) animation.getAnimatedValue());
                }
            });
            anim.start();
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
                    int targetColor = Color.parseColor(hexColor);
                    int fromColor = Color.parseColor("#2196F3");

                    ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, targetColor);
                    anim.setDuration(300);
                    anim.addUpdateListener(animation -> {
                        if (previewOutgoingBubble != null && previewOutgoingBubble.getBackground() != null) {
                            previewOutgoingBubble.getBackground().setTint((int) animation.getAnimatedValue());
                        }
                    });
                    anim.start();
                }
            });
        }
    }
}
