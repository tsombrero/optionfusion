package com.mosoft.momomentum.model;

import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.util.Util;

import java.util.Comparator;
import java.util.Date;

abstract public class Spread {
    OptionChain.OptionQuote buy, sell;

    OptionChain.Data underlying;

    protected Spread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        if (buy == null || sell == null || underlying == null)
            throw new IllegalArgumentException("Quotes and Chain cannot be null");

        this.buy = buy;
        this.sell = sell;
        this.underlying = underlying;
    }

    public static Spread newSpread(OptionChain.OptionQuote buy, OptionChain.OptionQuote sell, OptionChain.Data underlying) {
        if (buy == null || sell == null)
            return null;

        //whatever this is, not impl
        if (buy.getOptionType() != sell.getOptionType())
            return null;

        //calendar spreads not impl
        if (buy.getDaysUntilExpiration() != sell.getDaysUntilExpiration())
            return null;

        // don't mix multipliers
        if (buy.getMultiplier() != sell.getMultiplier())
            return null;

        if (!buy.hasAsk() || !sell.hasBid())
            return null;

        if (buy.getOptionType() == OptionChain.OptionType.CALL) {
            if (buy.getStrike() < sell.getStrike()) {
                //Bull Call Spread
                return new BullCallSpread(buy, sell, underlying);
            }
            if (buy.getStrike() > sell.getStrike()) {
                //Bear call spread not impl
                return null;
            }
        } else if (buy.getOptionType() == OptionChain.OptionType.PUT) {
            if (buy.getStrike() > sell.getStrike()) {
                //Bear Put Spread
                return new BearPutSpread(buy, sell, underlying);
            }
            if (buy.getStrike() < sell.getStrike()) {
                //Bull put spread not impl
                return null;
            }
        }
        return null;
    }

    public double getAsk() {
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

    abstract public double getMaxValueAtExpiration();

    abstract public double getPrice_BreakEven();


    public double getMaxPercentProfitAtExpiration() {
        return getMaxProfitAtExpiration() / getAsk();
    }

    public double getPriceChange_BreakEven() {
        return getPrice_BreakEven() - underlying.getLast();
    }

    public double getPercentChange_BreakEven() {
        return getPriceChange_BreakEven() / underlying.getLast();
    }

    // how much $ the price can change before the max profit limit
    public double getPriceChange_MaxProfit() {
        return sell.getStrike() - underlying.getLast();
    }

    // how much % the price can drop before cutting into profit
    public double getPercentChange_MaxProfit() {
        return getPriceChange_MaxProfit() / underlying.getLast();
    }

    public Date getExpiresDate() {
        return buy.getExpiration();
    }

    public double getMaxReturnAnnualized() {
        return Math.pow(1d + getMaxPercentProfitAtExpiration(), 365d / (double) getDaysToExpiration()) - 1d;
    }

    abstract public double getBreakEvenDepth();

    public String toString() {
        return String.format("%s $%.2f; dte:%d; spr:%.2f/%.2f ask:$%.2f MaxProfit: %s / %.1f%% weighted:%.3f",
                underlying.getSymbol(),
                underlying.getClose(),
                buy.getDaysUntilExpiration(),
                buy.getStrike(), sell.getStrike(),
                getAsk(),
                Util.formatDollars(getMaxProfitAtExpiration()),
                getMaxPercentProfitAtExpiration() * 100d,
                getMaxPercentProfitAtExpiration() / (double) getDaysToExpiration());
    }

    public int getDaysToExpiration() {
        return buy.getDaysUntilExpiration();
    }

    public boolean isInTheMoney_BreakEven() {
        if (buy.getOptionType() == OptionChain.OptionType.CALL) {
            return getPrice_BreakEven() < underlying.getLast();
        }
        return getPrice_BreakEven() > underlying.getLast();
    }

    public boolean isInTheMoney_MaxReturn() {
        if (buy.getOptionType() == OptionChain.OptionType.CALL) {
            return getPrice_MaxReturn() < underlying.getLast();
        }
        return getPrice_MaxReturn() > underlying.getLast();
    }

    public double getPrice_MaxReturn() {
        return sell.getStrike();
    }

    public boolean isCall() {
        return buy.getOptionType() == OptionChain.OptionType.CALL;
    }

    public String getUnderlyingSymbol() {
        return underlying.getSymbol();
    }

    public boolean isBullSpread() {
        if (buy.getOptionType() == OptionChain.OptionType.CALL
                && sell.getOptionType() == OptionChain.OptionType.CALL
                && buy.getStrike() < sell.getStrike())
            return true;

        if (buy.getOptionType() == OptionChain.OptionType.PUT
                && sell.getOptionType() == OptionChain.OptionType.PUT
                && buy.getStrike() < sell.getStrike())
            return true;

        return false;
    }

    public boolean isBearSpread() {
        return !isBullSpread();
    }

    // Sort by break-even price distance from the current price
    public static class DescendingBreakEvenDepthComparator implements Comparator<Spread> {
        @Override
        public int compare(Spread lhs, Spread rhs) {
            if (rhs.getBreakEvenDepth() > lhs.getBreakEvenDepth())
                return 1;
            if (rhs.getBreakEvenDepth() < lhs.getBreakEvenDepth())
                return -1;
            return 0;
        }
    }
}
