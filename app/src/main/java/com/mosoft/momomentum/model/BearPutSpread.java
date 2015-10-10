package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.provider.amtd.OptionChain;

public class BearPutSpread extends Spread {
    protected BearPutSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
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
