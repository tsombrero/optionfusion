package com.optionfusion.module;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import com.google.gson.reflect.TypeToken;
import com.optionfusion.model.provider.amtd.AmeritradeStockQuote;
import com.optionfusion.model.provider.yhoo.YhooStockQuote;

import java.lang.reflect.Type;
import java.util.List;

public class OptionFusionApplication extends Application {

    private OptionFusionApplicationComponent applicationComponent;

    public enum Provider {
        _UNKNOWN,
        AMERITRADE,
        YAHOO,
        GOOGLE_FINANCE;

        public Type getStockQuoteListType() {
            switch (this) {

                case AMERITRADE:
                    return new TypeToken<List<AmeritradeStockQuote>>() {
                    }.getType();
                case YAHOO:
                    return new TypeToken<List<YhooStockQuote>>() {
                    }.getType();
                case GOOGLE_FINANCE:
                    break;
            }
            return null;
        }
    }

    private Provider provider;

    public Provider getBackendProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeInjector();
    }

    private void initializeInjector() {
        applicationComponent = DaggerOptionFusionApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        applicationComponent.inject(this);
    }

    public static OptionFusionApplication from(@NonNull Context context) {
        return (OptionFusionApplication) context.getApplicationContext();
    }

    public OptionFusionApplicationComponent getComponent() {
        return applicationComponent;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
