package com.example.aquatech;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
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
        this(context, callerName, "AquaTech Partner", null, null, false, listener);
    }

    public IncomingCallPopup(@NonNull Context context, String callerName, String callerRole, String callerId, String myUid, boolean isTech, OnCallActionListener listener) {
        this.listener = listener;
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_incoming_call_popup);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.gravity = Gravity.TOP;
            params.y = 100;
            dialog.getWindow().setAttributes(params);
        }

        TextView tvName = dialog.findViewById(R.id.tvCallerName);
        if (tvName != null && callerName != null) {
            tvName.setText(callerName);
        }

        TextView tvLabel = dialog.findViewById(R.id.tvIncomingCallLabel);
        if (tvLabel != null && callerRole != null) {
            tvLabel.setText("Incoming Call • " + callerRole);
        }

        View avatar = dialog.findViewById(R.id.ivCallerAvatar);
        if (avatar != null && avatar.getParent() != null && avatar.getParent().getParent() != null) {
            View parentCard = (View) avatar.getParent().getParent();
            parentCard.setOnClickListener(v -> {
                Intent intent = new Intent(context, IncomingCallActivity.class);
                intent.putExtra("CALLER_NAME", callerName);
                intent.putExtra("CALLER_ROLE", callerRole);
                intent.putExtra("CALLER_ID", callerId);
                intent.putExtra("MY_UID", myUid);
                intent.putExtra("IS_TECH", isTech);
                context.startActivity(intent);
                dialog.dismiss();
            });
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