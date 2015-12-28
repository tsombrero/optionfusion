package com.mosoft.optionfusion.model.provider.goog;

import com.mosoft.optionfusion.model.HistoricalQuote;
import com.mosoft.optionfusion.model.provider.Interfaces;

import java.util.HashMap;
import java.util.Map;

public class GoogPriceHistory implements Interfaces.StockPriceHistory {

    private String symbol;
    private Interval interval;
    private Map<Long, HistoricalQuote> prices;

    public GoogPriceHistory(String symbol, Interval interval, Map<Long, HistoricalQuote> prices) {
        this.symbol = symbol;
        this.interval = interval;
        this.prices = prices;
    }

    public GoogPriceHistory() {
        prices = new HashMap<>();
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Interval getInterval() {
        return interval;
    }

    @Override
    public Map<Long, HistoricalQuote> getPrices() {
        return prices;
    }
}
