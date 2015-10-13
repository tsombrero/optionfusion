package com.mosoft.momomentum.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

import retrofit.Callback;
import retrofit.Response;

public class OptionChainProvider extends LruCache<String, OptionChain> {

    private final Context context;
    private final AmeritradeClient ameritradeClient;

    public OptionChainProvider(Context context, AmeritradeClient ameritradeClient) {
        super(10);
        this.context = context;
        this.ameritradeClient = ameritradeClient;
    }

    public void get(final String symbol, final OptionChainCallback callback) {
        OptionChain ret = get(symbol);
        if (ret != null) {
            callback.call(ret);
            return;
        }

        ameritradeClient.getOptionChain(symbol).enqueue(new Callback<OptionChain>() {
            @Override
            public void onResponse(Response<OptionChain> response) {
                if (!response.isSuccess()) {
                    Log.w("tag", "Failed: " + response.message());
                    return;
                }

                OptionChain oc = response.body();

                if (!oc.succeeded()) {
                    Log.w("tag", "Failed: " + oc.getError());
                    return;
                }

                Log.i("tag", "Got option chain: " + oc);

                put(symbol, oc);

                callback.call(oc);
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(context, "Failed getting option chain", Toast.LENGTH_SHORT);
                callback.call(null);
            }
        });
    }

    public interface OptionChainCallback {
        void call(OptionChain optionChain);
    }
}
