package com.optionfusion.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.optionfusion.R;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.tradedetails.LineChartViewHolder;

import butterknife.Bind;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.view.LineChartView;

public class StockDetailsFragment extends Fragment {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.stock_quote)
    View stockQuoteLayout;

    @Bind(R.id.list)
    RecyclerView recyclerView;

    private Interfaces.StockQuote stockQuote;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_stock_details, container, false);
        ButterKnife.bind(this, ret);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        setHasOptionsMenu(true);

        new SharedViewHolders.StockQuoteViewHolder(stockQuoteLayout, null).bind(stockQuote);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new StockDetailsAdapter());
        return ret;
    }

    public static Fragment newInstance(Interfaces.StockQuote stockQuote) {
        Fragment ret = new StockDetailsFragment();
        Bundle args = new Bundle();
        args.putString("symbol", stockQuote.getSymbol());
        ret.setArguments(args);
        return ret;
    }

    private class StockDetailsAdapter extends RecyclerView.Adapter {

        private static final int HISTORY_CHART_VIEW_TYPE = 0;

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case HISTORY_CHART_VIEW_TYPE:
                    return new LineChartViewHolder(new LineChartView(parent.getContext()));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (position) {
                case 0:
                    ((LineChartViewHolder) holder).bind(stockQuote.getSymbol());
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0)
                return HISTORY_CHART_VIEW_TYPE;
            return -1;
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }
}
