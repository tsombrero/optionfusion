package com.mosoft.optionfusion.ui.results;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.model.FilterSet;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.SharedViewHolders;
import com.mosoft.optionfusion.util.Util;

import java.util.Collections;
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
    SharedPreferences sharedPreferences;

    @Inject
    Gson gson;

    FilterSet filterSet;
    String symbol;
    ResultsAdapter resultsAdapter;

    private static final String TAG = "ResultsFragment";
    private static final String ARG_SYMBOL = "symbol";

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

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        symbol = getArguments().getString(ARG_SYMBOL);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        resultsAdapter = null;
    }

    public void initView() {
        if (filterSet == null)
            filterSet = FilterSet.loadForSymbol(symbol, gson, sharedPreferences);

        optionChainProvider.get(symbol, new OptionChainProvider.OptionChainCallback() {
            @Override
            public void call(Interfaces.OptionChain optionChain) {
                new SharedViewHolders.StockQuoteViewHolder(stockQuoteLayout, null).bind(optionChain.getUnderlyingStockQuote());
                onChange(filterSet);
            }
        });

        Util.hideSoftKeyboard(getActivity());
    }

    @Override
    public void onChange(final FilterSet filterSet) {
        this.filterSet = filterSet;

        filterSet.writeToPreferences(symbol, gson, sharedPreferences);

        new AsyncTask<Void, Void, List<Spread>>() {

            private Interfaces.OptionChain oc;

            @Override
            protected void onPostExecute(List<Spread> spreads) {
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
            protected List<Spread> doInBackground(Void... params) {
                oc = optionChainProvider.get(symbol);
                List<Spread> allSpreads = oc.getAllSpreads(filterSet);

                Log.i(TAG, "Closest matches:");

                if (allSpreads == null) return Collections.EMPTY_LIST;
                if (allSpreads.isEmpty()) return allSpreads;

                Collections.sort(allSpreads, filterSet.getComparator());

                int spreadCount = Math.min(40, allSpreads.size());

                for (Spread spread : allSpreads.subList(0, spreadCount)) {
                    Log.i(TAG, spread.toString() + "        " + spread.getBuy().getDescription() + " / " + spread.getSell().getDescription());
                }
                return allSpreads.subList(0, spreadCount);
            }
        }.execute();
    }

    @Override
    public void onResultSelected(Spread spread, View headerLayout, View briefDetailsLayout) {
        ((Host) getActivity()).showDetails(spread, this, headerLayout, briefDetailsLayout, stockQuoteLayout);
    }

    public interface Host {
        void showDetails(Spread spread, Fragment requestingFragment, View detailsLayout, View headerLayout, View stockInfoLayout);
    }
}
