package com.mosoft.momomentum.module;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;

public class MomentumApplication extends Application {

    private MomentumApplicationComponent applicationComponent;

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
}
