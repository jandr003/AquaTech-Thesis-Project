package com.example.aquatech;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class IncomingCallPopup {

    private final Dialog dialog;
    private final OnCallActionListener listener;

    public interface OnCallActionListener {
        void onAnswer();
        void onDecline();
    }

    public IncomingCallPopup(@NonNull Context context, String callerName, OnCallActionListener listener) {
        this.listener = listener;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_incoming_call_popup);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            // Show at the top like a heads-up notification
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = android.view.Gravity.TOP;
            params.y = 100; // Offset from top
            dialog.getWindow().setAttributes(params);
        }

        TextView tvName = dialog.findViewById(R.id.tvCallerName);
        if (tvName != null && callerName != null) {
            tvName.setText(callerName);
        }

        ImageView btnAnswer = dialog.findViewById(R.id.btnAnswerCall);
        if (btnAnswer != null) {
            btnAnswer.setOnClickListener(v -> {
                if (listener != null) listener.onAnswer();
                dialog.dismiss();
            });
        }

        ImageView btnDecline = dialog.findViewById(R.id.btnDeclineCall);
        if (btnDecline != null) {
            btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDecline();
                dialog.dismiss();
            });
        }
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
