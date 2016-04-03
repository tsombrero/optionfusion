package com.optionfusion.events;

import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StockQuotesUpdatedEvent {
    private final Map<String, Interfaces.StockQuote> stockQuoteMap = new HashMap<>();

    public StockQuotesUpdatedEvent(Map<String, Interfaces.StockQuote> stockQuotes) {
        this.stockQuoteMap.putAll(stockQuotes);
    }

    public StockQuotesUpdatedEvent(List<Interfaces.StockQuote> stockQuoteList) {
        for (Interfaces.StockQuote stockQuote : stockQuoteList) {
            stockQuoteMap.put(stockQuote.getSymbol(), stockQuote);
        }
    }

    public List<Interfaces.StockQuote> getStockQuoteList() {
        return new ArrayList(stockQuoteMap.values());
    }

    public Interfaces.StockQuote getStockQuote(String symbol) {
        return stockQuoteMap.get(symbol);
    }
}
