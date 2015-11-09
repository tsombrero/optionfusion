package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.provider.Interfaces;

public class BearPutSpread extends Spread {
    protected BearPutSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.StockQuote underlying) {
        super(buy, sell, underlying);
    }

    @Override
    public double getMaxValueAtExpiration() {
        return buy.getStrike() - sell.getStrike();
    }

    @Override
    public double getPrice_BreakEven() {
        return buy.getStrike() - getAsk();
    }

    @Override
    public double getBreakEvenDepth() {
        return getPrice_BreakEven() - underlying.getLast();
    }
}
