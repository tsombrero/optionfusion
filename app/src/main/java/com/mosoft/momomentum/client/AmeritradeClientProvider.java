package com.mosoft.momomentum.client;

import com.mosoft.momomentum.module.MomentumApplication;
import com.squareup.okhttp.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;

import retrofit.Retrofit;
import retrofit.SimpleXmlConverterFactory;

public class AmeritradeClientProvider extends ClientProvider implements ClientProvider.OptionChainClientProvider, ClientProvider.BrokerageClientProvider {

    private AmeritradeClient getClient(MomentumApplication application) {

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.interceptors().add(new LoggingInterceptor());

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        okHttpClient.setCookieHandler(cookieManager);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://apis.tdameritrade.com/apps/")
                .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
                .client(okHttpClient)
                .build();

        AmeritradeClient ret = new AmeritradeClient(retrofit.create(AmeritradeClient.RestInterface.class));

        application.getComponent().inject(ret);

        return ret;
    }


    @Override
    public ClientInterfaces.BrokerageClient getBrokerageClient(MomentumApplication application) {
        return getClient(application);
    }

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient(MomentumApplication application) {
        return getClient(application);
    }
}
