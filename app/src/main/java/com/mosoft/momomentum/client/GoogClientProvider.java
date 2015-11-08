package com.mosoft.momomentum.client;

import com.mosoft.momomentum.module.MomentumApplication;
import com.squareup.okhttp.OkHttpClient;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class GoogClientProvider extends ClientProvider implements ClientProvider.OptionChainClientProvider {

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient(MomentumApplication application) {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.interceptors().add(new LoggingInterceptor());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.google.com/finance/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        GoogClient ret = new GoogClient(retrofit.create(GoogClient.RestInterface.class));

        application.getComponent().inject(ret);

        return ret;
    }
}
