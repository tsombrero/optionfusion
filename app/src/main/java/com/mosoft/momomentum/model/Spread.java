package com.mosoft.momomentum.model;

import com.mosoft.momomentum.util.Util;

import java.util.Comparator;

abstract public class Spread {
    OptionChain.OptionQuote buy, sell;

    OptionChain.Data underlying;

    public Spread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
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

    public Double getMaxProfitAtExpiration() {
        return getMaxValueAtExpiration() - getAsk();
    }

    abstract public Double getMaxValueAtExpiration();
    abstract public Double getPrice_BreakEven();

    public Double getMaxPercentProfitAtExpiration() {
        return getMaxProfitAtExpiration() / getAsk();
    }

    // how much $ the price can drop before cutting into profit
    public Double getMaxPriceChange_MaxProfit() {
        return Math.abs(underlying.getBid() - sell.getStrike());
    }

    // how much % the price can drop before cutting into profit
    public Double getMaxPercentChange_MaxProfit() {
        return getMaxPriceChange_MaxProfit() / underlying.getAsk();
    }

    // Used for sorting purposes only, if we ever need "Annualized Profit %" that can replace this
    private Double profitWeight;

    private Double getProfitWeight() {
        if (profitWeight == null)
            profitWeight = getMaxPercentProfitAtExpiration() / getDaysToExpiration();
        return profitWeight;
    }

    abstract public Double getDepthOfBreakeven();
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

    public static class AscendingAnnualizedProfitComparator implements Comparator<Spread> {

        @Override
        public int compare(Spread lhs, Spread rhs) {
            return lhs.getProfitWeight().compareTo(rhs.getProfitWeight());
        }
    }

    public static class AscendingBreakEvenComparator implements Comparator<Spread> {

        @Override
        public int compare(Spread lhs, Spread rhs) {
            return lhs.getPrice_BreakEven().compareTo(rhs.getPrice_BreakEven());
        }
    }

    // Sort by break-even price distance from the current price
    public static class DescendingBreakEvenDepthComparator implements Comparator<Spread> {
        @Override
        public int compare(Spread lhs, Spread rhs) {
            return rhs.getDepthOfBreakeven().compareTo(lhs.getDepthOfBreakeven());
        }
    }
}
