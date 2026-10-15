package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class TechnicianCallHandler {

    public static void showIncomingCallPopup(
            Activity activity,
            String dbUrl,
            String customerId,
            String currentTechUid
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || customerId == null || customerId.trim().isEmpty()) return;

        FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(customerId.trim())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.child("fullName").getValue(String.class);
                        if (name == null) name = snapshot.child("name").getValue(String.class);
                        if (name == null) name = "Customer";

                        String finalName = name;
                        IncomingCallPopup popup = new IncomingCallPopup(
                            activity,
                            finalName,
                            "Customer",
                            customerId.trim(),
                            currentTechUid,
                            true,
                            new IncomingCallPopup.OnCallActionListener() {
                                @Override
                                public void onAnswer() {
                                    String chatId = currentTechUid + "_" + customerId.trim();
                                    DatabaseReference chatRef = FirebaseDatabase.getInstance(dbUrl).getReference("UserChats").child(chatId);
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("callStatus", "active");
                                    updates.put("callStartTime", ServerValue.TIMESTAMP);
                                    chatRef.updateChildren(updates);

                                    Intent intent = new Intent(activity, VoiceCallActivity.class);
                                    intent.putExtra("TECH_ID", currentTechUid);
                                    intent.putExtra("CUSTOMER_ID", customerId.trim());
                                    intent.putExtra("NAME", finalName);
                                    activity.startActivity(intent);
                                }

                                @Override
                                public void onDecline() {
                                    String chatId = currentTechUid + "_" + customerId.trim();
                                    FirebaseDatabase.getInstance(dbUrl).getReference("UserChats").child(chatId).child("callStatus").setValue("ended");
                                }
                            }
                        );
                        popup.show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
