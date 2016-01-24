package com.optionfusion.cache;

import android.util.Log;
import android.util.LruCache;

import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.model.provider.Interfaces;

import java.util.Date;

public class PriceHistoryProvider extends LruCache<String, Interfaces.StockPriceHistory> {

    private final ClientInterfaces.PriceHistoryClient priceHistoryClient;
    private static final String TAG = "PriceHistoryProvider";

    public PriceHistoryProvider(ClientInterfaces.PriceHistoryClient priceHistoryClient) {
        super(20);
        this.priceHistoryClient = priceHistoryClient;
    }

    void getStockPriceHistory(String symbol, Date startDate, final StockPriceHistoryCallback callback) {
        Interfaces.StockPriceHistory.Interval interval
                = Interfaces.StockPriceHistory.Interval.forStartDate(startDate);

        final String key = getKey(symbol, interval);

        Interfaces.StockPriceHistory ret = get(key);

        boolean needsRefresh = ret == null;

        needsRefresh |= ret.getAgeOfLastEntryMs() >= interval.maxAgeBeforeStaleMs();

        if (ret != null)
            callback.call(ret);

        if (needsRefresh) {
            priceHistoryClient.getPriceHistory(symbol, startDate, new ClientInterfaces.Callback<Interfaces.StockPriceHistory>() {
                @Override
                public void call(Interfaces.StockPriceHistory sph) {
                    put(key, sph);
                    callback.call(sph);
                }

                @Override
                public void onError(int status, String message) {
                    Log.e(TAG, "Failed getting price history: " + status + " " + message);
                }
            });
        }
    }

    String getKey(String symbol, Interfaces.StockPriceHistory.Interval interval) {
        return symbol + "|||" + interval;
    }

    public interface StockPriceHistoryCallback {
        void call(Interfaces.StockPriceHistory priceHistory);
    }
}
