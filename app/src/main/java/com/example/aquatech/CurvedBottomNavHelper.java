package com.example.aquatech;

import android.view.View;
import android.view.animation.OvershootInterpolator;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CurvedBottomNavHelper {

    public static void setupAnimatedNav(BottomNavigationView bottomNav) {
        if (bottomNav == null) return;

        bottomNav.post(() -> {
            int selectedId = bottomNav.getSelectedItemId();
            View selectedView = bottomNav.findViewById(selectedId);
            if (selectedView != null) {
                selectedView.setTranslationY(-8f);
                selectedView.setScaleX(1.12f);
                selectedView.setScaleY(1.12f);
            }
        });
    }

    public static void animateItemClick(BottomNavigationView bottomNav, int selectedItemId) {
        if (bottomNav == null) return;

        View selectedView = bottomNav.findViewById(selectedItemId);
        if (selectedView != null) {
            selectedView.animate()
                    .translationY(-10f)
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(2.0f))
                    .start();
        }

        for (int i = 0; i < bottomNav.getMenu().size(); i++) {
            int otherId = bottomNav.getMenu().getItem(i).getItemId();
            if (otherId != selectedItemId) {
                View otherView = bottomNav.findViewById(otherId);
                if (otherView != null) {
                    otherView.animate()
                            .translationY(0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start();
                }
            }
        }
    }
}
