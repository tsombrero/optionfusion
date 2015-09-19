package com.mosoft.momomentum.module;

import com.mosoft.momomentum.LoginActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface MomentumApplicationComponent {

    void inject(MomentumApplication application);

    void inject(LoginActivity activity);
}
