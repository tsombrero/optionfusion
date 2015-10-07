package com.mosoft.momomentum.module;

import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.ui.LoginActivity;
import com.mosoft.momomentum.ui.MainActivity;
import com.mosoft.momomentum.ui.results.ResultsFragment;
import com.mosoft.momomentum.ui.search.SearchFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface MomentumApplicationComponent {

    void inject(MomentumApplication application);

    void inject(LoginActivity activity);

    void inject(ResultsFragment resultsFragment);

    void inject(MainActivity mainActivity);

    void inject(SearchFragment searchFragment);

    void inject(OptionChainProvider optionChainProvider);
}
