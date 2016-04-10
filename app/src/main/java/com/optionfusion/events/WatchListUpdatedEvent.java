package com.optionfusion.events;

import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.backend.FusionStockQuote;

import java.util.ArrayList;
import java.util.List;

public class WatchListUpdatedEvent {
    private List<Interfaces.StockQuote> watchList;

    public List<Interfaces.StockQuote> getWatchList() {
        return new ArrayList<>(watchList);
    }

    private WatchListUpdatedEvent() {
    }

    public static WatchListUpdatedEvent fromEquityList(List<Equity> equities) {
        WatchListUpdatedEvent event = new WatchListUpdatedEvent();
        event.watchList = new ArrayList<>();
        for (Equity equity : equities) {
            event.watchList.add(new FusionStockQuote(equity));
        }
        return event;
    }

    public static WatchListUpdatedEvent fromStockQuoteList(List<Interfaces.StockQuote> stockQuotes) {
        WatchListUpdatedEvent event = new WatchListUpdatedEvent();
        event.watchList = new ArrayList<>(stockQuotes);
        return event;
    }
}
