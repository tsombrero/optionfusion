package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.client.AmeritradeClientProvider;
import com.mosoft.momomentum.client.ClientInterfaces;
import com.mosoft.momomentum.client.GoogClientProvider;
import com.mosoft.momomentum.client.YhooClientClientProvider;

import net.danlew.android.joda.JodaTimeAndroid;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final Application application;

    ApplicationModule(Application application) {
        this.application = application;
        JodaTimeAndroid.init(application);
    }

    @Provides
    @Singleton
    Application application() {
        return application;
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    ClientInterfaces.OptionChainClient provideOptionChainClient(Context context, AmeritradeClientProvider ameritradeClientProvider, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        switch(MomentumApplication.from(context).getProvider()) {
            case AMERITRADE:
                return ameritradeClientProvider.getOptionChainClient();
        }
        return new GoogClientProvider().getOptionChainClient(stockQuoteClient);
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    ClientInterfaces.BrokerageClient provideBrokerageClient(Context context, AmeritradeClientProvider ameritradeClientProvider) {
        switch(MomentumApplication.from(context).getProvider()) {
            case AMERITRADE:
                return ameritradeClientProvider.getBrokerageClient();
        }
        return null;
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    ClientInterfaces.StockQuoteClient provideStockQuoteClient(Context context, AmeritradeClientProvider ameritradeClientProvider, YhooClientClientProvider yhooClientProvider) {
        switch(MomentumApplication.from(context).getProvider()) {
            case AMERITRADE:
                //TODO
            default:
                return yhooClientProvider.getStockQuoteClient();
        }
    }

    @Provides
    @Singleton
    AmeritradeClientProvider provideAmeritradeClientProvider() {
        return new AmeritradeClientProvider();
    }

    @Provides
    @Singleton
    GoogClientProvider provideGoogClientProvider() {
        return new GoogClientProvider();
    }

    @Provides
    @Singleton
    YhooClientClientProvider provideYhooClientProvider() { return new YhooClientClientProvider(); }

    @Provides
    @Singleton
    OptionChainProvider getOptionChainProvider(Context context, ClientInterfaces.OptionChainClient client) {
        return new OptionChainProvider(context, client);
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder().create();
    }
}
