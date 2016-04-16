package com.optionfusion.events;

import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.List;

public class WatchListUpdatedEvent {
    private List<Interfaces.StockQuote> watchList;

    public List<Interfaces.StockQuote> getWatchList() {
        return new ArrayList<>(watchList);
    }

    private WatchListUpdatedEvent() {
    }

    public static WatchListUpdatedEvent fromStockQuoteList(List<Interfaces.StockQuote> stockQuotes) {
        WatchListUpdatedEvent event = new WatchListUpdatedEvent();
        event.watchList = new ArrayList<>();

        if (stockQuotes != null)
            event.watchList.addAll(stockQuotes);
        return event;
    }
}
