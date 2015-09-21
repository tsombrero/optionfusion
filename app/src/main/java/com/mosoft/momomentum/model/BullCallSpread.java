package com.mosoft.momomentum.model;

import com.mosoft.momomentum.util.Util;

import java.util.Comparator;

public class BullCallSpread {
    OptionChain.OptionQuote buy, sell;

    OptionChain.Data underlying;

    public BullCallSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        if (buy == null || sell == null || underlying == null)
            throw new IllegalArgumentException("Quotes and Chain cannot be null");

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
        return sell.getStrike() - buy.getStrike();
    }

    public Double getMaxProfitAtExpiration() {
        return getMaxValueAtExpiration() - getAsk();
    }

    public Double getMaxPercentProfitAtExpiration() {
        return getMaxProfitAtExpiration() / getAsk();
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

    // Used for sorting purposes only, if we ever need "Annualized Profit %" that can replace this
    private Double profitWeight;
    private Double getProfitWeight() {
        if (profitWeight == null)
            profitWeight = getMaxPercentProfitAtExpiration() / getDaysToExpiration();
        return profitWeight;
    }

    public String toString() {
        return String.format("%s $%.2f; dte:%d; spr:%.2f/%.2f ask:$%.2f MaxProfit: %s / %.1f%% weighted:%.3f",
                underlying.getSymbol(),
                underlying.getClose(),
                buy.getDaysUntilExpiration(),
                buy.getStrike(), sell.getStrike(),
                getAsk(),
                Util.Dollars(getMaxProfitAtExpiration()),
                getMaxPercentProfitAtExpiration() * 100d,
                getMaxPercentProfitAtExpiration() / (double) getDaysToExpiration());
    }

    public int getDaysToExpiration() {
        return buy.getDaysUntilExpiration();
    }

    public static class AscendingHighLegStrikeComparator implements Comparator<BullCallSpread> {

        @Override
        public int compare(BullCallSpread lhs, BullCallSpread rhs) {
            return lhs.getSell().getStrike().compareTo(rhs.getSell().getStrike());
        }
    }

    public static class AscendingAnnualizedProfitComparator implements Comparator<BullCallSpread> {

        @Override
        public int compare(BullCallSpread lhs, BullCallSpread rhs) {
            return lhs.getProfitWeight().compareTo(rhs.getProfitWeight());
        }
    }
}
