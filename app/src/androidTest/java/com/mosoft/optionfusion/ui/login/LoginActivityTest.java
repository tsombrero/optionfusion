package com.mosoft.optionfusion.ui.login;

import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.mosoft.optionfusion.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityTestRule<LoginActivity> activityRule = new ActivityTestRule<>(LoginActivity.class);

    @Test
    public void testStartLogin() throws Exception {
        onView(ViewMatchers.withId(R.id.no_thanks)).perform(click());
        onView(ViewMatchers.withId(R.id.fab)).perform(click());
        Thread.sleep(5000);
    }
}