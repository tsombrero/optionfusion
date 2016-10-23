package com.optionfusion.jobqueue;

import android.util.Log;
import android.widget.Toast;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.common.OptionKey;
import com.optionfusion.model.provider.Interfaces;

public class GetOptionChainJob extends BaseApiJob {

    private static final String TAG = "GetOptionChainJob";
    private final String symbol;
    private final boolean favoritesOnly;
    private Interfaces.OptionChain result;

    public GetOptionChainJob(String symbol, boolean favoritesOnly) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST)
                .singleInstanceBy(GetOptionChainJob.class.getSimpleName() + symbol));

        this.symbol = symbol;
        this.favoritesOnly = favoritesOnly;

        Log.i(TAG, "New StockQuoteJob : " + GetOptionChainJob.class.getSimpleName() + symbol);
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        result = optionChainClient.getOptionChain(symbol, favoritesOnly);
        optionChainProvider.put(symbol, result);
    }

    public Interfaces.OptionChain getResult() {
        return result;
    }

    public String getSymbol() {
        return symbol;
    }
}
