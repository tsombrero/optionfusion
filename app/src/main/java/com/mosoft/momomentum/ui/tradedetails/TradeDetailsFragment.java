package com.mosoft.momomentum.ui.tradedetails;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TradeDetailsFragment extends Fragment {

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

    private static final String ARG_TRADE = "trade";

    public static TradeDetailsFragment newInstance(Spread spread) {
        TradeDetailsFragment ret = new TradeDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRADE, spread);
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
        symbol = getArguments().getString(ARG_TRADE);
        initView();
    }

    public void initView() {

        final OptionChain oc = optionChainProvider.get(symbol);

        symbolView.setText(symbol);
        priceView.setText(Util.formatDollars(oc.getLast()));
        equityDescription.setText(oc.getEquityDescription());
    }
}
