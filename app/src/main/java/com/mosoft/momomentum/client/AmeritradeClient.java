package com.mosoft.momomentum.client;

import com.mosoft.momomentum.model.AmtdResponse;
import com.mosoft.momomentum.model.LoginResponse;

import retrofit.Call;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

public class AmeritradeClient {

    private RestInterface restInterface;


    AmeritradeClient(RestInterface restInterface) {
        this.restInterface = restInterface;
    }


    public Call<LoginResponse> logIn(String userId, String password) {
        return restInterface.logIn(userId, password, "JKRR", "1.0");
    }


    public interface RestInterface {
        @FormUrlEncoded
        @POST("LogIn?source=JKRR&version=1.0")
        Call<LoginResponse> logIn(@Field("userid") String userid, @Field("password") String password, @Field("source") String source, @Field("version") String version);
    }
}
