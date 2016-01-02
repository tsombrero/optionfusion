package com.mosoft.optionfusion.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.model.provider.Interfaces;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class StockQuoteProvider extends LruCache<String, Interfaces.StockQuote> {
    private final Context context;
    private final ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final String TAG = OptionChainProvider.class.getSimpleName();

    ArrayList<WeakReference<StockQuoteCallback>> stockQuoteListeners = new ArrayList<>();


    public StockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        super(10);
        this.context = context;
        this.stockQuoteClient = stockQuoteClient;
    }

    public void get(final String symbol, final StockQuoteCallback callback) {
        if (symbol == null)
            return;

        Interfaces.StockQuote ret = get(symbol);
        if (ret != null) {
            callback.call(ret);
            return;
        }

        stockQuoteClient.getStockQuote(symbol, new ClientInterfaces.Callback<Interfaces.StockQuote>() {
                    @Override
                    public void call(Interfaces.StockQuote quote) {
                        if (quote == null) {
                            Log.w(TAG, "Failed getting quote for " + symbol);
                            callback.call(null);
                            return;
                        }

                        Log.i("tag", "Got stock quote: " + quote);

                        put(symbol, quote);

                        callback.call(quote);
                    }

                    @Override
                    public void onError(int status, String message) {
                        Log.w("tag", "Failed: " + status + " " + message);
                        Toast.makeText(context, "Failed getting stock quote", Toast.LENGTH_SHORT);
                        callback.call(null);

                    }
                }
        );
    }

    public interface StockQuoteCallback {
        void call(Interfaces.StockQuote stockQuote);
    }
}
