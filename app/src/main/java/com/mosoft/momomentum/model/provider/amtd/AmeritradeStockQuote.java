package com.mosoft.momomentum.model.provider.amtd;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.provider.Interfaces;

public class AmeritradeStockQuote implements Interfaces.StockQuote {
    

    @Override
    public String getSymbol() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
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
        return null;
    }
}
