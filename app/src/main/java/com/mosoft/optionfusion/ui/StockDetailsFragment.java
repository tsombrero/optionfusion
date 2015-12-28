package com.mosoft.optionfusion.ui;

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

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.search.SearchFragment;

import butterknife.Bind;
import butterknife.ButterKnife;

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

        new SharedViewHolders.StockQuoteViewHolder(stockQuoteLayout).bind(stockQuote);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new StockDetailsAdapter());
        recyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }
}
