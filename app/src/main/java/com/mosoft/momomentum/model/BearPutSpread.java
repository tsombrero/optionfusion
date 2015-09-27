package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.amtd.OptionChain;

public class BearPutSpread extends Spread {
    protected BearPutSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        super(buy, sell, underlying);
    }

    @Override
    public Double getMaxValueAtExpiration() {
        return buy.getStrike() - sell.getStrike();
    }

    @Override
    public Double getPrice_BreakEven() {
        return buy.getStrike() - getAsk();
    }

    @Override
    public Double getBreakEvenDepth() {
        return getPrice_BreakEven() - underlying.getLast();
    }
}
