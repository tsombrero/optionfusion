package com.optionfusion.model.provider.backend;

import com.google.gson.Gson;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

public class FusionStockQuote implements Interfaces.StockQuote {


    private final OptionChainProto.StockQuote stockquote;

    public FusionStockQuote(OptionChainProto.StockQuote stockquote) {
        this.stockquote = stockquote;
    }

    @Override
    public String getSymbol() {
        return stockquote.getSymbol();
    }

    @Override
    public String getDescription() {
        //TODO
        return stockquote.getSymbol();
    }

    @Override
    public double getBid() {
        return stockquote.getClose();
    }

    @Override
    public double getAsk() {
        return stockquote.getClose();
    }

    @Override
    public double getLast() {
        return stockquote.getClose();
    }

    @Override
    public double getOpen() {
        return stockquote.getOpen();
    }

    @Override
    public double getClose() {
        return stockquote.getClose();
    }

    @Override
    public String toJson(Gson gson) {
        return this.toJson(gson);
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.OPTION_FUSION_BACKEND;
    }

    @Override
    public Double getChange() {
        return getClose() - getOpen();
    }

    @Override
    public Double getChangePercent() {
        return getChange() / getOpen();
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return stockquote.getTimestamp();
    }
}
