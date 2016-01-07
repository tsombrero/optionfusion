package com.mosoft.optionfusion.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.model.provider.Interfaces;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StockQuoteProvider extends LruCache<String, Interfaces.StockQuote> {
    private final Context context;
    private final ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final String TAG = OptionChainProvider.class.getSimpleName();

    ArrayList<WeakReference<StockQuoteCallback>> stockQuoteListeners = new ArrayList<>();


    public StockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        super(50);
        this.context = context;
        this.stockQuoteClient = stockQuoteClient;
    }

    public void get(Collection<String> symbols, final StockQuoteCallback callback) {

        ArrayList<Interfaces.StockQuote> quotes = new ArrayList<>();

        boolean needsUpdate = false;

        for (String symbol : symbols) {
            Interfaces.StockQuote quote = get(symbol);
            if (quote != null) {
                quotes.add(quote);
                needsUpdate |= System.currentTimeMillis() - quote.getLastUpdatedTimestamp() > TimeUnit.SECONDS.toMillis(30);
            } else {
                needsUpdate = true;
            }
        }

        callback.call(quotes);

        if (!needsUpdate) {
            return;
        }

        stockQuoteClient.getStockQuotes(symbols, new ClientInterfaces.Callback<List<Interfaces.StockQuote>>() {
            @Override
            public void call(List<Interfaces.StockQuote> quotes) {
                for (Interfaces.StockQuote quote : quotes) {
                    put(quote.getSymbol(), quote);
                }
                callback.call(quotes);
            }

            @Override
            public void onError(int status, String message) {
                Log.w("tag", "Failed: " + status + " " + message);
                Toast.makeText(context, "Failed getting stock quote", Toast.LENGTH_SHORT);
                callback.call(null);
            }
        });
    }

    public abstract static class StockQuoteCallback extends ClientInterfaces.Callback<List<Interfaces.StockQuote>> {
    }
}
