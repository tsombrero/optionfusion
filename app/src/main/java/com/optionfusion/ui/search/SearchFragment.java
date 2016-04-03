package com.optionfusion.ui.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.optionfusion.BuildConfig;
import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.db.DbHelper;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.events.WatchListUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.Interfaces.StockQuote;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.ui.widgets.SymbolSearchTextView;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class SearchFragment extends Fragment implements SharedViewHolders.SymbolSelectedListener, SwipeRefreshLayout.OnRefreshListener, AppBarLayout.OnOffsetChangedListener {

    private static final String PREFKEY_WATCHLIST = "recents";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.list)
    RecyclerView recyclerView;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Bind(R.id.appbarLayout)
    AppBarLayout appBarLayout;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    ClientInterfaces.AccountClient accountClient;

    @Inject
    DbHelper dbHelper;

    @Inject
    EventBus bus;

    @Bind(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    private StockQuoteAdapter adapter;

    private static final String TAG = "SearchFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        toolbar.setTitle("Option Fusion " + BuildConfig.VERSION_NAME);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        swipeRefreshLayout.setOnRefreshListener(this);

        setHasOptionsMenu(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new StockQuoteAdapter();
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

                //TODO
//                Set<String> symbols = adapter.getSymbols();
//                symbols.remove(symbol);
//                sharedPreferences.edit().putStringSet(PREFKEY_WATCHLIST, symbols).apply();
                adapter.removeItem(viewHolder.getAdapterPosition());
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

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

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onSymbolSelected(String symbol) {
        ((Host) getActivity()).openResultsFragment(symbol);
    }

    @Override
    public void onRefresh() {
        if (adapter != null)
            adapter.update();
    }

    public interface Host {
        void openResultsFragment(String symbol);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (appBarLayout != null)
            appBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (appBarLayout != null)
            appBarLayout.removeOnOffsetChangedListener(this);
    }

    SharedViewHolders.StockQuoteViewConfig viewConfig = new SharedViewHolders.StockQuoteViewConfig() {
        @Override
        public void onConfigChanged() {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };

    private class StockQuoteAdapter extends RecyclerView.Adapter<SharedViewHolders.StockQuoteViewHolder> {

        private final Context context;
        private List<StockQuote> stockQuoteList = new ArrayList<>();
        private boolean isUpdating;

        public StockQuoteAdapter() {
            this(Collections.EMPTY_LIST);
        }

        public StockQuoteAdapter(List<String> symbols) {
            context = getActivity();
            stockQuoteList = stockQuoteProvider.get(symbols);
            bus.register(this);
            update();
        }

        public void removeItem(int index) {
            notifyItemRemoved(index);
            stockQuoteList.remove(index);
        }

        public void update() {
            if (isUpdating)
                return;

            if (accountClient.getAccountUser() == null)
                return;

            isUpdating = true;
            stockQuoteList = stockQuoteProvider.get(accountClient.getAccountUser().getWatchlistTickers());

            if (hasDummyStockQuotes(stockQuoteList)) {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isUpdating) {
                            Toast.makeText(context, "Failed refreshing stock quotes", Toast.LENGTH_SHORT);
                            swipeRefreshLayout.setRefreshing(false);
                            isUpdating = false;
                        }
                    }
                }, 10000);
            } else {
                isUpdating = false;
                swipeRefreshLayout.setRefreshing(false);
            }
        }

        private boolean hasDummyStockQuotes(List<StockQuote> stockQuoteList) {
            for (StockQuote stockQuote : stockQuoteList) {
                if (stockQuote.getProvider() == OptionFusionApplication.Provider.DUMMY)
                    return true;
            }
            return false;
        }


        @Subscribe
        public void onEvent(StockQuotesUpdatedEvent event) {
            if (event.getStockQuoteList() == null)
                return;

            synchronized (stockQuoteList) {
                for (StockQuote oldStockQuote : new ArrayList<>(stockQuoteList)) {
                    stockQuoteList.remove(oldStockQuote);
                    stockQuoteList.add(Util.bestOf(oldStockQuote, event.getStockQuote(oldStockQuote.getSymbol())));
                }
            }

            notifyDataSetChanged();
            swipeRefreshLayout.setRefreshing(false);
            isUpdating = false;
        }

        @Subscribe
        public void onEvent(WatchListUpdatedEvent event) {
            update();
        }

        @Override
        public SharedViewHolders.StockQuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_quote, parent, false);
            return new SharedViewHolders.StockQuoteViewHolder(v, viewConfig, SearchFragment.this, bus);
        }

        @Override
        public void onBindViewHolder(SharedViewHolders.StockQuoteViewHolder holder, int position) {
            holder.bind(stockQuoteList.get(position));
        }

        @Override
        public int getItemCount() {
            return stockQuoteList == null ? 0 : stockQuoteList.size();
        }
    }

    @OnClick(R.id.fab)
    public void onOpenAddToWatchlistDialog() {
        Util.showSoftKeyboard(getActivity());

        AddToWatchlistDialog dialog = new AddToWatchlistDialog();
        dialog.setSymbolSearchListener(new SymbolSearchTextView.SymbolSearchListener() {
            @Override
            public void onSymbolSearch(String symbol) {
                Set<String> symbols = new HashSet<>(); //TODO populate with current set
                if (!symbols.contains(symbol)) {
                    symbols.add(symbol);
                    sharedPreferences.edit().putStringSet(PREFKEY_WATCHLIST, symbols).apply();
                }
                adapter.update();
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
}

