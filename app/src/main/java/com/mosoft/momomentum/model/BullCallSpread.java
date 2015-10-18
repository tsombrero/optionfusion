package com.mosoft.momomentum.model;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

public class BullCallSpread extends Spread {
    protected BullCallSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, OptionChain underlying) {
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
