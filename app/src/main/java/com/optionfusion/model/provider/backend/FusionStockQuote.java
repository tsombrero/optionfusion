package com.optionfusion.model.provider.backend;

import com.google.gson.Gson;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.StockQuote;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

public class FusionStockQuote implements Interfaces.StockQuote {


    private final Equity equity;

    public FusionStockQuote(Equity equity) {
        this.equity = equity;
    }

    // TODO we probably don't need this or OptionChainProto.StockQuote in general
    public FusionStockQuote(OptionChainProto.StockQuote protoStockQuote) {
        equity = new Equity();
        StockQuote stockQuote = new StockQuote();
        stockQuote.setTicker(protoStockQuote.getSymbol());
        stockQuote.setClose(protoStockQuote.getClose());
        stockQuote.setDataTimestamp(protoStockQuote.getTimestamp());
        stockQuote.setVolume(protoStockQuote.getVolume());
    }

    @Override
    public String getSymbol() {
        return equity.getTicker();
    }

    @Override
    public String getDescription() {
        return equity.getDescription();
    }

    @Override
    public double getBid() {
        return equity.getEodStockQuote().getClose();
    }

    @Override
    public double getAsk() {
        return equity.getEodStockQuote().getClose();
    }

    @Override
    public double getLast() {
        return equity.getEodStockQuote().getClose();
    }

    @Override
    public double getOpen() {
        return equity.getEodStockQuote().getOpen();
    }

    @Override
    public double getClose() {
        return equity.getEodStockQuote().getClose();
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
        return getClose() - equity.getEodStockQuote().getPreviousClose();
    }

    @Override
    public Double getChangePercent() {
        return getChange() / equity.getEodStockQuote().getPreviousClose();
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return equity.getEodStockQuote().getDataTimestamp();
    }
}
