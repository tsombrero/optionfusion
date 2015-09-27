package com.mosoft.momomentum.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;

import static com.mosoft.momomentum.util.Util.TAG;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    @Bind(R.id.edit_symbol)
    protected EditText editSymbolView;

    @Bind(R.id.profitPercent)
    protected EditText editPercentView;

    @Bind(R.id.submit)
    protected Button submitButton;

    @Bind(R.id.list)
    protected RecyclerView recyclerView;

    @Bind(R.id.symbol)
    protected TextView symbolView;

    @Bind(R.id.change)
    protected TextView changeView;

    @Bind(R.id.price)
    protected TextView priceView;

    @Bind(R.id.stockInfo)
    protected ViewGroup stockInfo;

    @Inject
    AmeritradeClient ameritradeClient;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MomentumApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, ret);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        return ret;
    }

    @OnClick(R.id.submit)
    public void onClick(View view) {
        final String symbol = editSymbolView.getText().toString();

        if (TextUtils.isEmpty(symbol))
            Toast.makeText(getActivity(), "Enter a ticker symbolView", Toast.LENGTH_SHORT);

        String percentText = editPercentView.getText().toString();

        if (TextUtils.isEmpty(percentText))
            Toast.makeText(getActivity(), "Enter a monthly percent growth", Toast.LENGTH_SHORT);

        final Double percent = Double.valueOf(percentText) / 100d;

        ameritradeClient.getOptionChain(symbol).enqueue(new Callback<OptionChain>() {
            @Override
            public void onResponse(Response<OptionChain> response) {
                if (!response.isSuccess()) {
                    Log.w("tag", "Failed: " + response.message());
                    return;
                }

                OptionChain oc = response.body();

                if (!oc.succeeded()) {
                    Log.w("tag", "Failed: " + oc.getError());
                    return;
                }

                Log.i("tag", "Got option chain: " + oc);

                SparseArray<Double> compoundGrowthByDTE = new SparseArray<>();

                for (OptionChain.OptionDate optiondate : oc.getOptionDates()) {
                    double months = ((double) optiondate.getDaysToExpiration()) / 365d * 12d;
                    double totalPercentGoal = Util.compoundGrowth(percent, months);
                    compoundGrowthByDTE.put(optiondate.getDaysToExpiration(), totalPercentGoal);
                }

                editPercentView.setVisibility(View.GONE);
                editSymbolView.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                stockInfo.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);

                symbolView.setText(oc.getSymbol());
                changeView.setText(Util.formatDollars(oc.getChange()));
                priceView.setText(Util.formatDollars(oc.getLast()));

                List<Spread> allSpreads = oc.getAllSpreads();

                Log.i(TAG, "Closest matches:");

                int j = allSpreads.size() / 2;
                int bisect = j / 2;
                while (bisect > 0) {
                    Spread spread = allSpreads.get(j);
                    if (spread.getMaxPercentProfitAtExpiration() > compoundGrowthByDTE.get(spread.getDaysToExpiration()))
                        j -= bisect;
                    else
                        j += bisect;

                    bisect /= 2;
                }

                if (allSpreads.isEmpty()) {
                    Toast.makeText(getActivity(), "Spreads List Empty", Toast.LENGTH_SHORT);
                    return;
                }

                allSpreads = allSpreads.subList(j, allSpreads.size() -1);

                Collections.sort(allSpreads, new Spread.DescendingBreakEvenDepthComparator());

                for (Spread spread : allSpreads.subList(0, 10)) {
                    Log.i(TAG, spread.toString() + "        " + spread.getBuy() + " / " + spread.getSell());
                }

                SpreadsAdapter adapter = new SpreadsAdapter(allSpreads.subList(0, 10));
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private class SpreadsAdapter extends RecyclerView.Adapter<SpreadViewHolder> {

        List<Spread> spreads = new ArrayList<>();

        public SpreadsAdapter(List<Spread> spreads) {
            this.spreads = spreads;
        }

        @Override
        public SpreadViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spread_details, parent, false);
            return new SpreadViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SpreadViewHolder holder, int position) {
            holder.bind(spreads.get(position));
        }

        @Override
        public int getItemCount() {
            return spreads.size();
        }
    }

    public static class SpreadViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.annualizedMaxProfit)
        TextView annualizedProfit;

        @Bind(R.id.askPrice)
        TextView askPrice;

        @Bind(R.id.breakEven)
        TextView breakEven;

        @Bind(R.id.daysToExp)
        TextView daysToExp;

        @Bind(R.id.description)
        TextView description;

        @Bind(R.id.maxProfit)
        TextView maxProfit;

        @Bind(R.id.percentChangeToleranceToBreakEven)
        TextView percentChangeToleranceToBreakEven;

        public SpreadViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Spread spread) {
            annualizedProfit.setText(Util.formatPercent(spread.getMaxProfitAnnualized()));
            askPrice.setText(Util.formatDollars(spread.getAsk()));
            breakEven.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            daysToExp.setText(String.valueOf(spread.getDaysToExpiration()));
            description.setText(spread.getDescription());
            maxProfit.setText(Util.formatDollars(spread.getMaxProfitAtExpiration()));
            percentChangeToleranceToBreakEven.setText(Util.formatPercent(spread.getMaxPercentChange_BreakEven()));
        }
    }
}
