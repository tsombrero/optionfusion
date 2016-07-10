package com.optionfusion.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import javax.inject.Inject;

public class SharedPrefStore {

    private static final String ACCOUNT_EMAIL = "ACCOUNT_EMAIL";
    private static final String SESSION_ID = "SESSION_ID";
    private SharedPreferences settings;

    private static final String TAG = "SharedPrefStore";

    @Inject
    public SharedPrefStore(Context context) {
        settings = context.getSharedPreferences(TAG, 0);
    }

    public void setEmail(String email) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_EMAIL, email);
        editor.commit();
    }

    public String getSessionId() {
        return settings.getString(SESSION_ID, null);
    }

    public void setSessionid(String sessionid) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SESSION_ID, sessionid);
        editor.commit();
    }

    public String getEmail() {
        return settings.getString(ACCOUNT_EMAIL, null);
    }

    public boolean isUserAuthenticated() {
        return !TextUtils.isEmpty(getEmail());
    }
}
