package com.optionfusion.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.SharedElementCallback;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.results.ResultsFragment;
import com.optionfusion.ui.search.SearchFragment;
import com.optionfusion.ui.tradedetails.TradeDetailsFragment;
import com.optionfusion.util.Util;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements SearchFragment.Host, ResultsFragment.Host {

    @Bind(R.id.progress)
    ProgressBar progressBar;

    @BindColor(R.color.accent)
    int accentColor;

    @Inject
    ClientInterfaces.AccountClient fusionClient;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    FusionUser fusionUser;

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
        progressBar.getIndeterminateDrawable().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        showProgress(true);
        new AsyncTask<Void, Void, FusionUser>(){
            @Override
            protected FusionUser doInBackground(Void... params) {
                fusionUser = fusionClient.getAccountUser();
                return fusionUser; //TODO something
            }

            @Override
            protected void onPostExecute(FusionUser fusionUser) {
                showProgress(false);
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.feedback:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/117581253558561684569")));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void openResultsFragment(final String symbol) {
        Util.hideSoftKeyboard(this);

        progressBar.setVisibility(View.VISIBLE);

        optionChainProvider.get(symbol, new OptionChainProvider.OptionChainCallback() {
            @Override
            public void call(Interfaces.OptionChain optionChain) {
                if (progressBar.getVisibility() != View.VISIBLE) {
                    return;
                }

                progressBar.setVisibility(View.GONE);

                if (optionChain != null) {
                    Fragment fragment = ResultsFragment.newInstance(optionChain.getSymbol());
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment, optionChain.getSymbol())
                            .addToBackStack(null)
                            .commit();
                } else {
                    Toast.makeText(MainActivity.this, "Failed getting option chain", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stockQuoteProvider.get(symbol, new StockQuoteProvider.StockQuoteCallback() {
            @Override
            public void call(List<Interfaces.StockQuote> stockQuotes) {
                if (stockQuotes == null || stockQuotes.isEmpty())
                    return;

                getSupportActionBar().setTitle(stockQuotes.get(0).getDescription());
            }

            @Override
            public void onError(int status, String message) {

            }
        });
    }

    @Override
    public void showDetails(VerticalSpread spread, Fragment requestingFragment, View headerLayout, View detailsLayout, View stockInfoLayout) {

        requestingFragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        requestingFragment.setExitTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        Fragment fragment = TradeDetailsFragment.newInstance(spread);
        fragment.setSharedElementEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.change_transform));
        fragment.setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.fade));

        fragment.setEnterSharedElementCallback(new MySharedElementCallback());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, spread.toString())
                .addSharedElement(detailsLayout, SharedViewHolders.BriefTradeDetailsHolder.getTransitionName(spread))
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    public void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
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
