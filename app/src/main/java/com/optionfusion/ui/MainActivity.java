package com.optionfusion.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.BuildConfig;
import android.support.v4.app.Fragment;
import android.support.v4.app.SharedElementCallback;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.birbit.android.jobqueue.JobManager;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.client.FusionClient;
import com.optionfusion.events.LoggedOutExceptionEvent;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.login.LoginActivity;
import com.optionfusion.ui.results.ResultsFragment;
import com.optionfusion.ui.search.WatchlistFragment;
import com.optionfusion.ui.tradedetails.TradeDetailsFragment;
import com.optionfusion.util.SharedPrefStore;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements WatchlistFragment.Host, ResultsFragment.Host {

    @Bind(R.id.progress)
    ProgressBar progressBar;

    @BindColor(R.color.accent)
    int accentColor;

    @Inject
    ClientInterfaces.AccountClient fusionClient;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    ClientInterfaces.AccountClient accountClient;

    @Inject
    EventBus bus;

    @Inject
    JobManager jobManager;

    @Inject
    SharedPrefStore sharedPrefStore;


    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.appbarLayout)
    AppBarLayout appBarLayout;



    private static GoogleApiClient apiClient;

    private static final int GOOGLE_API_CLIENTID = 2;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OptionFusionApplication.from(this).getComponent().inject(this);

        if (!sharedPrefStore.isUserAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bus.register(this);

        progressBar.getIndeterminateDrawable().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        showProgress(true);

        toolbar.setTitle("Option Fusion " + com.optionfusion.BuildConfig.VERSION_NAME);
        setSupportActionBar(toolbar);

        reconnect();
    }

    private void reconnect() {

        if (apiClient != null) {
            try {
                apiClient.stopAutoManage(this);
                if (apiClient.isConnected())
                    apiClient.disconnect();
                apiClient = null;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        apiClient = FusionClient.getGoogleApiClient(this, GOOGLE_API_CLIENTID);

        new AsyncTask<Void, Void, GoogleSignInResult>() {
            @Override
            protected GoogleSignInResult doInBackground(Void... params) {
                return accountClient.trySilentSignIn(apiClient);
            }

            @Override
            protected void onPostExecute(GoogleSignInResult googleSignInResult) {
                if (googleSignInResult != null && googleSignInResult.isSuccess()) {

                    accountClient.setGoogleAccount(googleSignInResult.getSignInAccount());

                    if (isDestroyed() || isFinishing())
                        return;

                    //TODO this can put multiple watchlists on the backstack

                    Fragment frag = WatchlistFragment.newInstance();
                    getSupportFragmentManager().beginTransaction()
                            .addToBackStack(null)
                            .add(R.id.fragment_container, frag, "tag_search")
                            .commitAllowingStateLoss();
                }
            }
        }.execute(null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/u/0/communities/118313361926845006072")));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private long lastConnectionFailedErr;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoggedOutException(LoggedOutExceptionEvent e) {
        if (apiClient != null)
            try {
                if (BuildConfig.DEBUG) {
                    File dumpfile = new File(getExternalCacheDir(), "connectionFailure_" + UUID.randomUUID());
                    FileOutputStream fos = new FileOutputStream(dumpfile);
                    apiClient.dump("FOO", null, new PrintWriter(fos), null);
                    fos.close();
                }
                Log.e(TAG, "Connection Failed");
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        if (lastConnectionFailedErr < System.currentTimeMillis() - 10000) {
            lastConnectionFailedErr = System.currentTimeMillis();
        }
        reconnect();
    }

    @Override
    @MainThread
    public void openResultsFragment(final String symbol) {
        if (progressBar.getVisibility() == View.VISIBLE)
            return;

        Util.hideSoftKeyboard(this);

        progressBar.setVisibility(View.VISIBLE);

        optionChainProvider.get(symbol, new ClientInterfaces.Callback<Interfaces.OptionChain>() {
            @Override
            public void call(Interfaces.OptionChain optionChain) {
                if (isDestroyed() || isFinishing())
                    return;

                if (progressBar.getVisibility() != View.VISIBLE)
                    return;

                showProgress(false);
                if (optionChain != null) {
                    Fragment fragment = ResultsFragment.newInstance(optionChain.getSymbol());
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment, optionChain.getSymbol())
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                } else {
                    onError(0, null);
                }
            }

            @Override
            public void onError(int status, String message) {
                Toast.makeText(MainActivity.this, R.string.failed_fetch_chain, Toast.LENGTH_SHORT).show();
            }
        });
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

        fragment.setEnterSharedElementCallback(new MySharedElementCallback());

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, spread.toString())
                .addSharedElement(detailsLayout, SharedViewHolders.BriefTradeDetailsHolder.getTransitionName(spread))
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        try {
            if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        } catch (Throwable t) {
            finish();
        }
    }

    @Override
    public void showProgress(final boolean show) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                fab.setEnabled(!show);
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
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
