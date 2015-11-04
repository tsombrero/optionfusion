package com.mosoft.momomentum.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.mosoft.momomentum.client.ClientInterfaces;
import com.mosoft.momomentum.model.provider.Interfaces;

public class OptionChainProvider extends LruCache<String, Interfaces.OptionChain> {

    private final Context context;
    private final ClientInterfaces.BrokerageClient brokerageClient;

    public OptionChainProvider(Context context, ClientInterfaces.BrokerageClient brokerageClient) {
        super(10);
        this.context = context;
        this.brokerageClient = brokerageClient;
    }

    public void get(final String symbol, final OptionChainCallback callback) {
        Interfaces.OptionChain ret = get(symbol);
        if (ret != null) {
            callback.call(ret);
            return;
        }

        brokerageClient.getOptionChain(symbol, new ClientInterfaces.Callback<Interfaces.OptionChain>() {
                    @Override
                    public void call(Interfaces.OptionChain oc) {
                        if (oc == null || !oc.succeeded()) {
                            Log.w("tag", "Failed: " + oc.getError());
                            callback.call(null);
                            return;
                        }

                        Log.i("tag", "Got option chain: " + oc);

                        put(symbol, oc);

                        callback.call(oc);
                    }

                    @Override
                    public void onError(int status, String message) {
                        Log.w("tag", "Failed: " + status + " " + message);
                        Toast.makeText(context, "Failed getting option chain", Toast.LENGTH_SHORT);
                        callback.call(null);

                    }
                }
        );
    }

    public interface OptionChainCallback {
        void call(Interfaces.OptionChain optionChain);
    }
}
