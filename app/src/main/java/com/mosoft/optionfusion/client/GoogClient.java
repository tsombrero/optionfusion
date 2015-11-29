package com.mosoft.optionfusion.client;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.util.Log;

import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.model.provider.goog.GoogOptionChain;
import com.mosoft.optionfusion.model.provider.goog.GoogSymbolLookupResult;

import java.io.IOException;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public class GoogClient implements ClientInterfaces.OptionChainClient, ClientInterfaces.SymbolLookupClient {

    private final RestInterface restInterface;

    ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final static String TAG = GoogClient.class.getSimpleName();

    public GoogClient(RestInterface restInterface, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        this.restInterface = restInterface;
        this.stockQuoteClient = stockQuoteClient;
    }

    @Override
    public void getOptionChain(String symbol, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        new OptionChainTask(callback).execute(symbol);
    }

    public Interfaces.StockQuote getStockQuote(String symbol) {
        return stockQuoteClient.getStockQuote(symbol, null);
    }

    @Override
    public Cursor getSymbolsMatching(String query) {
        try {
            Response<GoogSymbolLookupResult> result = this.restInterface.getMatchesForQuery(query).execute();
            if (result == null || result.body() == null)
                return ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR;

            return result.body().getResultCursor();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR;
    }

    private class OptionChainTask extends AsyncTask<String, Void, GoogOptionChain> {

        private final ClientInterfaces.Callback<Interfaces.OptionChain> callback;

        public OptionChainTask(ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
            this.callback = callback;
        }

        @Override
        protected GoogOptionChain doInBackground(String... params) {
            String symbol = params[0];

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

        @Override
        protected void onPostExecute(GoogOptionChain chain) {
            callback.call(chain);
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
    }


}
