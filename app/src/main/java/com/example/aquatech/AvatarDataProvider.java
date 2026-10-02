package com.example.aquatech;

import java.util.ArrayList;
import java.util.List;

public class AvatarDataProvider {

    public static List<AvatarModel> getCustomerAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();

        avatars.add(new AvatarModel(R.drawable.customer_avatar1));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_2));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_3));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_4));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_5));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_6));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_7));
        avatars.add(new AvatarModel(R.drawable.customer_avatar_8));
        return avatars;
    }


    public static List<AvatarModel> getTechnicianAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();

        avatars.add(new AvatarModel(R.drawable.technician_avatar1));
        avatars.add(new AvatarModel(R.drawable.technician_avatar2));
        avatars.add(new AvatarModel(R.drawable.technician_avatar3));
        avatars.add(new AvatarModel(R.drawable.technician_avatar4));
        avatars.add(new AvatarModel(R.drawable.technician_avatar5));
        avatars.add(new AvatarModel(R.drawable.technician_avatar6));
        avatars.add(new AvatarModel(R.drawable.technician_avatar7));
        avatars.add(new AvatarModel(R.drawable.technician_avatar8));
        return avatars;
    }

    public static List<AvatarModel> getAdminAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();

        avatars.add(new AvatarModel(R.drawable.admin_avatar1));
        avatars.add(new AvatarModel(R.drawable.admin_avatar2));
        return avatars;
    }
}