package com.mosoft.momomentum.module;

import com.mosoft.momomentum.ui.LoginActivity;
import com.mosoft.momomentum.ui.MainActivity;
import com.mosoft.momomentum.ui.MainActivityFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface MomentumApplicationComponent {

    void inject(MomentumApplication application);

    void inject(LoginActivity activity);

    void inject(MainActivityFragment mainActivityFragment);

    void inject(MainActivity mainActivity);
}
