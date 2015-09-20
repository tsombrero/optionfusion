package com.mosoft.momomentum.model;

public class BullCallSpread {
    OptionChain.OptionQuote buy, sell;

    OptionChain.Data underlying;

    public BullCallSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        this.buy = buy;
        this.sell = sell;
        this.underlying = underlying;
    }

    public Double getAsk() {
        return buy.getAsk() - sell.getBid();
    }

    public OptionChain.OptionQuote getBuy() {
        return buy;
    }

    public OptionChain.OptionQuote getSell() {
        return sell;
    }

    public Double getMaxValueAtExpiration() {
        return buy.getStrike() - sell.getStrike();
    }

    public Double getMaxProfitAtExpiration() {
        return getMaxValueAtExpiration() - getAsk();
    }

    public Double getMaxPriceDrop_MaxProfit() {
        return underlying.getBid() - sell.getStrike();
    }

    public Double getMaxPercentDrop_MaxProfit() {
        return getMaxPriceDrop_MaxProfit() / underlying.getAsk();
    }

    public Double getPrice_BreakEven() {
        return buy.getStrike() + getAsk();
    }

}
