package com.mosoft.momomentum.model;

public class BearPutSpread extends Spread {
    public BearPutSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
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
    public Double getDepthOfBreakeven() {
        return getPrice_BreakEven() - underlying.getLast();
    }
}
