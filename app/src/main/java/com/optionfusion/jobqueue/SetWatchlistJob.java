package com.optionfusion.jobqueue;

import android.text.TextUtils;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.events.WatchListUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SetWatchlistJob extends BaseApiJob {
    private Collection<String> symbols;

    static public SetWatchlistJob fromStockQuoteList(Collection<Interfaces.StockQuote> stockQuoteList) {
        List<String> symbols = new ArrayList<>();
        if (stockQuoteList != null) {
            for (Interfaces.StockQuote stockQuote : stockQuoteList) {
                symbols.add(stockQuote.getSymbol());
            }
        }
        return new SetWatchlistJob(symbols);
    }

    public SetWatchlistJob(Collection<String> symbols) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST)
                .singleInstanceBy(SetWatchlistJob.class.getSimpleName() + TextUtils.join(",", symbols)));
        this.symbols = symbols;
    }

    @Override
    public void onAdded() {
        bus.post(WatchListUpdatedEvent.fromStockQuoteList(stockQuoteProvider.getFromSymbols(symbols)));
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        List<Equity> watchList = accountClient.setWatchlist(symbols);
        bus.post(WatchListUpdatedEvent.fromEquityList(watchList));
    }
}
