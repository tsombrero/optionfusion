package com.optionfusion.jobqueue;

import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.events.WatchListUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.List;

public class GetWatchlistJob extends BaseApiJob {

    private static final String TAG = "GetWatchlistJob";

    public GetWatchlistJob() {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST));
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        List<Interfaces.StockQuote> stockQuotes = accountClient.getWatchlist();
        Log.d(TAG, "Got " + (stockQuotes == null ? "0" : stockQuotes.size()) + " quotes from FusionUser");
        stockQuoteProvider.refresh(stockQuotes);
        bus.post(WatchListUpdatedEvent.fromStockQuoteList(stockQuotes));
    }
}
