package com.mosoft.optionfusion.module;

import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.client.AmeritradeClient;
import com.mosoft.optionfusion.client.AmeritradeClientProvider;
import com.mosoft.optionfusion.client.GoogClient;
import com.mosoft.optionfusion.client.GoogClientProvider;
import com.mosoft.optionfusion.client.YhooClient;
import com.mosoft.optionfusion.client.YhooClientClientProvider;
import com.mosoft.optionfusion.ui.MainActivity;
import com.mosoft.optionfusion.ui.login.LoginActivity;
import com.mosoft.optionfusion.ui.login.LoginFragment;
import com.mosoft.optionfusion.ui.login.StartFragment;
import com.mosoft.optionfusion.ui.results.FilterViewHolder;
import com.mosoft.optionfusion.ui.results.ResultsFragment;
import com.mosoft.optionfusion.ui.search.SearchFragment;
import com.mosoft.optionfusion.ui.tradedetails.TradeDetailsFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = ApplicationModule.class)
public interface OptionFusionApplicationComponent {

    void inject(OptionFusionApplication application);

    void inject(LoginActivity activity);

    void inject(ResultsFragment resultsFragment);

    void inject(MainActivity mainActivity);

    void inject(SearchFragment searchFragment);

    void inject(OptionChainProvider optionChainProvider);

    void inject(FilterViewHolder filterViewHolder);

    void inject(TradeDetailsFragment tradeDetailsFragment);

    void inject(LoginFragment loginFragment);

    void inject(StartFragment startFragment);

    void inject(AmeritradeClient ameritradeClient);

    void inject(AmeritradeClientProvider ameritradeClientProvider);

    void inject(GoogClientProvider googClientProvider);

    void inject(GoogClient googClient);

    void inject(YhooClientClientProvider yhooClientProvider);

    void inject(YhooClient yhooClient);
}
