package com.optionfusion.cache;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.StockQuote;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.jobqueue.GetStockQuotesJob;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.backend.FusionStockQuote;
import com.optionfusion.model.provider.dummy.DummyStockQuote;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StockQuoteProvider {
    private final JobManager jobManager;
    private final String TAG = OptionChainProvider.class.getSimpleName();

    private final HashMap<String, Interfaces.StockQuote> data = new HashMap<>();

    private long minTimeBetweenFetches = TimeUnit.SECONDS.toMillis(30);

    public StockQuoteProvider(EventBus bus, JobManager jobManager) {
        this.jobManager = jobManager;
        bus.register(this);
    }

    public Interfaces.StockQuote get(@NotNull String symbol) {
        ArrayList<Interfaces.StockQuote> list = getFromSymbols(Collections.singletonList(symbol));
        if (list != null && !list.isEmpty())
            return list.get(0);

        return null;
    }

    public ArrayList<Interfaces.StockQuote> getFromEquityList(@NotNull List<Equity> equities) {
        ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();

        boolean needsUpdate = false;

        if (equities == null)
            return ret;

        synchronized (TAG) {
            for (Equity equity : equities) {
                Interfaces.StockQuote oldQuote = data.get(equity.getSymbol());
                Interfaces.StockQuote newQuote = new FusionStockQuote(equity);

                Interfaces.StockQuote quoteToUse = oldQuote;

                if (oldQuote == null || oldQuote.getQuoteTimestamp() <= newQuote.getQuoteTimestamp()) {
                    quoteToUse = newQuote;
                }

                ret.add(quoteToUse);
                needsUpdate |= (System.currentTimeMillis() - quoteToUse.getLastUpdatedLocalTimestamp() > minTimeBetweenFetches);
            }
        }

        if (needsUpdate)
            jobManager.addJobInBackground(GetStockQuotesJob.fromEquities(equities));

        Collections.sort(ret, Interfaces.StockQuote.COMPARATOR);

        return ret;
    }

    public ArrayList<Interfaces.StockQuote> getFromSymbols(@NotNull Collection<String> symbols) {
        ArrayList<Interfaces.StockQuote> ret = new ArrayList<>();

        boolean needsUpdate = false;

        synchronized (TAG) {
            for (String symbol : symbols) {
                Interfaces.StockQuote quote = data.get(symbol);
                if (quote != null) {
                    ret.add(quote);
                    needsUpdate |= (System.currentTimeMillis() - quote.getLastUpdatedLocalTimestamp() > minTimeBetweenFetches);
                } else {
                    Interfaces.StockQuote dummy = new DummyStockQuote(symbol);
                    data.put(symbol, dummy);
                    needsUpdate = true;
                }
            }
        }

        if (needsUpdate)
            jobManager.addJobInBackground(GetStockQuotesJob.fromSymbols(symbols));

        Collections.sort(ret, Interfaces.StockQuote.COMPARATOR);

        return ret;
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(StockQuotesUpdatedEvent event) {
        synchronized (TAG) {
            for (Interfaces.StockQuote quote : event.getStockQuoteList()) {
                data.put(quote.getSymbol(), quote);
            }
        }
    }
}
