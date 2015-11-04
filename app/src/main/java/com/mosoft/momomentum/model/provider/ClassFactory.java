package com.mosoft.momomentum.model.provider;

import android.util.Log;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeOptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

public class ClassFactory {
    private static final String TAG = "Provider.ClassFactory";

    public static Interfaces.OptionQuote OptionQuoteFromJson(Gson gson, MomentumApplication.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, AmeritradeOptionChain.OptionQuote.class);
                case GOOGLE_FINANCE:
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }


    public static AmeritradeOptionChain OptionChainFromJson(Gson gson, MomentumApplication.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, AmeritradeOptionChain.class);
                case GOOGLE_FINANCE:
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }
}
