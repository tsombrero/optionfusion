package com.mosoft.momomentum.client;

import com.mosoft.momomentum.model.provider.amtd.LoginResponse;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

import retrofit.Call;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public class AmeritradeClient {

    private RestInterface restInterface;
    private String sessionId;

    AmeritradeClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }

    public Call<LoginResponse> logIn(String userId, String password) {
        return restInterface.logIn(userId, password, "JKRR", "1.0");
    }

    public Call<OptionChain> getOptionChain(String symbol) {
        return restInterface.getOptionChain(symbol);
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public interface RestInterface {
        @FormUrlEncoded
        @POST("100/LogIn?source=JKRR&version=1.0")
        Call<LoginResponse> logIn(@Field("userid") String userid, @Field("password") String password, @Field("source") String source, @Field("version") String version);

        //https://apis.tdameritrade.com/apps/200/OptionChain?source=<#sourceID# >&symbolView=AMTD&expire=200709&quotes=true
        @GET("200/OptionChain?source=JKRR&quotes=true&range=ALL")
        Call<OptionChain> getOptionChain(@Query("symbol") String symbol);
    }
}
