package com.optionfusion.events;

import com.optionfusion.model.provider.Interfaces;

import java.util.List;

public class WatchListUpdatedEvent {
    private final List<Interfaces.StockQuote> watchList;

    public List<Interfaces.StockQuote> getWatchList() {
        return watchList;
    }

    public WatchListUpdatedEvent(List<Interfaces.StockQuote> watchList) {
        this.watchList = watchList;
    }
}
