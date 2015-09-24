package com.mosoft.momomentum.model;

public class BullCallSpread extends Spread {
    public BullCallSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
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
    public Double getDepthOfBreakeven() {
        return underlying.getLast() - getPrice_BreakEven();
    }
}
