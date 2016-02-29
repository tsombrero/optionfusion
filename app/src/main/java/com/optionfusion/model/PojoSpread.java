package com.optionfusion.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.optionfusion.model.provider.ClassFactory;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.util.Util;

import org.joda.time.DateTime;

import java.util.Comparator;

abstract public class PojoSpread implements Parcelable, com.optionfusion.model.provider.VerticalSpread {
    Interfaces.OptionQuote buy, sell;
    Interfaces.StockQuote underlying;

    private static final String TAG = "Spread";

    // Sort by break-even price distance from the current price, with a small weight for profitability
    public static final Comparator<PojoSpread> ASCENDING_RISK_COMPARATOR = new Comparator<PojoSpread>() {
        @Override
        public int compare(PojoSpread lhs, PojoSpread rhs) {
            return Double.compare(rhs.getWeightedValue(), lhs.getWeightedValue());
        }
    };

    public static final Comparator<PojoSpread> DESCENDING_MAX_RETURN_COMPARATOR = new Comparator<PojoSpread>() {
        @Override
        public int compare(PojoSpread lhs, PojoSpread rhs) {
            return Double.compare(rhs.getMaxReturnAnnualized(), lhs.getMaxReturnAnnualized());
        }
    };

    protected PojoSpread() {}

    protected PojoSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.StockQuote underlying) {
        if (buy == null || sell == null || underlying == null)
            throw new IllegalArgumentException("Quotes and Chain cannot be null");

        this.buy = buy;
        this.sell = sell;
        this.underlying = underlying;
    }

    @Override
    public double getBuyStrike() {
        return getBuy().getStrike();
    }

    @Override
    public double getSellStrike() {
        return getSell().getStrike();
    }

    @Override
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

    public static PojoSpread newSpread(Interfaces.OptionQuote buy, Interfaces.OptionQuote sell, Interfaces.StockQuote underlying) {
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

        if (!buy.hasAsk() || !sell.hasBid() || buy.getStrike() < 0.5D || sell.getStrike() < 0.5D)
            return null;

        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            if (buy.getStrike() < sell.getStrike()) {
                if (buy.getBid() - sell.getAsk() < 0.5d)
                    return null;

                //Bull Call Spread
                return new BullCallSpread(buy, sell, underlying);
            }
            if (buy.getStrike() > sell.getStrike()) {
                //TODO Bear call spread not impl
                return null;
            }
        } else if (buy.getOptionType() == Interfaces.OptionType.PUT) {
            if (buy.getStrike() > sell.getStrike()) {
                if (buy.getBid() - sell.getAsk() < 0.5d)
                    return null;

                //Bear Put Spread
                return new BearPutSpread(buy, sell, underlying);
            }
            if (buy.getStrike() < sell.getStrike()) {
                //TODO Bull put spread not impl
                return null;
            }
        }
        return null;
    }

    @Override
    public double getAsk() {
        return buy.getAsk() - sell.getBid();
    }

    @Override
    public double getBid() {
        return buy.getBid() - sell.getAsk();
    }

    public Interfaces.OptionQuote getBuy() {
        return buy;
    }

    public Interfaces.OptionQuote getSell() {
        return sell;
    }

    @Override
    public double getMaxReturn() {
        return getMaxValueAtExpiration() - getAsk();
    }


    @Override
    public double getMaxPercentProfitAtExpiration() {
        return getMaxReturn() / getAsk();
    }

    // how much $ the price can change before the max profit limit
    @Override
    public double getPriceChange_MaxProfit() {
        return sell.getStrike() - underlying.getLast();
    }

    // how much % the price can drop before cutting into profit
    @Override
    public double getPercentChange_MaxProfit() {
        return getPriceChange_MaxProfit() / underlying.getLast();
    }

    @Override
    public DateTime getExpiresDate() {
        return buy.getExpiration();
    }

    @Override
    public double getMaxReturnAnnualized() {
        return Math.pow(1d + getMaxPercentProfitAtExpiration(), 365d / (double) getDaysToExpiration()) - 1d;
    }

    public String toString() {
        return String.format("%s $%.2f; dte:%d; spr:%.2f/%.2f b/a:$%.2f/%.2f MaxProfit: %s / %.1f%% risk:%.3f",
                underlying.getSymbol(),
                underlying.getLast(),
                buy.getDaysUntilExpiration(),
                buy.getStrike(), sell.getStrike(),
                getAsk(), getBid(),
                Util.formatDollars(getMaxReturn()),
                getMaxPercentProfitAtExpiration() * 100d,
                getWeightedValue()) + getWeightComponents();
    }

    public String getWeightComponents() {
        return " WT: " + getMaxReturnAnnualized() + "/5 + " + getBreakEvenDepth() + " / " + underlying.getLast();

    }

    @Override
    public int getDaysToExpiration() {
        return buy.getDaysUntilExpiration();
    }

    @Override
    public boolean isInTheMoney_BreakEven() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            return getPrice_BreakEven() < underlying.getLast();
        }
        return getPrice_BreakEven() > underlying.getLast();
    }

    @Override
    public boolean isInTheMoney_MaxReturn() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL) {
            return getPrice_MaxReturn() < underlying.getLast();
        }
        return getPrice_MaxReturn() > underlying.getLast();
    }

    @Override
    public double getPrice_MaxReturn() {
        return sell.getStrike();
    }

    @Override
    public double getPrice_MaxLoss() {
        return buy.getStrike();
    }

    @Override
    public boolean isCall() {
        return buy.getOptionType() == Interfaces.OptionType.CALL;
    }

    @Override
    public String getUnderlyingSymbol() {
        return underlying.getSymbol();
    }

    @Override
    public boolean isBullSpread() {
        if (buy.getOptionType() == Interfaces.OptionType.CALL
                && sell.getOptionType() == Interfaces.OptionType.CALL
                && buy.getStrike() < sell.getStrike())
            return true;

        return buy.getOptionType() == Interfaces.OptionType.PUT
                && sell.getOptionType() == Interfaces.OptionType.PUT
                && buy.getStrike() < sell.getStrike();

    }

    @Override
    public boolean isBearSpread() {
        return !isBullSpread();
    }

    @Override
    public double getWeightedValue() {
        return Math.min(.5, getMaxReturnAnnualized() / 5D) + (WEIGHT_LOWRISK * getBreakEvenDepth() / underlying.getLast());
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getBuy().getProvider().ordinal());
        dest.writeString(getBuy().toJson(ClassFactory.gson));
        dest.writeInt(getSell().getProvider().ordinal());
        dest.writeString(getSell().toJson(ClassFactory.gson));
        dest.writeInt(underlying.getProvider().ordinal());
        dest.writeString(underlying.toJson(ClassFactory.gson));
    }

    public static final Parcelable.Creator<PojoSpread> CREATOR = new SpreadCreator();

    public static class SpreadCreator implements Parcelable.Creator<PojoSpread> {
        public PojoSpread createFromParcel(Parcel in) {
            Interfaces.OptionQuote buy = ClassFactory.OptionQuoteFromJson(readProvider(in), in.readString());
            Interfaces.OptionQuote sell = ClassFactory.OptionQuoteFromJson(readProvider(in), in.readString());
            Interfaces.OptionChain oc = ClassFactory.OptionChainFromJson(readProvider(in), in.readString());
            return PojoSpread.newSpread(buy, sell, oc.getUnderlyingStockQuote());
        }

        public PojoSpread[] newArray(int size) {
            return new PojoSpread[size];
        }

        private OptionFusionApplication.Provider readProvider(Parcel in) {
            return OptionFusionApplication.Provider.values()[in.readInt()];
        }
    }

    @Override
    public String getDescriptionNoExp() {
        return String.format("%s %.2f/%.2f", getSpreadType().toString(), getBuy().getStrike(), getSell().getStrike());
    }

    @Override
    public String getDescription() {
        return String.format("%s %.2f/%.2f %s", getSpreadType().toString(), getBuy().getStrike(), getSell().getStrike(), Util.getFormattedOptionDate(getExpiresDate()));
    }

    @Override
    public String getBuySymbol() {
        return getBuy().getOptionSymbol();
    }

    @Override
    public String getSellSymbol() {
        return getSell().getOptionSymbol();
    }
}
