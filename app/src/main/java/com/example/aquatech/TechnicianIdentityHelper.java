package com.example.aquatech;

import android.content.Intent;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TechnicianIdentityHelper {

    public interface IdentityCallback {
        void onIdentityResolved(String uid, String name);
    }

    public static void fetchTechnicianIdentity(String dbUrl, String uid, String email, IdentityCallback callback) {
        if (uid != null && !uid.isEmpty()) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(uid);
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String resolvedName = "";
                    if (snapshot.exists()) {
                        resolvedName = snapshot.child("fullName").getValue(String.class);
                        if (resolvedName == null) {
                            resolvedName = snapshot.child("name").getValue(String.class);
                        }
                    }

                    if (resolvedName == null || resolvedName.trim().isEmpty()) {
                        resolvedName = resolveTechNameFromEmail(email);
                    }

                    if (callback != null) {
                        callback.onIdentityResolved(uid, resolvedName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (callback != null) {
                        callback.onIdentityResolved(uid, resolveTechNameFromEmail(email));
                    }
                }
            });
        } else if (email != null && !email.isEmpty()) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance(dbUrl).getReference("Users");
            usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String resolvedUid = email;
                    String resolvedName = resolveTechNameFromEmail(email);

                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            resolvedUid = child.getKey();
                            String n = child.child("fullName").getValue(String.class);
                            if (n == null) {
                                n = child.child("name").getValue(String.class);
                            }

                            if (n != null && !n.trim().isEmpty()) {
                                resolvedName = n.trim();
                            }
                            break;
                        }
                    }

                    if (callback != null) {
                        callback.onIdentityResolved(resolvedUid, resolvedName);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (callback != null) {
                        callback.onIdentityResolved(email, resolveTechNameFromEmail(email));
                    }
                }
            });
        }
    }

    public static void attachLiveAvatarListener(String dbUrl, String uid, ImageView imageView) {
        if (uid == null || uid.isEmpty() || imageView == null) {
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(uid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String imgUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    Integer resId = snapshot.child("avatarResId").getValue(Integer.class);

                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        Glide.with(imageView.getContext())
                                .load(imgUrl)
                                .placeholder(R.drawable.technician_avatar1)
                                .circleCrop()
                                .into(imageView);
                    } else if (resId != null && resId > 0) {
                        Glide.with(imageView.getContext())
                                .load(resId)
                                .placeholder(R.drawable.technician_avatar1)
                                .circleCrop()
                                .into(imageView);
                    } else {
                        Glide.with(imageView.getContext())
                                .load(R.drawable.technician_avatar1)
                                .circleCrop()
                                .into(imageView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static String resolveTechNameFromEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "Technician";
        }

        String cleanEmail = email.toLowerCase().trim();
        int atIndex = cleanEmail.indexOf("@");

        if (atIndex > 0) {
            String prefix = cleanEmail.substring(0, atIndex).replace(".", " ");
            String[] words = prefix.split(" ");
            StringBuilder sb = new StringBuilder();

            for (String word : words) {
                if (!word.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        sb.append(word.substring(1));
                    }
                }
            }

            if (sb.length() > 0) {
                return sb.toString();
            }
        }

        return "Technician";
    }

    public static String getStoredEmail(Intent intent, FirebaseAuth mAuth) {
        if (intent != null && intent.hasExtra("USER_EMAIL")) {
            String e = intent.getStringExtra("USER_EMAIL");
            if (e != null && !e.isEmpty()) {
                return e.toLowerCase().trim();
            }
        }

        if (mAuth != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                return user.getEmail().toLowerCase().trim();
            }
        }

        return "";
    }
}
