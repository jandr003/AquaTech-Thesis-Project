package com.example.aquatech;

public class AvatarModel {
    private int imageResId;
    private String imageUrl;

    public AvatarModel(int imageResId) {
        this.imageResId = imageResId;
    }

    public AvatarModel(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}