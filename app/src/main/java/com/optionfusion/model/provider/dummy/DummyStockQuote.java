package com.optionfusion.model.provider.dummy;

import com.google.gson.Gson;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

public class DummyStockQuote implements Interfaces.StockQuote {
    private String symbol;
    private String description = "";

    public DummyStockQuote(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public DummyStockQuote(String symbol) {
        this(symbol, null);
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public double getBid() {
        return 0;
    }

    @Override
    public double getAsk() {
        return 0;
    }

    @Override
    public double getLast() {
        return 0;
    }

    @Override
    public double getOpen() {
        return 0;
    }

    @Override
    public double getClose() {
        return 0;
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.DUMMY;
    }

    @Override
    public Double getChange() {
        return 0d;
    }

    @Override
    public Double getChangePercent() {
        return 0d;
    }

    @Override
    public long getLastUpdatedLocalTimestamp() {
        return 0;
    }

    @Override
    public long getQuoteTimestamp() {
        return 0;
    }
}
