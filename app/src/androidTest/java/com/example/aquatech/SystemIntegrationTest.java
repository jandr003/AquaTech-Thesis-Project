package com.example.aquatech;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.anything;

import android.content.Intent;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SystemIntegrationTest {

    @Rule
    public ActivityScenarioRule<SplashActivity> activityRule =
            new ActivityScenarioRule<>(SplashActivity.class);

    private void waitTime(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }

    @Test
    public void testFullSystemFlow() {
        // --- STEP 1: CUSTOMER SIGNUP & REQUEST ---
        waitTime(12000); // Wait for Splash animations
        
        onView(withId(R.id.tvDontHaveAccount))
                .check(matches(isDisplayed()))
                .perform(click());

        // Fill up fields with unique data (timestamp ensures bypass triggers)
        String timestamp = String.valueOf(System.currentTimeMillis());
        onView(withId(R.id.etUsername)).perform(typeText("user_" + timestamp), closeSoftKeyboard());
        onView(withId(R.id.etFullName)).perform(typeText("Juan Dela Cruz"), closeSoftKeyboard());
        onView(withId(R.id.etEmail)).perform(typeText("test_" + timestamp + "@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.etMobile)).perform(typeText("09123456789"), closeSoftKeyboard());
        onView(withId(R.id.etAddress)).perform(typeText("Manila, Philippines"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("Password123!"), closeSoftKeyboard());
        
        onView(isRoot()).perform(closeSoftKeyboard());
        
        try {
            onView(withId(R.id.btnSignUp)).perform(scrollTo(), click());
        } catch (Exception e) {
            onView(withId(R.id.btnSignUp)).perform(click());
        }

        // Wait for Bypass to trigger
        waitTime(5000); 

        // Unit Selection Screen
        onView(withId(R.id.whiteCardItem1)).perform(click());
        
        waitTime(3000);
        onView(withId(R.id.etUnitNumber)).perform(typeText("A-101"), closeSoftKeyboard());
        
        // SELECT PURCHASE TYPE from Spinner (REQUIRED)
        onView(withId(R.id.purchaseTypeDropdown)).perform(click());
        onData(anything()).atPosition(1).perform(click()); // Select SUBSCRIPTION
        
        onView(withId(R.id.btnConfirm)).perform(click());

        // --- CUSTOMER DASHBOARD ---
        waitTime(8000); 
        onView(withId(R.id.requestButton)).perform(click());
        
        waitTime(2000);
        onView(withId(R.id.buttonNext)).perform(click());
        onView(withId(R.id.buttonNext2)).perform(click());
        onView(withId(R.id.buttonNext3)).perform(click());
        onView(withId(R.id.remarksInput)).perform(typeText("Demo Request"), closeSoftKeyboard());
        onView(withId(R.id.buttonSubmit)).perform(click());
        
        waitTime(5000);

        // --- STEP 2: VERIFY AS ADMIN (OPTIONAL IF BYPASS IS READY) ---
        activityRule.getScenario().onActivity(activity -> {
            Intent intent = new Intent(activity, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        });

        waitTime(12000);
        onView(withId(R.id.etLoginInput)).perform(typeText("mgmt"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
        waitTime(5000);
        onView(withId(R.id.techProfileAvatar)).perform(click());
        onView(withText("PROCEED AS ADMIN")).perform(click());
        waitTime(3000);
        onView(withId(R.id.cardOpen)).check(matches(isDisplayed()));
    }
}
