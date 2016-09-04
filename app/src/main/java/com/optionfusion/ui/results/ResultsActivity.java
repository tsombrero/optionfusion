package com.optionfusion.ui.results;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;

import com.optionfusion.R;
import com.optionfusion.db.DbHelper;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.ui.tradedetails.TradeDetailsFragment;

import javax.inject.Inject;

public class ResultsActivity extends AppCompatActivity implements ResultsFragment.Host {

    public static final String EXTRA_SYMBOL = "EXTRA_SYMBOL";

    @Inject
    DbHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        OptionFusionApplication.from(this).getComponent().inject(this);

        String symbol = getSymbol();

        if (symbol == null)
            finish();

        Fragment fragment = ResultsFragment.newInstance(symbol);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, symbol)
                .commitAllowingStateLoss();
    }

    public String getSymbol() {
        if (getIntent().hasExtra(EXTRA_SYMBOL))
            return getIntent().getExtras().getString(EXTRA_SYMBOL);

        return null;
    }

    @Override
    public void onBackPressed() {
        DbSpread.clearDeletedFavorites(dbHelper.getWritableDatabase());

        if (getSupportFragmentManager().getBackStackEntryCount() == 0)
            finish();
        else
            super.onBackPressed();
    }

    @Override
    public void showDetails(VerticalSpread spread, Fragment requestingFragment, View headerLayout, View detailsLayout, View stockInfoLayout) {

        Fragment fragment = TradeDetailsFragment.newInstance(spread);
        if (Build.VERSION.SDK_INT >= 21) {
            requestingFragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
            requestingFragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
            fragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
            fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
        }

//        fragment.setEnterSharedElementCallback(new MySharedElementCallback());

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, spread.toString())
                .addSharedElement(detailsLayout, SharedViewHolders.BriefTradeDetailsHolder.getTransitionName(spread))
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

}
