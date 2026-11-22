package com.example.aquatech;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

public class CustomerDashboardNavHelper {

    public static void setupBottomNavigation(Activity activity) {
        if (activity == null) {
            return;
        }

        View navHome = activity.findViewById(R.id.navCustHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                View sv = activity.findViewById(R.id.customerScrollView);
                if (sv != null) {
                    sv.scrollTo(0, 0);
                }
            });
        }

        View navRequest = activity.findViewById(R.id.navCustRequest);
        if (navRequest != null) {
            navRequest.setOnClickListener(v -> activity.startActivity(new Intent(activity, ServiceLogActivity.class)));
        }

        View navMessages = activity.findViewById(R.id.navCustMessages);
        if (navMessages != null) {
            navMessages.setOnClickListener(v -> activity.startActivity(new Intent(activity, ChatListTech.class)));
        }

        View navHistory = activity.findViewById(R.id.navCustHistory);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> activity.startActivity(new Intent(activity, ServiceHistoryActivity.class)));
        }

        View navProfile = activity.findViewById(R.id.navCustProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> activity.startActivity(new Intent(activity, CustomerProfileActivity.class)));
        }
    }
}
