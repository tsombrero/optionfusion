package com.optionfusion.client;

import android.os.AsyncTask;
import android.util.Log;

import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.goog.GoogOptionChain;
import com.optionfusion.model.provider.goog.GoogPriceHistory;
import com.optionfusion.model.provider.goog.GoogSymbolLookupResult;
import com.optionfusion.util.Util;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public class GoogClient implements ClientInterfaces.OptionChainClient, ClientInterfaces.SymbolLookupClient, ClientInterfaces.PriceHistoryClient {

    private final RestInterface restInterface;

    ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final static String TAG = GoogClient.class.getSimpleName();

    public GoogClient(RestInterface restInterface, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        this.restInterface = restInterface;
        this.stockQuoteClient = stockQuoteClient;
    }

    @Override
    public Interfaces.OptionChain getOptionChain(String symbol, boolean favoritesOnly) {
        Interfaces.StockQuote quote = getStockQuote(symbol);

        // TODO decouple quote from chain
        if (quote == null) {
            Log.e(TAG, "Failed getting option chain because quote is empty");
            return null;
        }

        GoogOptionChain ret = new GoogOptionChain();
        ret.setStockQuote(quote);
        try {
            Response<GoogOptionChain.GoogExpirations> expirations = restInterface.getExpirations(symbol).execute();

            if (expirations == null || expirations.body() == null || expirations.body().getExpirations() == null)
                return null;

            for (GoogOptionChain.GoogExpiration expiration : expirations.body().getExpirations()) {
                Response<GoogOptionChain.GoogOptionDate> datechain = restInterface.getChainForDate(symbol, expiration.getY(), expiration.getM(), expiration.getD()).execute();
                ret.addToChain(datechain.body());
            }
            ret.setSucceeded(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public Interfaces.StockQuote getStockQuote(String symbol) {
        return stockQuoteClient.getStockQuote(symbol, null);
    }

    @Override
    public List<ClientInterfaces.SymbolLookupResult> getSymbolsMatching(String query) {
        try {
            Response<GoogSymbolLookupResult> result = this.restInterface.getMatchesForQuery(query).execute();
            if (result == null || result.body() == null)
                return Collections.EMPTY_LIST;

            return result.body().getResultList();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void getPriceHistory(String symbol, Date start, ClientInterfaces.Callback<Interfaces.StockPriceHistory> callback) {
        new PriceHistoryTask(callback, symbol, start).execute();

    }

    private class PriceHistoryTask extends AsyncTask<String, Void, GoogPriceHistory> {
        private final ClientInterfaces.Callback<Interfaces.StockPriceHistory> callback;
        private final String symbol;
        private final Date start;

        public PriceHistoryTask(ClientInterfaces.Callback<Interfaces.StockPriceHistory> callback, String symbol, Date start) {
            this.callback = callback;
            this.symbol = symbol;
            this.start = start;
        }

        @Override
        protected GoogPriceHistory doInBackground(String... params) {
            String period;
            long dateDiff = System.currentTimeMillis() - start.getTime();

            int years = Util.roundUp(dateDiff, TimeUnit.DAYS.toMillis(365), 0, 1, 2, 3, 5, 10, 20, 40);

            if (years == 0) {
                int days = Util.roundUp(dateDiff, TimeUnit.DAYS.toMillis(1), 1, 2, 3, 5, 10, 15, 30, 60, 90, 120, 240, 480);
                if (days == 0)
                    days = 1;
                period = String.format("%dd", days);
            } else {
                period = String.format("%dY", years);
            }

            Call<GoogPriceHistory> request = restInterface
                    .getPriceHistory(
                            symbol,
                            period,
                            Interfaces.StockPriceHistory.Interval.forStartDate(start).getIntervalInSeconds());

            try {
                Response<GoogPriceHistory> response = request.execute();
                return response.body();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public interface RestInterface {

        // http://www.google.com/finance/option_chain?q=GOOG&output=json
        @GET("option_chain?output=json")
        Call<GoogOptionChain.GoogExpirations> getExpirations(@Query("q") String symbol);

        // http://www.google.com/finance/option_chain?q=GOOG&expd=17&expm=1&expy=2015&output=json
        @GET("option_chain?output=json")
        Call<GoogOptionChain.GoogOptionDate> getChainForDate(@Query("q") String symbol, @Query("expy") int year, @Query("expm") int month, @Query("expd") int day);

        // http://www.google.com/finance/match?matchtype=matchall&q=foo
        @GET("match?matchtype=matchall&output=json")
        Call<GoogSymbolLookupResult> getMatchesForQuery(@Query("q") String symbol);

        //https://www.google.com/finance/getprices?q=MSFT&i=604800&p=40Y&f=d,c,v,o,h,l&df=cpct&auto=1
        @GET("getprices?f=d,c,v,o,h,l&df=cpct&auto=1")
        Call<GoogPriceHistory> getPriceHistory(@Query("q") String symbol, @Query("p") String period, @Query("i") long interval);

        // https://www.google.com/finance/kd?output=json&keydevs=1&sort=date&recnews=0&s=MSFT
//        Call<StockNews> getNews(@Query("s") String symbol);
    }

}
