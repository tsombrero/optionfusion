package com.mosoft.momomentum.ui.results;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mosoft.momomentum.util.Util.TAG;

public class ResultsFragment extends Fragment implements ResultsAdapter.FilterChangeListener {

    @Bind(R.id.list)
    protected RecyclerView recyclerView;

    @Bind(R.id.symbol)
    protected TextView symbolView;

    @Bind(R.id.price)
    protected TextView priceView;

    @Bind(R.id.stockInfo)
    protected ViewGroup stockInfo;

    @Bind(R.id.equityDescription)
    protected TextView equityDescription;

    @Inject
    OptionChainProvider optionChainProvider;

    FilterSet filterSet = new FilterSet();
    String symbol;
    ResultsAdapter resultsAdapter;

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
        MomentumApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_results, container, false);
        ButterKnife.bind(this, ret);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        symbol = getArguments().getString(ARG_SYMBOL);
        initView();
    }

    public void initView() {

        OptionChain oc = optionChainProvider.get(symbol);

        symbolView.setText(symbol);
        priceView.setText(Util.formatDollars(oc.getLast()));
        equityDescription.setText(oc.getEquityDescription());

        List<Spread> allSpreads = oc.getAllSpreads(filterSet);

        Log.i(TAG, "Closest matches:");

        if (allSpreads.isEmpty()) {
            Toast.makeText(getActivity(), "Spreads List Empty", Toast.LENGTH_SHORT);
            return;
        }

        Collections.sort(allSpreads, filterSet.getComparator());

        int spreadCount = Math.min(10, allSpreads.size());

        for (Spread spread : allSpreads.subList(0, spreadCount)) {
            Log.i(TAG, spread.toString() + "        " + spread.getBuy() + " / " + spread.getSell());
        }

        if (resultsAdapter == null) {
            resultsAdapter = new ResultsAdapter(filterSet, allSpreads.subList(0, spreadCount), getActivity(), this);
            recyclerView.setAdapter(resultsAdapter);
        } else {
            resultsAdapter.update(filterSet, allSpreads.subList(0, spreadCount));
        }
    }

    @Override
    public void onChange(FilterSet filterSet) {
        this.filterSet = filterSet;
        initView();
    }
}
