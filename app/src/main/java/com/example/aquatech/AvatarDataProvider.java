package com.example.aquatech;

import java.util.ArrayList;
import java.util.List;

public class AvatarDataProvider {

    public static List<AvatarModel> getCustomerAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();

        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140048.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140037.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140061.png"));

        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140047.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140051.png"));
        avatars.add(new AvatarModel("https://cdn-icons-png.flaticon.com/512/4140/4140040.png"));

        return avatars;
    }

    public static List<AvatarModel> getTechnicianAvatars() {
        List<AvatarModel> avatars = new ArrayList<>();
        avatars.add(new AvatarModel("https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&auto=format&fit=crop&q=80"));
        avatars.add(new AvatarModel("https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=400&auto=format&fit=crop&q=80"));
        avatars.add(new AvatarModel("https://images.unsplash.com/photo-1504384308090-c894fdcc538d?w=400&auto=format&fit=crop&q=80"));
        return avatars;
    }
}