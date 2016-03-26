package com.optionfusion.model;

import android.os.Parcelable;

import com.optionfusion.model.provider.Interfaces;

public class BullCallSpread extends PojoSpread {
    protected BullCallSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.OptionChain chain) {
        super(buy, sell, chain);
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
        return chain.getUnderlyingPrice() - getPrice_BreakEven();
    }

    public static final Parcelable.Creator<PojoSpread> CREATOR = new SpreadCreator();
}
