package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MomentumApplication extends Application {

    private static Gson gson;
    private MomentumApplicationComponent applicationComponent;

    public enum Provider {
        AMERITRADE,
        GOOGLE_FINANCE
    }

    private Provider provider;

    public Provider getProvider() {
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
        applicationComponent = DaggerMomentumApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        applicationComponent.inject(this);
    }

    public static MomentumApplication from(@NonNull Context context) {
        return (MomentumApplication) context.getApplicationContext();
    }

    public MomentumApplicationComponent getComponent() {
        return applicationComponent;
    }

    // We often need Gson where context is not available, and it's the same for everyone, so use this old-school singleton
    // TODO figure out how to inject Gson without plumbing a context into everywhere that needs it
    public static Gson getGson() {
        if (gson == null)
            gson = new GsonBuilder().create();

        return gson;
    }
}
