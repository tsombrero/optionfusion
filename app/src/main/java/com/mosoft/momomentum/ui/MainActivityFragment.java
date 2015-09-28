package com.mosoft.momomentum.ui;

import android.content.res.Resources;
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
import com.mosoft.momomentum.model.SpreadFilter;
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

    @Bind(R.id.price)
    protected TextView priceView;

    @Bind(R.id.stockInfo)
    protected ViewGroup stockInfo;

    @Bind(R.id.equityDescription)
    protected TextView equityDescription;

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

        final double percent = Double.valueOf(percentText) / 100d;

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

                editPercentView.setVisibility(View.GONE);
                editSymbolView.setVisibility(View.GONE);
                submitButton.setVisibility(View.GONE);
                stockInfo.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);

                symbolView.setText(oc.getSymbol());
                priceView.setText(Util.formatDollars(oc.getLast()));
                equityDescription.setText(oc.getEquityDescription());

                SpreadFilter filter = new SpreadFilter();
                filter.setMinMonthlyReturn(percent);
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

                SpreadsAdapter adapter = new SpreadsAdapter(allSpreads.subList(0, spreadCount));
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
            return new SpreadViewHolder(itemView, getResources());
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

        @Bind(R.id.annualizedMaxReturn)
        TextView annualizedReturn;

        @Bind(R.id.askPrice)
        TextView askPrice;

        @Bind(R.id.breakEvenPrice)
        TextView breakEvenPrice;

        @Bind(R.id.daysToExp)
        TextView daysToExp;

        @Bind(R.id.descriptionLeft)
        TextView description;

        @Bind(R.id.descriptionRight)
        TextView expirationDate;

        @Bind(R.id.maxReturn)
        TextView maxReturn;

        @Bind(R.id.percentChangeToBreakEven)
        TextView percentChangeToBreakEven;

        @Bind(R.id.percentChangeToMaxReturn)
        TextView percentChangeToMaxReturn;

        @Bind(R.id.maxReturnPrice)
        TextView maxReturnPrice;

        @Bind(R.id.title_maxReturnPrice)
        TextView title_maxReturnPrice;

        @Bind(R.id.title_breakEvenPrice)
        TextView title_breakEvenPrice;

        private Resources resources;

        public SpreadViewHolder(View itemView, Resources resources) {
            super(itemView);
            this.resources = resources;
            ButterKnife.bind(this, itemView);
        }

        public void bind(Spread spread) {
            annualizedReturn.setText(Util.formatPercent(spread.getMaxReturnAnnualized()));
            askPrice.setText(Util.formatDollars(spread.getAsk()));
            breakEvenPrice.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            maxReturnPrice.setText(Util.formatDollars(spread.getPrice_MaxReturn()));
            daysToExp.setText(String.valueOf(spread.getDaysToExpiration()) + " days");
            description.setText(String.format("%s %.2f/%.2f", spread.getBuy().getOptionType().toString(), spread.getBuy().getStrike(), spread.getSell().getStrike()));
            expirationDate.setText(Util.getFormattedOptionDate(spread.getExpiresDate()));
            maxReturn.setText(Util.formatDollars(spread.getMaxProfitAtExpiration()));
            percentChangeToBreakEven.setText(Util.formatPercent(spread.getPercentChange_BreakEven()) + (spread.isInTheMoney_BreakEven() ? "" : "  OTM"));
            percentChangeToMaxReturn.setText(Util.formatPercent(spread.getPercentChange_MaxProfit()) + (spread.isInTheMoney_MaxReturn() ? "" : "  OTM"));

            title_maxReturnPrice.setText(String.format(resources.getString(R.string.formatPriceAtMaxReturn), spread.isCall() ? "Above" : "Below"));
            title_breakEvenPrice.setText(String.format(resources.getString(R.string.formatPriceAtBreakEven), spread.isCall() ? "Below" : "Above"));

            int color = spread.isInTheMoney_BreakEven()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.red_900);

            percentChangeToBreakEven.setTextColor(color);
            breakEvenPrice.setTextColor(color);

            color = spread.isInTheMoney_MaxReturn()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.red_900);

            percentChangeToMaxReturn.setTextColor(color);
            maxReturnPrice.setTextColor(color);
        }
    }
}
