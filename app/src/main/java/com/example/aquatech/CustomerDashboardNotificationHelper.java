package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class CustomerDashboardNotificationHelper {

    public interface OnNotificationActionListener {
        void onNotificationClicked(String ticketId, String action);
    }

    public static void showTopNotification(
            Activity activity,
            FrameLayout notificationContainer,
            String message,
            String ticketId,
            OnNotificationActionListener listener
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || notificationContainer == null) {
            return;
        }

        activity.runOnUiThread(() -> {
            notificationContainer.removeAllViews();

            View notifView = LayoutInflater.from(activity).inflate(R.layout.item_notification, notificationContainer, false);
            if (notifView == null) {
                return;
            }

            TextView tvMsg = notifView.findViewById(R.id.tvNotifMessage);
            TextView tvTime = notifView.findViewById(R.id.tvNotifTime);

            if (tvMsg != null && message != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tvMsg.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    tvMsg.setText(Html.fromHtml(message));
                }
            }

            if (tvTime != null) {
                tvTime.setText("Just now");
            }

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );

            int marginSide = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
            int marginTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, activity.getResources().getDisplayMetrics());
            params.setMargins(marginSide, marginTop, marginSide, 0);

            notifView.setLayoutParams(params);
            notificationContainer.addView(notifView);

            notifView.setTranslationY(-500f);
            notifView.animate().translationY(0f).setDuration(600).setInterpolator(new OvershootInterpolator()).start();

            notifView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClicked(ticketId, "OPEN");
                }
                removeNotification(activity, notificationContainer, notifView);
            });

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                removeNotification(activity, notificationContainer, notifView);
            }, 10000);
        });
    }

    public static void removeNotification(Activity activity, FrameLayout notificationContainer, View notifView) {
        if (notifView != null && notifView.getParent() != null && notificationContainer != null) {
            activity.runOnUiThread(() -> {
                notifView.animate().translationY(-500f).setDuration(500).withEndAction(() -> {
                    notificationContainer.removeView(notifView);
                }).start();
            });
        }
    }
}
