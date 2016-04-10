package com.optionfusion.jobqueue;

import android.text.TextUtils;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.events.WatchListUpdatedEvent;

import java.util.Collection;
import java.util.List;

public class SetWatchlistJob extends BaseApiJob {
    private final Collection<String> symbols;

    public SetWatchlistJob(Collection<String> symbols) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST)
                .singleInstanceBy(SetWatchlistJob.class.getSimpleName() + TextUtils.join(",", symbols)));
        this.symbols = symbols;
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        List<Equity> watchList = accountClient.setWatchlist(symbols);
        bus.post(WatchListUpdatedEvent.fromEquityList(watchList));
    }
}
