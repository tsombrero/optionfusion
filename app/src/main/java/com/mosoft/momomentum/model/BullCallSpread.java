package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.amtd.OptionChain;

public class BullCallSpread extends Spread {
    protected BullCallSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        super(buy, sell, underlying);
    }

    @Override
    public Double getMaxValueAtExpiration() {
        return sell.getStrike() - buy.getStrike();
    }

    @Override
    public Double getPrice_BreakEven() {
        return buy.getStrike() + getAsk();
    }

    @Override
    public Double getPriceChangeToBreakEven() {
        return getPrice_BreakEven() - underlying.getLast();
    }

    @Override
    public Double getBreakEvenDepth() {
        return underlying.getLast() - getPrice_BreakEven();
    }
}
