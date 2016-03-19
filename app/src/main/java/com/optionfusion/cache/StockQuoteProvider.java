package com.optionfusion.cache;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.model.provider.Interfaces;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StockQuoteProvider extends LruCache<String, Interfaces.StockQuote> {
    private final Context context;
    private final ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final String TAG = OptionChainProvider.class.getSimpleName();

    ArrayList<WeakReference<StockQuoteCallback>> stockQuoteListeners = new ArrayList<>();

    HashMap<String, Long> timeLastRequested = new HashMap<>();


    public StockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        super(50);
        this.context = context;
        this.stockQuoteClient = stockQuoteClient;
    }

    public void get(Collection<String> symbols, final StockQuoteCallback callback) {

        ArrayList<Interfaces.StockQuote> quotes = new ArrayList<>();

        boolean needsUpdate = false;

        if (symbols != null) { //TODO make the caller pass in the symbols
            for (String symbol : symbols) {
                Interfaces.StockQuote quote = get(symbol);
                if (quote != null) {
                    quotes.add(quote);
                    needsUpdate |= System.currentTimeMillis() - quote.getLastUpdatedTimestamp() > TimeUnit.SECONDS.toMillis(30);
                } else {
                    needsUpdate = true;
                }
            }

            if (!needsUpdate) {
                callback.call(quotes);
                return;
            }
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
