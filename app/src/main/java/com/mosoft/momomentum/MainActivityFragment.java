package com.mosoft.momomentum;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mosoft.momomentum.client.AmeritradeClient;
import com.mosoft.momomentum.model.BullCallSpread;
import com.mosoft.momomentum.model.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

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

    @Bind(R.id.symbol)
    protected TextView symbolView;

    @Bind(R.id.profitPercent)
    protected TextView percentView;

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
        return ret;
    }

    @OnClick(R.id.submit)
    public void onClick(View view) {
        String symbol = symbolView.getText().toString();

        if (TextUtils.isEmpty(symbol))
            Toast.makeText(getActivity(), "Enter a ticker symbol", Toast.LENGTH_SHORT);

        String percentText = percentView.getText().toString();

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
                }

                Log.i("tag", "Got option chain: " + oc);

                SparseArray<Double> compoundGrowthByDTE = new SparseArray<>();

                for (OptionChain.OptionDate optiondate : oc.getOptionDates()) {
                    double months = ((double) optiondate.getDaysToExpiration()) / 365d * 12d;
                    double totalPercentGoal = compoundGrowth(percent, months);
                    compoundGrowthByDTE.put(optiondate.getDaysToExpiration(), totalPercentGoal);
                }

                List<BullCallSpread> allSpreads = oc.getAllBullCallSpreads();

                Log.i(TAG, "Closest matches:");

                int j = allSpreads.size() / 2;
                int bisect = j / 2;
                while (bisect > 0) {
                    BullCallSpread spread = allSpreads.get(j);
                    if (spread.getMaxPercentProfitAtExpiration() > compoundGrowthByDTE.get(spread.getDaysToExpiration()))
                        j -= bisect;
                    else
                        j += bisect;

                    bisect /= 2;
                }

                allSpreads = allSpreads.subList(j, allSpreads.size() -1);

                Collections.sort(allSpreads, new BullCallSpread.AscendingHighLegStrikeComparator());

                for (BullCallSpread spread : allSpreads.subList(0, 10)) {
                    Log.i(TAG, spread.toString());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });
    }

    //TODO there's a formula for this!
    double compoundGrowth(final double periodicGrowth, final double periodCount) {
        double ret = 1d;
        double periodRemaining = periodCount;

        while (periodRemaining > 1d) {
            ret *= (1d + periodicGrowth);
            periodRemaining -= 1d;
        }

        ret *= (1d + (periodicGrowth * periodRemaining));

        Log.d(TAG, String.format("%2.1f%% compounded monthly for %.2f months is %2.1f%%", periodicGrowth * 100d, periodCount, (ret * 100d) - 100d));

        return ret - 1d;
    }
}
