package com.mosoft.optionfusion.client;

import com.squareup.okhttp.OkHttpClient;

import java.net.CookieManager;
import java.net.CookiePolicy;

import retrofit.Retrofit;
import retrofit.SimpleXmlConverterFactory;

public class AmeritradeClientProvider extends ClientProvider implements ClientProvider.OptionChainClientProvider, ClientProvider.BrokerageClientProvider {

    private AmeritradeClient client;

    private synchronized AmeritradeClient getClient() {
        if (client == null) {
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

            client = new AmeritradeClient(retrofit.create(AmeritradeClient.RestInterface.class));
        }

        return client;
    }

    @Override
    public ClientInterfaces.BrokerageClient getBrokerageClient() {
        return getClient();
    }

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient() {
        return getClient();
    }
}
