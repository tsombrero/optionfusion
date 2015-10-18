package com.mosoft.momomentum.module;

import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.ui.LoginActivity;
import com.mosoft.momomentum.ui.MainActivity;
import com.mosoft.momomentum.ui.results.FilterViewHolder;
import com.mosoft.momomentum.ui.search.SearchFragment;
import com.mosoft.momomentum.ui.tradedetails.TradeDetailsFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface MomentumApplicationComponent {

    void inject(MomentumApplication application);

    void inject(LoginActivity activity);

    void inject(com.mosoft.momomentum.ui.results.ResultsFragment resultsFragment);

    void inject(MainActivity mainActivity);

    void inject(SearchFragment searchFragment);

    void inject(OptionChainProvider optionChainProvider);

    void inject(FilterViewHolder filterViewHolder);

    void inject(TradeDetailsFragment tradeDetailsFragment);
}
