package com.mosoft.optionfusion.ui.search;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.model.provider.Interfaces.StockQuote;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.SharedViewHolders;
import com.mosoft.optionfusion.ui.widgets.SymbolSearchTextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchFragment extends Fragment implements SharedViewHolders.StockQuoteViewHolderListener {

    private static final String PREFKEY_WATCHLIST = "recents";

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.list)
    RecyclerView recyclerView;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    ClientInterfaces.StockQuoteClient stockQuoteClient;
    private StockQuoteAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setHasOptionsMenu(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new StockQuoteAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return ret;
    }

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onTogglePriceChangeFormat() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSymbolSelected(String symbol) {
        ((Host) getActivity()).openResultsFragment(symbol);
    }

    public interface Host {
        void openResultsFragment(String symbol);
    }

    public Set<String> getRecentSymbols() {
        Set ret = sharedPreferences.getStringSet(PREFKEY_WATCHLIST, Collections.EMPTY_SET);
        if (ret.isEmpty()) {
            ret = new HashSet<>(Arrays.asList("AAPL", "CSCO", "GOOG", "NFLX", "TSLA", "FB", "AMZN", "BRK-A"));
            sharedPreferences.edit().putStringSet(PREFKEY_WATCHLIST, ret).apply();
        }
        return ret;
    }

    private class StockQuoteAdapter extends RecyclerView.Adapter<SharedViewHolders.StockQuoteViewHolder> {

        private final Context context;
        private List<StockQuote> stockQuoteList;

        public StockQuoteAdapter() {
            context = getActivity();
            update();
        }

        public void update() {
            stockQuoteClient.getStockQuotes(getRecentSymbols(), new ClientInterfaces.Callback<List<StockQuote>>() {
                @Override
                public void call(List<StockQuote> stockQuotes) {
                    stockQuoteList = stockQuotes;
                    Collections.sort(stockQuoteList, new Comparator<StockQuote>() {
                        @Override
                        public int compare(StockQuote lhs, StockQuote rhs) {
                            return lhs.getSymbol().compareTo(rhs.getSymbol());
                        }
                    });
                    notifyDataSetChanged();
                }

                @Override
                public void onError(int status, String message) {
                    Toast.makeText(context, "Failed getting quotes (" + message + ")", Toast.LENGTH_SHORT);
                }
            });
        }

        @Override
        public SharedViewHolders.StockQuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_quote, parent, false);
            return new SharedViewHolders.StockQuoteViewHolder(v, SearchFragment.this);
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
    public void onAddToWatchlist() {
        AddToWatchlistFragment dialog = new AddToWatchlistFragment();
        dialog.setSymbolSearchListener(new SymbolSearchTextView.SymbolSearchListener() {
            @Override
            public void onSymbolSearch(String symbol) {
                Set<String> symbols = getRecentSymbols();
                if (!symbols.contains(symbol)) {
                    symbols.add(symbol);
                    sharedPreferences.edit().putStringSet(PREFKEY_WATCHLIST, symbols).apply();
                }
                adapter.update();
            }
        });
        dialog.show(getChildFragmentManager(), null);
    }

    public static class AddToWatchlistFragment extends DialogFragment implements SymbolSearchTextView.SymbolSearchListener {

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

            return v;
        }

        @Override
        public void onSymbolSearch(String symbol) {
            dismiss();
            listener.onSymbolSearch(symbol);
        }
    }
}

