package com.example.aquatech;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CustomerRatingDialogHelper {

    public interface OnRatingSubmittedCallback {
        void onRatingFinished();
    }

    public static void showRatingDialog(
            Activity activity,
            String dbUrl,
            String ticketId,
            String techName,
            String techId,
            OnRatingSubmittedCallback callback
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rate_technician);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setWindowAnimations(android.R.style.Animation_Dialog);
        }

        ImageView ivTechProfile = dialog.findViewById(R.id.ivTechProfile);
        TextView tvTechName = dialog.findViewById(R.id.tvTechName);
        RatingBar ratingBar = dialog.findViewById(R.id.ratingBar);
        EditText etFeedback = dialog.findViewById(R.id.etRatingFeedback);
        Button btnSubmit = dialog.findViewById(R.id.btnSubmitRating);
        ImageView btnClose = dialog.findViewById(R.id.btnCloseRating);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (tvTechName != null) {
            tvTechName.setText(techName != null ? techName : "Technician");
        }

        if (techId != null) {
            DatabaseReference techRef = FirebaseDatabase.getInstance(dbUrl).getReference("Technicians").child(techId);
            techRef.child("profileImageUrl").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String url = snapshot.getValue(String.class);
                    if (ivTechProfile != null && !activity.isFinishing() && !activity.isDestroyed()) {
                        if (url != null && !url.isEmpty()) {
                            Glide.with(activity).load(url).circleCrop().into(ivTechProfile);
                        } else {
                            ivTechProfile.setImageResource(R.drawable.new_technician);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else if (ivTechProfile != null) {
            ivTechProfile.setImageResource(R.drawable.new_technician);
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (ratingBar == null) {
                    return;
                }

                float rating = ratingBar.getRating();
                if (rating < 1) {
                    Toast.makeText(activity, "Please select at least 1 star", Toast.LENGTH_SHORT).show();
                    return;
                }

                String feedbackText = etFeedback != null && etFeedback.getText() != null ? etFeedback.getText().toString().trim() : "";

                Map<String, Object> ratingUpdate = new HashMap<>();
                ratingUpdate.put("rating", rating);
                ratingUpdate.put("ratingFeedback", feedbackText);
                ratingUpdate.put("rated", true);

                if (ticketId != null) {
                    DatabaseReference requestsRef = FirebaseDatabase.getInstance(dbUrl).getReference("ServiceRequests");
                    requestsRef.child(ticketId).updateChildren(ratingUpdate)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity, "Thank you for your rating and feedback!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                if (callback != null) {
                                    callback.onRatingFinished();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(activity, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                                if (callback != null) {
                                    callback.onRatingFinished();
                                }
                            });
                }
            });
        }

        dialog.setOnDismissListener(d -> {
            if (callback != null) {
                callback.onRatingFinished();
            }
        });

        dialog.show();
    }
}
