package com.mosoft.optionfusion.module;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public class OptionFusionApplication extends Application {

    private OptionFusionApplicationComponent applicationComponent;

    public enum Provider {
        AMERITRADE,
        YAHOO,
        GOOGLE_FINANCE
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
}
