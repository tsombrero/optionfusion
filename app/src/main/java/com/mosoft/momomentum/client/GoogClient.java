package com.mosoft.momomentum.client;

import android.os.AsyncTask;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.goog.GoogOptionChain;

import java.io.IOException;

import retrofit.Call;
import retrofit.Response;
import retrofit.http.GET;
import retrofit.http.Query;

public class GoogClient implements ClientInterfaces.OptionChainClient {

    private final RestInterface restInterface;
    private final static String TAG = GoogClient.class.getSimpleName();

    public GoogClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }

    @Override
    public void getOptionChain(String symbol, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        new OptionChainTask(callback).execute(symbol);
    }

    private class OptionChainTask extends AsyncTask<String, Void, GoogOptionChain> {

        private final ClientInterfaces.Callback<Interfaces.OptionChain> callback;

        public OptionChainTask(ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
            this.callback = callback;
        }

        @Override
        protected GoogOptionChain doInBackground(String... params) {
            String symbol = params[0];
            GoogOptionChain ret = new GoogOptionChain();

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
    }


}
