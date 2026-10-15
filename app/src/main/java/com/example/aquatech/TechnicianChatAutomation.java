package com.example.aquatech;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class TechnicianChatAutomation {

    public static void sendAcceptanceChatMessage(String dbUrl, String currentTechUid, String currentTechName, String customerId, String ticketId, String customerName, String htmlMsg) {
        if (currentTechUid == null || currentTechUid.isEmpty() || customerId == null || customerId.isEmpty()) return;

        String chatId = currentTechUid + "_" + customerId;
        DatabaseReference chatRef = FirebaseDatabase.getInstance(dbUrl).getReference("UserChats").child(chatId);

        DatabaseReference systemMsgRef = chatRef.child("messages").push();
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("text", htmlMsg);
        systemMessage.put("senderId", currentTechUid);
        systemMessage.put("timestamp", ServerValue.TIMESTAMP);
        systemMessage.put("system", true);
        systemMessage.put("isUser", false);
        systemMsgRef.setValue(systemMessage);

        String welcomeText = "Hello! I'm Technician " + currentTechName + ". I've accepted your ticket #" + ticketId;
        DatabaseReference welcomeMsgRef = chatRef.child("messages").push();
        Map<String, Object> welcomeMessage = new HashMap<>();
        welcomeMessage.put("text", welcomeText);
        welcomeMessage.put("senderId", currentTechUid);
        welcomeMessage.put("timestamp", ServerValue.TIMESTAMP);
        welcomeMessage.put("system", false);
        welcomeMessage.put("isUser", false);
        welcomeMsgRef.setValue(welcomeMessage);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("customerName", customerName != null ? customerName : "Customer");
        metadata.put("techName", currentTechName);
        metadata.put("techId", currentTechUid);
        metadata.put("customerId", customerId);
        metadata.put("ticketId", ticketId);
        metadata.put("lastMessage", welcomeText);
        metadata.put("lastSenderId", currentTechUid);
        metadata.put("timestamp", ServerValue.TIMESTAMP);

        chatRef.updateChildren(metadata);
    }

    public static void sendNotificationToUser(String dbUrl, String userId, String message, String type, String ticketId, String senderName, boolean isToCustomer) {
        if (userId == null || userId.isEmpty()) return;
        String target = isToCustomer ? "CustomerNotifications" : "Notifications";
        DatabaseReference ref = FirebaseDatabase.getInstance(dbUrl).getReference(target).child(userId).push();

        NotificationModel notification = new NotificationModel(
                ref.getKey(),
                message,
                System.currentTimeMillis(),
                type,
                ticketId,
                senderName != null ? senderName : "Technician"
        );
        ref.setValue(notification);
    }
}
