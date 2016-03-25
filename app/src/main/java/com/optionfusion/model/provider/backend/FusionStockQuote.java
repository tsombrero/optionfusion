package com.optionfusion.model.provider.backend;

import com.google.gson.Gson;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.StockQuote;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

public class FusionStockQuote implements Interfaces.StockQuote {

    private final StockQuote stockQuote;
    private String equityDescription;
    private long createTimestamp = System.currentTimeMillis();

    public FusionStockQuote(Equity equity) {
        this.stockQuote = equity.getEodStockQuote();
        this.equityDescription = equity.getDescription();
    }

    @Override
    public String getSymbol() {
        return stockQuote.getTicker();
    }

    @Override
    public String getDescription() {
        return equityDescription;
    }

    @Override
    public double getBid() {
        return stockQuote.getClose();
    }

    @Override
    public double getAsk() {
        return stockQuote.getClose();
    }

    @Override
    public double getLast() {
        return stockQuote.getClose();
    }

    @Override
    public double getOpen() {
        return stockQuote.getOpen();
    }

    @Override
    public double getClose() {
        return stockQuote.getClose();
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.OPTION_FUSION_BACKEND;
    }

    @Override
    public Double getChange() {
        return getClose() - stockQuote.getPreviousClose();
    }

    @Override
    public Double getChangePercent() {
        return getChange() / stockQuote.getPreviousClose();
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return createTimestamp;
    }

    @Override
    public long getQuoteTimestamp() {
        return stockQuote.getDataTimestamp();
    }
}
