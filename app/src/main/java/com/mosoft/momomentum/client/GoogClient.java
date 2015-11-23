package com.mosoft.momomentum.client;

import android.os.AsyncTask;
import android.util.Log;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.goog.GoogOptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public class GoogClient implements ClientInterfaces.OptionChainClient {

    private final RestInterface restInterface;

    //TODO decouple stockquote from optionchain
    public void setStockQuoteClient(ClientInterfaces.StockQuoteClient stockQuoteClient) {
        this.stockQuoteClient = stockQuoteClient;
    }

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

        // http://google.com/finance/match?matchtype=matchall&q=foo
        // @GET SYMBOL LOOKUP
    }


}
