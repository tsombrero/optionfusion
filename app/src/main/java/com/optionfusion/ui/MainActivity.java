package com.optionfusion.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.BuildConfig;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.birbit.android.jobqueue.JobManager;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.client.FusionClient;
import com.optionfusion.db.DbHelper;
import com.optionfusion.events.LoggedOutExceptionEvent;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.favorites.FavoritesFragment;
import com.optionfusion.ui.help.HelpFragment;
import com.optionfusion.ui.login.LoginActivity;
import com.optionfusion.ui.results.ResultsActivity;
import com.optionfusion.ui.results.ResultsAdapter;
import com.optionfusion.ui.search.WatchlistFragment;
import com.optionfusion.ui.tradedetails.TradeDetailsActivity;
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
import java.util.concurrent.RunnableFuture;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements WatchlistFragment.Host, ResultsAdapter.SpreadSelectedListener {

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

    @Inject
    DbHelper dbHelper;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.appbarLayout)
    AppBarLayout appBarLayout;

    @Bind(R.id.tablayout)
    TabLayout tabLayout;

    @Bind(R.id.pager)
    ViewPager pager;


    private static final String TAG = "MainActivity";
    private TabPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OptionFusionApplication.from(this).getComponent().inject(this);

        if (!sharedPrefStore.isUserAuthenticated()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        bus.register(this);

        toolbar.setTitle("Option Fusion " + com.optionfusion.BuildConfig.VERSION_NAME);
        setSupportActionBar(toolbar);

        pagerAdapter = new MainActivity.TabPagerAdapter(getSupportFragmentManager(), this);
        pager.setAdapter(pagerAdapter);

        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                tabLayout.getTabAt(position).select();
                toolbar.setTitle(pagerAdapter.getPageTitle(position));
                switch (position) {
                    case 0:
                        fab.show();
                        break;
                    case 1:
                    case 2:
                    default:
                        fab.hide();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public static class TabPagerAdapter extends FragmentPagerAdapter {

        private final Activity activity;
        WatchlistFragment watchlistFragment;
        FavoritesFragment favoritesFragment;
        HelpFragment helpFragment;

        public TabPagerAdapter(FragmentManager fm, Activity activity) {
            super(fm);
            this.activity = activity;
            watchlistFragment = WatchlistFragment.newInstance();
            favoritesFragment = FavoritesFragment.newInstance();
            helpFragment = HelpFragment.newInstance();
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 1:
                    return favoritesFragment;
                case 2:
                    return helpFragment;
            }
            return watchlistFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return activity.getString(R.string.favorites);
            }
            return activity.getString(R.string.watchlist);
        }
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoggedOutException(LoggedOutExceptionEvent e) {
        sharedPrefStore.setEmail(null);
        sharedPrefStore.setSessionid(null);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    @MainThread
    public void openResultsFragment(final String symbol) {

        Util.hideSoftKeyboard(this);

        optionChainProvider.get(symbol, new ClientInterfaces.Callback<Interfaces.OptionChain>() {
            @Override
            public void call(Interfaces.OptionChain optionChain) {
                if (pagerAdapter.watchlistFragment != null)
                    pagerAdapter.watchlistFragment.showProgress(false);

                if (isDestroyed() || isFinishing())
                    return;

                if (optionChain != null) {
                    Intent intent = new Intent(MainActivity.this, ResultsActivity.class);
                    intent.putExtra(ResultsActivity.EXTRA_SYMBOL, symbol);
                    startActivity(intent);
                } else {
                    onError(0, null);
                }
            }

            @Override
            public void onError(int status, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.failed_fetch_chain, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onResultSelected(VerticalSpread spread, View headerLayout, View detailsLayout) {
        Intent intent = new Intent(this, TradeDetailsActivity.class);
        intent.putExtra(TradeDetailsActivity.EXTRA_SPREAD, spread);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {

        DbSpread.clearDeletedFavorites(dbHelper.getWritableDatabase());

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

    @OnClick(R.id.fab)
    public void onFabClicked(View v) {
        pagerAdapter.watchlistFragment.onFabClicked();
    }
}
