package com.mosoft.optionfusion.model;

import com.mosoft.optionfusion.model.provider.Interfaces;

public class BullCallSpread extends Spread {
    protected BullCallSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.StockQuote underlying) {
        super(buy, sell, underlying);
    }

    @Override
    public double getMaxValueAtExpiration() {
        return sell.getStrike() - buy.getStrike();
    }

    @Override
    public double getPrice_BreakEven() {
        return buy.getStrike() + getAsk();
    }

    @Override
    public double getBreakEvenDepth() {
        return underlying.getLast() - getPrice_BreakEven();
    }
}
