package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;

import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.client.AmeritradeClientProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final Application application;

    private AmeritradeClient ameritradeClient;

    ApplicationModule(Application application) {
        this.application = application;
        ameritradeClient = new AmeritradeClientProvider().getClient();
    }

    @Provides @Singleton
    Application application() {
        return application;
    }

    @Provides @Singleton
    AmeritradeClient getAmeritradeClient() {
        return ameritradeClient;
    }
}
