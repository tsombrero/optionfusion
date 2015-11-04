package com.mosoft.momomentum.client;

import android.util.Log;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeLoginResponse;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeOptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

import java.util.List;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public class AmeritradeClient implements ClientInterfaces.BrokerageClient {

    private RestInterface restInterface;
    private String sessionId;

    private static String TAG = "AmeritradeClient";

    public Response<AmeritradeLoginResponse> response;

    AmeritradeClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }

    @Override
    public void logIn(String userId, String password, final ClientInterfaces.Callback<ClientInterfaces.LoginResponse> callback) {
        final Call<AmeritradeLoginResponse> callable = restInterface.logIn(userId, password, "JKRR", "1.0");
        callable.enqueue(new Callback<AmeritradeLoginResponse>() {

            @Override
            public void onResponse(Response<AmeritradeLoginResponse> response) {
                AmeritradeClient.this.response = response;
                setSessionId(response.body().getSessionId());
                callback.call(response.body());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failed", t);
            }
        });
    }

    @Override
    public void getOptionChain(String symbol, final ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        Call<AmeritradeOptionChain> callable = restInterface.getOptionChain(symbol);
        callable.enqueue(new Callback<AmeritradeOptionChain>(){
            @Override
            public void onResponse(Response<AmeritradeOptionChain> response) {
                callback.call(response.body());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Failed", t);
            }
        });
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public interface RestInterface {
        @FormUrlEncoded
        @POST("100/LogIn?source=JKRR&version=1.0")
        Call<AmeritradeLoginResponse> logIn(@Field("userid") String userid, @Field("password") String password, @Field("source") String source, @Field("version") String version);

        //https://apis.tdameritrade.com/apps/200/OptionChain?source=<#sourceID# >&symbolView=AMTD&expire=200709&quotes=true
        @GET("200/OptionChain?source=JKRR&quotes=true&range=ALL")
        Call<AmeritradeOptionChain> getOptionChain(@Query("symbol") String symbol);
    }

    @Override
    public void getAccounts(ClientInterfaces.Callback<List<? extends Interfaces.Account>> callback) {
        callback.call(response.body().getAccounts());
    }

    @Override
    public boolean isAuthenticated() {
        return response.isSuccess();
    }
}
