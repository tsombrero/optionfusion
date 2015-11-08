package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;

import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.client.AmeritradeClientProvider;
import com.mosoft.momomentum.client.ClientInterfaces;

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
    ClientInterfaces.OptionChainClient getBrokerageClient(Context context) {
        switch(MomentumApplication.from(context).getProvider()) {
            case AMERITRADE:
                return new AmeritradeClientProvider().getClient(MomentumApplication.from(context));
        }
        return null;
    }

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
}
