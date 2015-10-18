package com.mosoft.momomentum.ui.tradedetails;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.ui.SharedViewHolders;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TradeDetailsFragment extends Fragment {

    @Bind(R.id.stockInfo)
    protected ViewGroup stockInfo;

    @Bind(R.id.details_brief)
    protected ViewGroup briefDetailsLayout;

    @Bind(R.id.header)
    protected ViewGroup spreadHeaderLayout;

    @Bind(R.id.detail_sell)
    protected TextView textViewSell;

    @Bind(R.id.detail_buy)
    protected TextView textViewBuy;

    @Inject
    OptionChainProvider optionChainProvider;

    Spread spread;

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
        View ret = inflater.inflate(R.layout.fragment_trade_details, container, false);
        ButterKnife.bind(this, ret);

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        spread = getArguments().getParcelable(ARG_TRADE);
        initView();
    }

    public void initView() {
        final OptionChain oc = optionChainProvider.get(spread.getUnderlyingSymbol());

        new SharedViewHolders.StockInfoHolder(stockInfo).bind(oc);
        new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout).bind(spread);
        new SharedViewHolders.TradeDetailsHeaderHolder(spreadHeaderLayout).bind(spread);

        spreadHeaderLayout.setElevation(stockInfo.getElevation());
        spreadHeaderLayout.findViewById(R.id.item_menu).setVisibility(View.GONE);

        textViewBuy.setText("BUY one of: " + spread.getBuy());
        textViewSell.setText("SELL one of: " + spread.getSell());
    }
}
