package com.optionfusion.ui.watchlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.R;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.jobqueue.GetFavoritesJob;
import com.optionfusion.jobqueue.GetWatchlistJob;
import com.optionfusion.jobqueue.SetWatchlistJob;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.backend.FusionStockQuote;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.ui.widgets.SymbolSearchTextView;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;

public class WatchlistFragment extends Fragment implements SharedViewHolders.SymbolSelectedListener, SwipeRefreshLayout.OnRefreshListener, AppBarLayout.OnOffsetChangedListener {

    private static final String KEY_WATCHLIST = "watchlist";
    private static final String KEY_PROVIDER = "provider";

    @Bind(R.id.list)
    RecyclerView recyclerView;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    ClientInterfaces.AccountClient accountClient;

    @Inject
    EventBus bus;

    @Inject
    JobManager jobManager;

    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @Bind(R.id.progress)
    ProgressBar progressBar;

    private StockQuoteAdapter adapter;

    private static final String TAG = "SearchFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        swipeRefreshLayout.setOnRefreshListener(this);

        setHasOptionsMenu(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new StockQuoteAdapter(this);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                if (!(viewHolder instanceof SharedViewHolders.StockQuoteViewHolder))
                    return;

                String symbol = ((SharedViewHolders.StockQuoteViewHolder) viewHolder).getSymbol();

                if (symbol == null)
                    return;

                adapter.removeItem(viewHolder.getAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        onRestoreInstanceState(savedInstanceState);

        return ret;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (swipeRefreshLayout == null)
            return;

        if (i == 0) {
            swipeRefreshLayout.setEnabled(true);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }
    }

    public static WatchlistFragment newInstance() {
        return new WatchlistFragment();
    }

    @Override
    public void onSymbolSelected(String symbol) {
        showProgress(true);
        ((Host) getActivity()).openResultsFragment(symbol);
    }

    @Override
    public void onRefresh() {
        if (adapter != null)
            adapter.onManualRefresh();
    }

    public interface Host {
        void openResultsFragment(String symbol);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter == null || adapter.getStockQuoteList().isEmpty()) {
            showProgress(true);
            jobManager.addJobInBackground(new GetWatchlistJob());
            jobManager.addJobInBackground(new GetFavoritesJob());
        } else {
            showProgress(false);
        }
    }

    SharedViewHolders.StockQuoteViewConfig viewConfig = new SharedViewHolders.StockQuoteViewConfig() {
        @Override
        public void onConfigChanged() {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };

    public void showProgress(final boolean show) {
        Host host = ((Host) getActivity());
        if (host != null && isAdded() && isVisible()) {
            progressBar.post(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    public void onFabClicked() {
        onOpenAddToWatchlistDialog();
    }

    public void onOpenAddToWatchlistDialog() {
        Util.showSoftKeyboard(getActivity());

        AddToWatchlistDialog dialog = new AddToWatchlistDialog();
        dialog.setSymbolSearchListener(new SymbolSearchTextView.SymbolSearchListener() {
            @Override
            public void onSymbolSearch(String symbol) {
                Set<String> symbols = new HashSet<>();

                List<Interfaces.StockQuote> watchlist = adapter.getStockQuoteList();

                if (watchlist == null)
                    watchlist = new ArrayList<>();

                for (Interfaces.StockQuote stockQuote : watchlist) {
                    symbols.add(stockQuote.getSymbol());
                }

                if (!symbols.contains(symbol)) {
                    symbols.add(symbol);
                    jobManager.addJobInBackground(new SetWatchlistJob(symbols));
                }
            }
        });
        dialog.setCancelable(true);
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.addToBackStack(null);
        dialog.show(ft, null);
    }


    public static class AddToWatchlistDialog extends DialogFragment implements SymbolSearchTextView.SymbolSearchListener {

        private SymbolSearchTextView.SymbolSearchListener listener;

        public void setSymbolSearchListener(SymbolSearchTextView.SymbolSearchListener listener) {
            this.listener = listener;
        }

        @Bind(R.id.search)
        SymbolSearchTextView symbolSearchTextView;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogTheme);
            super.onCreate(savedInstanceState);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.dialog_add_symbol, container, false);
            ButterKnife.bind(this, v);

            symbolSearchTextView.setSymbolSearchListener(this);
            symbolSearchTextView.requestFocus();
            return v;
        }

        @Override
        public void onSymbolSearch(String symbol) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(symbolSearchTextView.getWindowToken(), 0);
            dismiss();
            listener.onSymbolSearch(symbol.replace('.', '-'));
        }

        @OnEditorAction(R.id.search)
        public boolean onSubmit() {
            onSymbolSearch(symbolSearchTextView.getText().toString().toUpperCase());
            return true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null && adapter.getStockQuoteList() != null && adapter.getStockQuoteList().size() > 0) {
            outState.putInt(KEY_PROVIDER, adapter.getStockQuoteList().get(0).getProvider().ordinal());
            outState.putParcelableArrayList(KEY_WATCHLIST, adapter.getStockQuoteList());
        }
    }

    private void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        int provider = savedInstanceState.getInt(KEY_PROVIDER, OptionFusionApplication.Provider._UNKNOWN.ordinal());
        switch (OptionFusionApplication.Provider.values()[provider]) {
            case OPTION_FUSION_BACKEND:
                ArrayList<FusionStockQuote> watchlist = savedInstanceState.getParcelableArrayList(KEY_WATCHLIST);
                //// FIXME: 4/7/16 lame to copy, can't cast?
                ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();
                ret.addAll(watchlist);
                adapter.setStockQuoteList(ret);
                break;
        }
    }

}

