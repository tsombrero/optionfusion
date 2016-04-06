package com.optionfusion.jobqueue;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.events.WatchListUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;

public class GetWatchlistJob extends BaseApiJob {

    public GetWatchlistJob() {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .singleInstanceBy(GetWatchlistJob.class.getSimpleName()));
    }

    @Override
    public void onRun() throws Throwable {
        FusionUser user = accountClient.getAccountUser();
        if (user != null) {
            ArrayList<Interfaces.StockQuote> stockQuotes = stockQuoteProvider.get(user.getWatchlistTickers());
            bus.post(new WatchListUpdatedEvent(stockQuotes));
        }
    }
}
