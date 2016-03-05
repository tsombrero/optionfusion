package com.optionfusion.module;

import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.client.AmeritradeClient;
import com.optionfusion.client.AmeritradeClientProvider;
import com.optionfusion.client.FusionClient;
import com.optionfusion.client.FusionClientProvider;
import com.optionfusion.client.GoogClient;
import com.optionfusion.client.GoogClientProvider;
import com.optionfusion.client.YhooClient;
import com.optionfusion.client.YhooClientClientProvider;
import com.optionfusion.ui.MainActivity;
import com.optionfusion.ui.StockDetailsFragment;
import com.optionfusion.ui.login.GoogleLoginFragment;
import com.optionfusion.ui.login.LoginActivity;
import com.optionfusion.ui.login.AmeritradeLoginFragment;
import com.optionfusion.ui.login.StartFragment;
import com.optionfusion.ui.results.FilterViewHolder;
import com.optionfusion.ui.results.ResultsFragment;
import com.optionfusion.ui.search.SearchFragment;
import com.optionfusion.ui.tradedetails.TradeDetailsFragment;
import com.optionfusion.ui.widgets.SymbolSearchTextView;

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

    void inject(AmeritradeLoginFragment ameritradeLoginFragment);

    void inject(StartFragment startFragment);

    void inject(AmeritradeClient ameritradeClient);

    void inject(AmeritradeClientProvider ameritradeClientProvider);

    void inject(GoogClientProvider googClientProvider);

    void inject(GoogClient googClient);

    void inject(YhooClientClientProvider yhooClientProvider);

    void inject(YhooClient yhooClient);

    void inject(StockDetailsFragment stockDetailsFragment);

    void inject(SymbolSearchTextView symbolSearchTextView);

    void inject(GoogleLoginFragment googleLoginFragment);

    void inject(FusionClientProvider fusionClientProvider);

    void inject(FusionClient fusionClient);
}
