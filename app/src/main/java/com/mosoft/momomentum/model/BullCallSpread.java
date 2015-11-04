package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeOptionChain;

public class BullCallSpread extends Spread {
    protected BullCallSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, AmeritradeOptionChain underlying) {
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
