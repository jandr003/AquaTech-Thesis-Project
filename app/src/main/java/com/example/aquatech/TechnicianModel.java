package com.example.aquatech;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class TechnicianModel {
    public String techId;
    public String fullName;
    public String name;
    public String username;
    public String techName;
    public String email;
    public String accountStatus; 
    public Verification verification;
    public String profileImageUrl;
    public float rating; // Dagdag para sa performance rating

    public TechnicianModel() {}

    public static class Verification {
        public String status; 
        public String idUrl;
        public String certUrl;
        public long submittedAt;

        public Verification() {}
    }
}
