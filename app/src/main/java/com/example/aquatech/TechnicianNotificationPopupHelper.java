package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class TechnicianNotificationPopupHelper {

    public static void showResubmitPopup(
            Activity activity,
            FrameLayout notificationContainer,
            String message,
            Runnable onOpenReport
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || notificationContainer == null) return;

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.layout_resubmit_report_popup, notificationContainer, false);
        TextView tvMsg = layout.findViewById(R.id.tvResubmitMessage);
        Button btnOpen = layout.findViewById(R.id.btnOpenReport);
        Button btnDismiss = layout.findViewById(R.id.btnDismiss);

        if (tvMsg != null) tvMsg.setText(message);
        if (btnOpen != null) {
            btnOpen.setOnClickListener(v -> {
                notificationContainer.removeAllViews();
                if (onOpenReport != null) {
                    onOpenReport.run();
                } else {
                    Intent intent = new Intent(activity, ResubmitReportActivity.class);
                    activity.startActivity(intent);
                }
            });
        }
        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> notificationContainer.removeAllViews());
        }

        notificationContainer.removeAllViews();
        notificationContainer.addView(layout);
    }
}
