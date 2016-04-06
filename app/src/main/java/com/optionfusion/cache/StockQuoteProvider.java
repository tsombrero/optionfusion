package com.optionfusion.cache;

import android.content.Context;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.jobqueue.GetStockQuotesJob;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.dummy.DummyStockQuote;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StockQuoteProvider extends HashMap<String, Interfaces.StockQuote> {
    private final Context context;
    private final ClientInterfaces.StockQuoteClient stockQuoteClient;
    private final JobManager jobManager;
    private final String TAG = OptionChainProvider.class.getSimpleName();

    private long minTimeBetweenFetches = TimeUnit.SECONDS.toMillis(30);

    public StockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient, EventBus bus, JobManager jobManager) {
        this.context = context;
        this.stockQuoteClient = stockQuoteClient;
        this.jobManager = jobManager;
        bus.register(this);
    }

    public Interfaces.StockQuote get(@NotNull String symbol) {
        ArrayList<Interfaces.StockQuote> list = get(Collections.singletonList(symbol));
        if (list != null && !list.isEmpty())
            return list.get(0);

        return null;
    }

    public ArrayList<Interfaces.StockQuote> get(@NotNull List<String> symbols) {
        ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();

        boolean needsUpdate = false;

        synchronized (TAG) {
            for (String symbol : symbols) {
                Interfaces.StockQuote quote = super.get(symbol);
                if (quote != null) {
                    ret.add(quote);
                    needsUpdate |= (System.currentTimeMillis() - quote.getLastUpdatedLocalTimestamp() > minTimeBetweenFetches);
                } else {
                    Interfaces.StockQuote dummy = new DummyStockQuote(symbol);
                    put(symbol, dummy);
                }
            }
        }

        if (needsUpdate)
            jobManager.addJobInBackground(new GetStockQuotesJob(symbols));

        Collections.sort(ret, Interfaces.StockQuote.COMPARATOR);

        return ret;
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(StockQuotesUpdatedEvent event) {
        synchronized (TAG) {
            for (Interfaces.StockQuote quote : event.getStockQuoteList()) {
                put(quote.getSymbol(), quote);
            }
        }
    }

    public abstract static class StockQuoteCallback extends ClientInterfaces.Callback<List<Interfaces.StockQuote>> {
    }
}
