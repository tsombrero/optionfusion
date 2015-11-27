package com.mosoft.optionfusion.model.provider;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mosoft.optionfusion.model.provider.amtd.AmeritradeOptionChain;
import com.mosoft.optionfusion.model.provider.goog.GoogOptionChain;
import com.mosoft.optionfusion.module.MomentumApplication;

public class ClassFactory {
    private static final String TAG = "Provider.ClassFactory";

    private static final Gson gson = new GsonBuilder().create();

    public static Interfaces.OptionQuote OptionQuoteFromJson(MomentumApplication.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, AmeritradeOptionChain.OptionQuote.class);
                case GOOGLE_FINANCE:
                    return gson.fromJson(json, GoogOptionChain.GoogOptionQuote.class);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }


    public static Interfaces.OptionChain OptionChainFromJson(MomentumApplication.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, AmeritradeOptionChain.class);
                case GOOGLE_FINANCE:
                    return gson.fromJson(json, GoogOptionChain.class);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }
}
