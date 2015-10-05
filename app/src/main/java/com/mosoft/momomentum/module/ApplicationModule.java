package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;

import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.client.AmeritradeClientProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final Application application;

    ApplicationModule(Application application) {
        this.application = application;
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


}
