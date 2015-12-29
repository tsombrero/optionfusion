package com.mosoft.optionfusion.model.provider.goog;

import com.mosoft.optionfusion.model.HistoricalQuote;
import com.mosoft.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;

public class GoogPriceHistory implements Interfaces.StockPriceHistory {

    private String symbol;
    private Interval interval;
    private ArrayList<HistoricalQuote> prices;

    public GoogPriceHistory() {
        prices = new ArrayList<>();
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
    public HistoricalQuote[] getPrices() {
        return prices.toArray(new HistoricalQuote[]{});
    }

    @Override
    public long getAgeOfLastEntryMs() {
        if (prices.isEmpty())
            return System.currentTimeMillis();

        return System.currentTimeMillis() - prices.get(prices.size() - 1).getDate();
    }

    @Override
    public void addQuote(HistoricalQuote quote) {
        if (prices.isEmpty() || quote.getDate() > prices.get(prices.size() - 1).getDate())
            prices.add(quote);

        prices.remove(quote);
        prices.add(quote);

        return;
    }
}
