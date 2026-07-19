package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HelpCenterActivity extends AppCompatActivity {

    private EditText etSearch;
    private View faqCard, faq1, faq2, faq3, faq4, faq5;
    private View div1, div2, div3, div4;
    private TextView tvQ1, tvQ2, tvQ3, tvQ4, tvQ5, tvPopularTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_center);

        setupStatusBar();
        initializeViews();
        setupSearch();
        setupFaqs();
    }

    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void initializeViews() {
        findViewById(R.id.btnBackHelp).setOnClickListener(v -> finish());
        
        etSearch = findViewById(R.id.etSearchHelp);
        faqCard = findViewById(R.id.faqCard);
        tvPopularTitle = findViewById(R.id.tvPopularTopics);
        
        // FAQ Containers
        faq1 = findViewById(R.id.layoutFaq1);
        faq2 = findViewById(R.id.layoutFaq2);
        faq3 = findViewById(R.id.layoutFaq3);
        faq4 = findViewById(R.id.layoutFaq4);
        faq5 = findViewById(R.id.layoutFaq5);
        
        // Dividers
        div1 = findViewById(R.id.divider1);
        div2 = findViewById(R.id.divider2);
        div3 = findViewById(R.id.divider3);
        div4 = findViewById(R.id.divider4);
        
        // Questions (para sa filtering)
        tvQ1 = findViewById(R.id.tvQuestion1);
        tvQ2 = findViewById(R.id.tvQuestion2);
        tvQ3 = findViewById(R.id.tvQuestion3);
        tvQ4 = findViewById(R.id.tvQuestion4);
        tvQ5 = findViewById(R.id.tvQuestion5);

        findViewById(R.id.btnChatSupport).setOnClickListener(v -> {
            startActivity(new Intent(this, AquaBuddyActivity.class));
        });

        findViewById(R.id.btnCallSupport).setOnClickListener(v -> {
            String phoneNumber = "tel:09123456789"; 
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(phoneNumber));
            startActivity(intent);
        });
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFaqs(s.toString().toLowerCase().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void filterFaqs(String query) {
        if (query.isEmpty()) {
            showAllFaqs();
            return;
        }

        boolean f1 = tvQ1 != null && tvQ1.getText().toString().toLowerCase().contains(query);
        boolean f2 = tvQ2 != null && tvQ2.getText().toString().toLowerCase().contains(query);
        boolean f3 = tvQ3 != null && tvQ3.getText().toString().toLowerCase().contains(query);
        boolean f4 = tvQ4 != null && tvQ4.getText().toString().toLowerCase().contains(query);
        boolean f5 = tvQ5 != null && tvQ5.getText().toString().toLowerCase().contains(query);

        if (faq1 != null) faq1.setVisibility(f1 ? View.VISIBLE : View.GONE);
        if (faq2 != null) faq2.setVisibility(f2 ? View.VISIBLE : View.GONE);
        if (faq3 != null) faq3.setVisibility(f3 ? View.VISIBLE : View.GONE);
        if (faq4 != null) faq4.setVisibility(f4 ? View.VISIBLE : View.GONE);
        if (faq5 != null) faq5.setVisibility(f5 ? View.VISIBLE : View.GONE);

        if (div1 != null) div1.setVisibility(f1 && (f2 || f3 || f4 || f5) ? View.VISIBLE : View.GONE);
        if (div2 != null) div2.setVisibility(f2 && (f3 || f4 || f5) ? View.VISIBLE : View.GONE);
        if (div3 != null) div3.setVisibility(f3 && (f4 || f5) ? View.VISIBLE : View.GONE);
        if (div4 != null) div4.setVisibility(f4 && f5 ? View.VISIBLE : View.GONE);

        boolean hasAnyMatch = f1 || f2 || f3 || f4 || f5;
        if (faqCard != null) faqCard.setVisibility(hasAnyMatch ? View.VISIBLE : View.GONE);
        if (tvPopularTitle != null) tvPopularTitle.setVisibility(hasAnyMatch ? View.VISIBLE : View.GONE);
    }

    private void showAllFaqs() {
        if (faq1 != null) faq1.setVisibility(View.VISIBLE);
        if (faq2 != null) faq2.setVisibility(View.VISIBLE);
        if (faq3 != null) faq3.setVisibility(View.VISIBLE);
        if (faq4 != null) faq4.setVisibility(View.VISIBLE);
        if (faq5 != null) faq5.setVisibility(View.VISIBLE);
        if (div1 != null) div1.setVisibility(View.VISIBLE);
        if (div2 != null) div2.setVisibility(View.VISIBLE);
        if (div3 != null) div3.setVisibility(View.VISIBLE);
        if (div4 != null) div4.setVisibility(View.VISIBLE);
        if (faqCard != null) faqCard.setVisibility(View.VISIBLE);
        if (tvPopularTitle != null) tvPopularTitle.setVisibility(View.VISIBLE);
    }

    private void setupFaqs() {
        setupFaqToggle(faq1, R.id.tvAnswer1, R.id.ivArrow1);
        setupFaqToggle(faq2, R.id.tvAnswer2, R.id.ivArrow2);
        setupFaqToggle(faq3, R.id.tvAnswer3, R.id.ivArrow3);
        setupFaqToggle(faq4, R.id.tvAnswer4, R.id.ivArrow4);
        setupFaqToggle(faq5, R.id.tvAnswer5, R.id.ivArrow5);
    }

    private void setupFaqToggle(View layout, int answerId, int arrowId) {
        if (layout == null) return;
        final TextView tvAnswer = layout.findViewById(answerId);
        final ImageView ivArrow = layout.findViewById(arrowId);

        layout.setOnClickListener(v -> {
            if (tvAnswer != null && ivArrow != null) {
                if (tvAnswer.getVisibility() == View.GONE) {
                    tvAnswer.setVisibility(View.VISIBLE);
                    ivArrow.setRotation(180f);
                } else {
                    tvAnswer.setVisibility(View.GONE);
                    ivArrow.setRotation(0f);
                }
            }
        });
    }
}
