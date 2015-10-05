package com.mosoft.momomentum.ui;

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
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.SpreadFilter;
import com.mosoft.momomentum.model.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mosoft.momomentum.util.Util.TAG;

/**
 * A placeholder fragment containing a simple view.
 */
public class SpreadListFragment extends Fragment {

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

    SpreadFilter filter;
    String symbol;

    private static final String ARG_SYMBOL="symbol";

    static SpreadListFragment newInstance(String symbol) {
        SpreadListFragment ret = new SpreadListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SYMBOL, symbol);
        ret.setArguments(args);
        return ret;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MomentumApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_trades, container, false);
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

        SpreadFilter filter = new SpreadFilter();
        List<Spread> allSpreads = oc.getAllSpreads(filter);

        Log.i(TAG, "Closest matches:");

        if (allSpreads.isEmpty()) {
            Toast.makeText(getActivity(), "Spreads List Empty", Toast.LENGTH_SHORT);
            return;
        }

        Collections.sort(allSpreads, new Spread.DescendingBreakEvenDepthComparator());

        int spreadCount = Math.min(10, allSpreads.size());

        for (Spread spread : allSpreads.subList(0, spreadCount)) {
            Log.i(TAG, spread.toString() + "        " + spread.getBuy() + " / " + spread.getSell());
        }

        SpreadsAdapter adapter = new SpreadsAdapter(filter, allSpreads.subList(0, spreadCount), getResources());
        recyclerView.setAdapter(adapter);
    }


}
