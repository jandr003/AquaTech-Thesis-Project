package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class CustomerDashboardNotificationPopupHelper {

    public static void showTopNotificationPopup(
            Activity activity,
            FrameLayout notificationContainer,
            String techName,
            boolean hasActiveRequest,
            String currentTicketId
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || notificationContainer == null) {
            return;
        }

        activity.runOnUiThread(() -> {
            LayoutInflater inflater = LayoutInflater.from(activity);
            View layout = inflater.inflate(R.layout.dialog_technician_accepted, notificationContainer, false);
            if (layout == null) {
                return;
            }

            TextView tvTechName = layout.findViewById(R.id.tvAcceptedTechName);
            TextView btnView = layout.findViewById(R.id.btnAcceptedView);
            ImageView btnClose = layout.findViewById(R.id.btnCloseAccepted);

            if (tvTechName != null) {
                tvTechName.setText("Technician " + (techName != null ? techName : "Technician") + " is on the way");
            }

            if (btnView != null) {
                btnView.setOnClickListener(v -> {
                    notificationContainer.removeAllViews();
                    if (hasActiveRequest && currentTicketId != null) {
                        Intent intent = new Intent(activity, ServiceReceiptActivity.class);
                        intent.putExtra("TICKET_ID", currentTicketId);
                        activity.startActivity(intent);
                    }
                });
            }

            if (btnClose != null) {
                btnClose.setOnClickListener(v -> notificationContainer.removeAllViews());
            }

            notificationContainer.removeAllViews();
            notificationContainer.addView(layout);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (notificationContainer != null) {
                    notificationContainer.removeAllViews();
                }
            }, 15000);
        });
    }

    public static void showIncomingCallPopup(
            Activity activity,
            String dbUrl,
            String message,
            String callerTechId,
            String assignedTechId,
            String assignedTechName,
            String myUid
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || myUid == null) {
            return;
        }

        String currentTechId = callerTechId != null ? callerTechId : assignedTechId;
        String techName = assignedTechName != null ? assignedTechName : "AquaTech Technician";

        IncomingCallPopup popup = new IncomingCallPopup(
                activity,
                techName,
                "AquaTech Technician",
                currentTechId,
                myUid,
                false,
                new IncomingCallPopup.OnCallActionListener() {
                    @Override
                    public void onAnswer() {
                        String chatId = currentTechId + "_" + myUid;

                        Map<String, Object> callUpdates = new HashMap<>();
                        callUpdates.put("callStatus", "active");
                        callUpdates.put("callStartTime", ServerValue.TIMESTAMP);

                        FirebaseDatabase.getInstance(dbUrl).getReference("UserChats").child(chatId).updateChildren(callUpdates);

                        Intent intent = new Intent(activity, CustomerVoiceCallActivity.class);
                        intent.putExtra("NAME", techName);
                        intent.putExtra("TECH_ID", currentTechId);
                        intent.putExtra("CUSTOMER_ID", myUid);
                        activity.startActivity(intent);
                    }

                    @Override
                    public void onDecline() {
                        String chatId = currentTechId + "_" + myUid;
                        FirebaseDatabase.getInstance(dbUrl).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
                    }
                }
        );

        popup.show();
    }
}
