package com.mosoft.momomentum.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mosoft.momomentum.model.provider.ClassFactory;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import java.util.Comparator;
import java.util.Date;

abstract public class Spread implements Parcelable {
    Interfaces.OptionQuote buy, sell;
    Interfaces.OptionChain underlying;

    public enum SpreadType {
        BULL_CALL,
        BEAR_CALL,
        BULL_PUT,
        BEAR_PUT;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }

    protected Spread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, OptionChain underlying) {
        if (buy == null || sell == null || underlying == null)
            throw new IllegalArgumentException("Quotes and Chain cannot be null");

        this.buy = buy;
        this.sell = sell;
        this.underlying = underlying;
    }

    public SpreadType getSpreadType() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            return buy.getStrike() < sell.getStrike()
                    ? SpreadType.BULL_CALL
                    : SpreadType.BEAR_CALL;
        }

        return buy.getStrike() > sell.getStrike()
                ? SpreadType.BEAR_PUT
                : SpreadType.BULL_PUT;
    }

    public static Spread newSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, OptionChain underlying) {
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

        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            if (buy.getStrike() < sell.getStrike()) {
                //Bull Call Spread
                return new BullCallSpread(buy, sell, underlying);
            }
            if (buy.getStrike() > sell.getStrike()) {
                //Bear call spread not impl
                return null;
            }
        } else if (buy.getOptionType() == Interfaces.OptionType.PUT) {
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

    public Interfaces.OptionQuote getBuy() {
        return buy;
    }

    public Interfaces.OptionQuote getSell() {
        return sell;
    }

    public double getMaxReturn() {
        return getMaxValueAtExpiration() - getAsk();
    }

    abstract public double getMaxValueAtExpiration();

    abstract public double getPrice_BreakEven();


    public double getMaxPercentProfitAtExpiration() {
        return getMaxReturn() / getAsk();
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
                Util.formatDollars(getMaxReturn()),
                getMaxPercentProfitAtExpiration() * 100d,
                getWeightedValue());
    }

    public int getDaysToExpiration() {
        return buy.getDaysUntilExpiration();
    }

    public boolean isInTheMoney_BreakEven() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            return getPrice_BreakEven() < underlying.getLast();
        }
        return getPrice_BreakEven() > underlying.getLast();
    }

    public boolean isInTheMoney_MaxReturn() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            return getPrice_MaxReturn() < underlying.getLast();
        }
        return getPrice_MaxReturn() > underlying.getLast();
    }

    public double getPrice_MaxReturn() {
        return sell.getStrike();
    }

    public double getPrice_MaxLoss() { return buy.getStrike(); }

    public boolean isCall() {
        return buy.getOptionType() == Interfaces.OptionType.CALL;
    }

    public String getUnderlyingSymbol() {
        return underlying.getSymbol();
    }

    public boolean isBullSpread() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL
                && sell.getOptionType() == Interfaces.OptionType.CALL
                && buy.getStrike() < sell.getStrike())
            return true;

        return buy.getOptionType() == Interfaces.OptionType.PUT
                && sell.getOptionType() == Interfaces.OptionType.PUT
                && buy.getStrike() < sell.getStrike();

    }

    public boolean isBearSpread() {
        return !isBullSpread();
    }

    public double getWeightedValue() {
        return getBreakEvenDepth() * Math.max(0, getMaxPercentProfitAtExpiration());
    }

    // Sort by break-even price distance from the current price
    public static class DescendingBreakEvenDepthComparator implements Comparator<Spread> {
        @Override
        public int compare(Spread lhs, Spread rhs) {
            return Double.compare(rhs.getBreakEvenDepth(), lhs.getBreakEvenDepth());
        }
    }

    public static class DescendingWeightComparator implements Comparator<Spread> {
        @Override
        public int compare(Spread lhs, Spread rhs) {
            return Double.compare(rhs.getWeightedValue(), lhs.getWeightedValue());
        }
    }

    public static class DescendingMaxReturnComparator implements Comparator<Spread> {
        @Override
        public int compare(Spread lhs, Spread rhs) {
            return Double.compare(rhs.getMaxPercentProfitAtExpiration(), lhs.getMaxPercentProfitAtExpiration());
        }
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getBuy().getProvider().ordinal());
        dest.writeString(getBuy().toJson(MomentumApplication.getGson()));

        dest.writeInt(getSell().getProvider().ordinal());
        dest.writeString(getSell().toJson(MomentumApplication.getGson()));

        dest.writeInt(underlying.getProvider().ordinal());
        dest.writeString(underlying.toJson(MomentumApplication.getGson()));
    }

    public static final Parcelable.Creator<Spread> CREATOR = new SpreadCreator();

    public static class SpreadCreator implements Parcelable.Creator<Spread> {
        public Spread createFromParcel(Parcel in) {
            Interfaces.Provider provider = Interfaces.Provider.values()[in.readInt()];
            Interfaces.OptionQuote buy = ClassFactory.OptionQuoteFromJson(MomentumApplication.getGson(), provider, in.readString());

            provider = Interfaces.Provider.values()[in.readInt()];
            Interfaces.OptionQuote sell = ClassFactory.OptionQuoteFromJson(MomentumApplication.getGson(), provider, in.readString());

            provider = Interfaces.Provider.values()[in.readInt()];
            OptionChain oc = ClassFactory.OptionChainFromJson(MomentumApplication.getGson(), provider, in.readString());
            return Spread.newSpread(buy, sell, oc);
        }

        public Spread[] newArray(int size) {
            return new Spread[size];
        }
    }
}
