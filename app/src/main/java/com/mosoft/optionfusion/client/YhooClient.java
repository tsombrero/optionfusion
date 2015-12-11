package com.mosoft.optionfusion.client;

import android.text.TextUtils;
import android.util.Log;

import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.model.provider.yhoo.YhooStockQuote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.Query;

public class YhooClient implements ClientInterfaces.StockQuoteClient {
    private final static String yqlStockQueryFormat = "select symbol,Name,Bid,Ask,LastTradePriceOnly,Open,PreviousClose from yahoo.finance.quotes where symbol in ('%s')";
    private final RestInterface restInterface;
    private final static String TAG = YhooClient.class.getSimpleName();

    public YhooClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }

    @Override
    public Interfaces.StockQuote getStockQuote(String symbol, final ClientInterfaces.Callback<Interfaces.StockQuote> callback) {
        if (callback == null) {
            List<Interfaces.StockQuote> ret = getStockQuotes(Collections.singletonList(symbol), null);
            if (ret != null && !ret.isEmpty()) {
                return ret.get(0);
            }
            return null;
        }

        getStockQuotes(Collections.singletonList(symbol), new ClientInterfaces.Callback<List<Interfaces.StockQuote>>() {
            @Override
            public void call(List<Interfaces.StockQuote> quotes) {
                if (quotes == null || quotes.isEmpty())
                    return;

                callback.call(quotes.get(0));
            }

            @Override
            public void onError(int status, String message) {
                Log.i(TAG, "getStockQuote error: " + message);
            }
        });

        return null;
    }

    @Override
    public List<Interfaces.StockQuote> getStockQuotes(Collection<String> symbols, final ClientInterfaces.Callback<List<Interfaces.StockQuote>> callback) {
        String symbolList = TextUtils.join("','", symbols);
        Call<YhooStockQuote> quoteCall = restInterface.getStockQuote(String.format(yqlStockQueryFormat, symbolList));
        if (callback == null) {
            try {
                Response<YhooStockQuote> response = quoteCall.execute();
                ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();
                List<YhooStockQuote.QuoteData> quotes = response.body().getQuotes();
                if (quotes != null)
                    ret.addAll(quotes);
                return ret;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //TODO more error handling
            Log.e(TAG, "Failed getting stock quotes");
            return null;
        }

        quoteCall.enqueue(new Callback<YhooStockQuote>() {
            @Override
            public void onResponse(Response<YhooStockQuote> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();
                    ret.addAll(response.body().getQuotes());
                    callback.call(ret);
                }
                Log.e(TAG, "Failed!");
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failed!", t);
                callback.onError(0, t.getMessage());
            }
        });
        return null;
    }

    public interface RestInterface {
        //http://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(%22GOOG%22)&env=http%3A%2F%2Fdatatables.org%2Falltables.env&format=json
        @GET("yql?env=http%3A%2F%2Fdatatables.org%2Falltables.env&format=json")
        Call<YhooStockQuote> getStockQuote(@Query("q") String query);
    }
}
