package com.optionfusion.model;

import android.os.Parcelable;

import com.optionfusion.model.provider.Interfaces;

public class BearPutSpread extends PojoSpread {
    protected BearPutSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.OptionChain underlying) {
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
        return getPrice_BreakEven() - chain.getUnderlyingPrice();
    }

    public static final Parcelable.Creator<PojoSpread> CREATOR = new SpreadCreator();
}
