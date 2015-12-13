package com.mosoft.optionfusion.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.SharedElementCallback;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.results.ResultsFragment;
import com.mosoft.optionfusion.ui.search.SearchFragment;
import com.mosoft.optionfusion.ui.tradedetails.TradeDetailsFragment;
import com.mosoft.optionfusion.util.Util;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements SearchFragment.Host, ResultsFragment.Host {

    @Bind(R.id.progress)
    ProgressBar progressBar;

    @Inject
    OptionChainProvider optionChainProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OptionFusionApplication.from(this).getComponent().inject(this);
        ButterKnife.bind(this);

        Fragment frag = SearchFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .add(R.id.fragment_container, frag, "tag_search")
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void openResultsFragment(final String symbol) {
        Util.hideSoftKeyboard(this);
        progressBar.setVisibility(View.VISIBLE);

        optionChainProvider.get(symbol, new OptionChainProvider.OptionChainCallback() {
            @Override
            public void call(Interfaces.OptionChain optionChain) {
                progressBar.setVisibility(View.GONE);

                if (optionChain != null) {
                    Fragment fragment = ResultsFragment.newInstance(optionChain.getUnderlyingStockQuote().getSymbol());
                    getSupportActionBar().setTitle(optionChain.getUnderlyingStockQuote().getDescription());
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment, optionChain.getUnderlyingStockQuote().getSymbol())
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.failed_getting_chain), Toast.LENGTH_SHORT);
                }
            }
        });
    }

    @Override
    public void showDetails(Spread spread, Fragment requestingFragment, View headerLayout, View detailsLayout, View stockInfoLayout) {

        requestingFragment.setSharedElementReturnTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        requestingFragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
        requestingFragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        Fragment fragment = TradeDetailsFragment.newInstance(spread);
        fragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));
        fragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        fragment.setEnterSharedElementCallback(new MySharedElementCallback());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, spread.toString())
                .addSharedElement(detailsLayout, SharedViewHolders.BriefTradeDetailsHolder.getTransitionName(spread))
                .addSharedElement(headerLayout, SharedViewHolders.TradeDetailsHeaderHolder.getTransitionName(spread))
                .addSharedElement(stockInfoLayout, SharedViewHolders.StockInfoHolder.getTransitionName(spread.getUnderlyingSymbol()))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    public class MySharedElementCallback extends SharedElementCallback {
        @Override
        public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementStart(sharedElementNames, sharedElements, sharedElementSnapshots);
        }

        @Override
        public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
            super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots);
        }

        @Override
        public void onRejectSharedElements(List<View> rejectedSharedElements) {
            super.onRejectSharedElements(rejectedSharedElements);
        }

        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            super.onMapSharedElements(names, sharedElements);
        }

        @Override
        public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix, RectF screenBounds) {
            return super.onCaptureSharedElementSnapshot(sharedElement, viewToGlobalMatrix, screenBounds);
        }

        @Override
        public View onCreateSnapshotView(Context context, Parcelable snapshot) {
            return super.onCreateSnapshotView(context, snapshot);
        }
    }
}
