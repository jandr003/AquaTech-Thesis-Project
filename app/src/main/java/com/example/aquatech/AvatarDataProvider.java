package com.example.aquatech;

import java.util.ArrayList;
import java.util.List;

public class AvatarDataProvider {

    public static List<AvatarModel> getTechnicianAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();
        // Professional tech icons
        avatars.add(new AvatarModel(R.drawable.technician_man1));
        avatars.add(new AvatarModel(R.drawable.technician_woman2));
        return avatars;
    }

    public static List<AvatarModel> getCustomerAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();
        avatars.add(new AvatarModel(R.drawable.man_user_02b_bg));
        avatars.add(new AvatarModel(R.drawable.man_user_05c_bg));
        avatars.add(new AvatarModel(R.drawable.man_user_07a_bg));
        avatars.add(new AvatarModel(R.drawable.woman_user_01a_bg));
        avatars.add(new AvatarModel(R.drawable.woman_user_03c_bg));
        avatars.add(new AvatarModel(R.drawable.woman_user_06b_bg));
        return avatars;
    }
}
