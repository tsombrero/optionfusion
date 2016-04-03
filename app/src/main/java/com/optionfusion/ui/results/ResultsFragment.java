package com.optionfusion.ui.results;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ResultsFragment extends Fragment implements ResultsAdapter.ResultsListener {

    @Bind(R.id.list)
    protected RecyclerView recyclerView;

    @Bind(R.id.stock_quote)
    protected ViewGroup stockQuoteLayout;

    @Bind(R.id.toolbar)
    android.support.v7.widget.Toolbar toolbar;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    Gson gson;

    @Inject
    EventBus bus;

    FilterSet filterSet;
    String symbol;
    ResultsAdapter resultsAdapter;

    private static final String TAG = "ResultsFragment";
    private static final String ARG_SYMBOL = "symbol";
    private SharedViewHolders.StockQuoteViewHolder stockQuoteViewHolder;

    public static ResultsFragment newInstance(String symbol) {
        ResultsFragment ret = new ResultsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SYMBOL, symbol);
        ret.setArguments(args);
        return ret;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_results, container, false);
        ButterKnife.bind(this, ret);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Vertical Spreads");

        bus.register(this);

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        symbol = getArguments().getString(ARG_SYMBOL);
        bus.register(this);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        resultsAdapter = null;
        bus.unregister(this);
    }

    public void initView() {
        if (filterSet == null)
            filterSet = FilterSet.loadForSymbol(symbol, gson, sharedPreferences);

        Interfaces.StockQuote stockQuote = stockQuoteProvider.get(symbol);

        stockQuoteViewHolder = new SharedViewHolders.StockQuoteViewHolder(stockQuoteLayout, null, null, bus);
        stockQuoteViewHolder.bind(stockQuote);

        onChange(filterSet);
        Util.hideSoftKeyboard(getActivity());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StockQuotesUpdatedEvent event) {
        Interfaces.StockQuote stockQuote = event.getStockQuote(symbol);
        if (stockQuote != null && stockQuoteViewHolder != null)
            stockQuoteViewHolder.bind(stockQuote);
    }

    @Override
    public void onChange(final FilterSet filterSet) {
        this.filterSet = filterSet;

        filterSet.writeToPreferences(symbol, gson, sharedPreferences);

        new AsyncTask<Void, Void, List<VerticalSpread>>() {

            private Interfaces.OptionChain oc;

            @Override
            protected void onPostExecute(List<VerticalSpread> spreads) {
                if (resultsAdapter == null) {
                    resultsAdapter = new ResultsAdapter(filterSet, symbol, spreads, getActivity(), ResultsFragment.this);
                    recyclerView.setAdapter(resultsAdapter);
                } else {
                    resultsAdapter.update(filterSet, spreads);
                }

                if (spreads.isEmpty()) {
                    Toast.makeText(getActivity(), "No results", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            protected List<VerticalSpread> doInBackground(Void... params) {
                oc = optionChainProvider.get(symbol);
                List<VerticalSpread> allSpreads = oc.getAllSpreads(filterSet);
                return allSpreads;

//                Log.i(TAG, "Closest matches:");
//
//                if (allSpreads == null) return Collections.EMPTY_LIST;
//
//                if (allSpreads.isEmpty()) return allSpreads;
//
//                Collections.sort(allSpreads, filterSet.getComparator());
//
//                int spreadCount = Math.min(40, allSpreads.size());
//
//                for (Spread spread : allSpreads.subList(0, spreadCount)) {
//                    Log.i(TAG, spread.toString() + "        " + spread.getBuy() + " / " + spread.getSell());
//                }
//                return allSpreads.subList(0, spreadCount);
            }
        }.execute();
    }

    @Override
    public void onResultSelected(VerticalSpread spread, View headerLayout, View briefDetailsLayout) {
        ((Host) getActivity()).showDetails(spread, this, headerLayout, briefDetailsLayout, stockQuoteLayout);
    }

    public interface Host {
        void showDetails(VerticalSpread spread, Fragment requestingFragment, View detailsLayout, View headerLayout, View stockInfoLayout);
    }
}
