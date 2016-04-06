package com.optionfusion.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import javax.inject.Inject;

public class SharedPrefStore {

    private static final String ACCOUNT_NAME = "ACCOUNT_NAME";
    private SharedPreferences settings;

    private static final String TAG = "SharedPrefStore";

    @Inject
    public SharedPrefStore(Context context) {
        settings = context.getSharedPreferences(TAG, 0);
    }

    public void setAccountName(String accountName) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_NAME, accountName);
        editor.commit();
    }

    public String getEmail() {
        return settings.getString(ACCOUNT_NAME, null);
    }

    public boolean isUserAuthenticated() {
        return !TextUtils.isEmpty(getEmail());
    }
}
