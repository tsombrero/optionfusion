package com.mosoft.optionfusion.client;

import android.util.Log;

import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.model.provider.yhoo.YhooStockQuote;

import java.io.IOException;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.Query;

public class YhooClient implements ClientInterfaces.StockQuoteClient {
    private final static String yqlStockQueryFormat = "select * from yahoo.finance.quotes where symbol in ('%s')";
    private final RestInterface restInterface;
    private final static String TAG = YhooClient.class.getSimpleName();

    public YhooClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }

    @Override
    public Interfaces.StockQuote getStockQuote(String symbol, final ClientInterfaces.Callback<Interfaces.StockQuote> callback) {
        Call<YhooStockQuote> quoteCall = restInterface.getStockQuote(String.format(yqlStockQueryFormat, symbol));
        if (callback == null) {
            try {
                Response<YhooStockQuote> response = quoteCall.execute();
                return response.body();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //TODO more error handling
            Log.e(TAG, "Failed getting stock quote");
            return null;
        }

        quoteCall.enqueue(new Callback<YhooStockQuote>() {
            @Override
            public void onResponse(Response<YhooStockQuote> response, Retrofit retrofit) {
                if (response.isSuccess()) {
                    callback.call(response.body());
                }
                Log.e(TAG, "Failed!");
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failed!", t);
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
