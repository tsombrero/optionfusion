package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.client.AmeritradeClientProvider;

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

    @Provides
    @Singleton
    AmeritradeClient getAmeritradeClient() {
        return new AmeritradeClientProvider().getClient();
    }

    @Provides
    @Singleton
    OptionChainProvider getOptionChainProvider(Context context, AmeritradeClient ameritradeClient) {
        return new OptionChainProvider(context, ameritradeClient);
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
