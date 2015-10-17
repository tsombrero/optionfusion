package com.mosoft.momomentum.model.provider;

import android.util.Log;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

public class ClassFactory {
    private static final String TAG = "Provider.ClassFactory";

    public static Interfaces.OptionQuote OptionQuoteFromJson(Gson gson, Interfaces.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, OptionChain.OptionQuote.class);
                case GOOGGLE_FINANCE:
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }


    public static OptionChain OptionChainFromJson(Gson gson, Interfaces.Provider provider, String json) {
        try {
            switch (provider) {
                case AMERITRADE:
                    return gson.fromJson(json, OptionChain.class);
                case GOOGGLE_FINANCE:
                    break;
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }
}
