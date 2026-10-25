package com.example.aquatech;

import android.content.Intent;
import android.graphics.Color;
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

public class TechnicianHelpCenterActivity extends AppCompatActivity {

    private EditText etSearch;
    private View layoutFaq1, layoutFaq2, layoutFaq3, layoutFaq4, layoutFaq5, layoutFaq6;
    private TextView tvQ1, tvQ2, tvQ3, tvQ4, tvQ5, tvQ6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_help_center);

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
        View btnBack = findViewById(R.id.btnBackTechHelp);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        etSearch = findViewById(R.id.etSearchTechHelp);

        layoutFaq1 = findViewById(R.id.layoutTechFaq1);
        layoutFaq2 = findViewById(R.id.layoutTechFaq2);
        layoutFaq3 = findViewById(R.id.layoutTechFaq3);
        layoutFaq4 = findViewById(R.id.layoutTechFaq4);
        layoutFaq5 = findViewById(R.id.layoutTechFaq5);
        layoutFaq6 = findViewById(R.id.layoutTechFaq6);

        tvQ1 = findViewById(R.id.tvTechQ1);
        tvQ2 = findViewById(R.id.tvTechQ2);
        tvQ3 = findViewById(R.id.tvTechQ3);
        tvQ4 = findViewById(R.id.tvTechQ4);
        tvQ5 = findViewById(R.id.tvTechQ5);
        tvQ6 = findViewById(R.id.tvTechQ6);

        View catRequests = findViewById(R.id.catServiceRequests);
        if (catRequests != null) {
            catRequests.setOnClickListener(v -> startActivity(new Intent(this, TechnicianTicketsGuideActivity.class)));
        }

        View catReports = findViewById(R.id.catServiceReports);
        if (catReports != null) {
            catReports.setOnClickListener(v -> startActivity(new Intent(this, TechnicianReportsGuideActivity.class)));
        }
        View catProcedures = findViewById(R.id.catNotifications);
        if (catProcedures != null) {
            catProcedures.setOnClickListener(v -> startActivity(new Intent(this, TechnicianProceduresGuideActivity.class)));
        }
        View catTrouble = findViewById(R.id.catTroubleshooting);
        if (catTrouble != null) {
            catTrouble.setOnClickListener(v -> startActivity(new Intent(this, TechnicianTroubleshootingGuideActivity.class)));
        }
        View catAccount = findViewById(R.id.catAccountProfile);
        if (catAccount != null) {
            catAccount.setOnClickListener(v -> startActivity(new Intent(this, TechnicianAccountGuideActivity.class)));
        }
    }

    private void setupCategoryClick(int catId, View faqLayout, int answerId, int arrowId) {
        View catView = findViewById(catId);
        if (catView != null && faqLayout != null) {
            catView.setOnClickListener(v -> {
                TextView tvAnswer = faqLayout.findViewById(answerId);
                ImageView ivArrow = faqLayout.findViewById(arrowId);
                if (tvAnswer != null && ivArrow != null) {
                    tvAnswer.setVisibility(View.VISIBLE);
                    ivArrow.setRotation(270f);
                    faqLayout.getParent().requestChildFocus(faqLayout, faqLayout);
                }
            });
        }
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
        boolean f6 = tvQ6 != null && tvQ6.getText().toString().toLowerCase().contains(query);

        if (layoutFaq1 != null) layoutFaq1.setVisibility(f1 ? View.VISIBLE : View.GONE);
        if (layoutFaq2 != null) layoutFaq2.setVisibility(f2 ? View.VISIBLE : View.GONE);
        if (layoutFaq3 != null) layoutFaq3.setVisibility(f3 ? View.VISIBLE : View.GONE);
        if (layoutFaq4 != null) layoutFaq4.setVisibility(f4 ? View.VISIBLE : View.GONE);
        if (layoutFaq5 != null) layoutFaq5.setVisibility(f5 ? View.VISIBLE : View.GONE);
        if (layoutFaq6 != null) layoutFaq6.setVisibility(f6 ? View.VISIBLE : View.GONE);
    }

    private void showAllFaqs() {
        if (layoutFaq1 != null) layoutFaq1.setVisibility(View.VISIBLE);
        if (layoutFaq2 != null) layoutFaq2.setVisibility(View.VISIBLE);
        if (layoutFaq3 != null) layoutFaq3.setVisibility(View.VISIBLE);
        if (layoutFaq4 != null) layoutFaq4.setVisibility(View.VISIBLE);
        if (layoutFaq5 != null) layoutFaq5.setVisibility(View.VISIBLE);
        if (layoutFaq6 != null) layoutFaq6.setVisibility(View.VISIBLE);
    }

    private void setupFaqs() {
        setupFaqToggle(layoutFaq1, R.id.tvTechA1, R.id.ivTechArrow1);
        setupFaqToggle(layoutFaq2, R.id.tvTechA2, R.id.ivTechArrow2);
        setupFaqToggle(layoutFaq3, R.id.tvTechA3, R.id.ivTechArrow3);
        setupFaqToggle(layoutFaq4, R.id.tvTechA4, R.id.ivTechArrow4);
        setupFaqToggle(layoutFaq5, R.id.tvTechA5, R.id.ivTechArrow5);
        setupFaqToggle(layoutFaq6, R.id.tvTechA6, R.id.ivTechArrow6);
    }

    private void setupFaqToggle(View layout, int answerId, int arrowId) {
        if (layout == null) return;
        final TextView tvAnswer = layout.findViewById(answerId);
        final ImageView ivArrow = layout.findViewById(arrowId);

        layout.setOnClickListener(v -> {
            if (tvAnswer != null && ivArrow != null) {
                if (tvAnswer.getVisibility() == View.GONE) {
                    tvAnswer.setVisibility(View.VISIBLE);
                    ivArrow.setRotation(270f);
                } else {
                    tvAnswer.setVisibility(View.GONE);
                    ivArrow.setRotation(90f);
                }
            }
        });
    }
}
