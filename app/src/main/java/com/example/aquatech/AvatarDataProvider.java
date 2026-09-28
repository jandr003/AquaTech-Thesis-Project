package com.example.aquatech;

import java.util.ArrayList;
import java.util.List;

public class AvatarDataProvider {

    public static List<AvatarModel> getCustomerAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();

        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140048.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140047.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140051.png"));

        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140040.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140037.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140039.png"));

        return avatars;
    }

    public static List<AvatarModel> getTechnicianAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();
        avatars.add(new AvatarModel(R.drawable.technician_man1));
        avatars.add(new AvatarModel(R.drawable.technician_woman1));
        avatars.add(new AvatarModel(R.drawable.technician_woman2));
        return avatars;
    }
}