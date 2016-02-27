package com.optionfusion.model.filter;

import android.os.Parcel;
import android.os.Parcelable;

import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.ui.widgets.rangebar.RangeBar;
import com.optionfusion.model.Spread;
import com.optionfusion.util.Util;

import java.util.ArrayList;

public class StrikeFilter extends Filter implements RangeBar.RangeBarDataProvider {

    private final double limitLo;
    private final double limitHi;

    transient public static final StrikeFilter EMPTY_BULLISH = new StrikeFilter(0, Double.MAX_VALUE, Type.BULLISH);
    transient public static final StrikeFilter EMPTY_BEARISH = new StrikeFilter(0, Double.MAX_VALUE, Type.BEARISH);

    public Double getMinValue() {
        return limitLo;
    }

    public Double getMaxValue() {
        return limitHi;
    }

    public enum Type {BULLISH, BEARISH}

    private final Type type;

    public StrikeFilter(double limitLo, double limitHi, Type type) {

        this.limitLo = limitLo;
        this.limitHi = limitHi;
        this.type = type;
    }

    @Override
    public void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs) {
        //TODO
    }

    @Override
    public boolean pass(Spread spread) {
        Type spreadType = spread.isBullSpread() ? Type.BULLISH : Type.BEARISH;
        if (spreadType != type)
            return true;
        return pass(spread.getPrice_MaxReturn());
    }

    private boolean pass(double limit) {
        return limit >= this.limitLo && limit <= this.limitHi;
    }

    @Override
    public boolean pass(Interfaces.OptionDate optionDate) {
        return true;
    }

    @Override
    public boolean pass(Interfaces.OptionQuote optionQuote) {
        return pass(optionQuote.getAsk());
    }

    @Override
    public String getPillText() {
        return (type == Type.BULLISH
                ? "Strike for Bullish Spreads: "
                : "Strike for Bearish Spreads: ")
                + Util.formatDollarRange(limitLo, limitHi);
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof StrikeFilter &&
                ((StrikeFilter) filter).type == this.type;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.STRIKE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    //Parcelable
    public StrikeFilter(Parcel parcel) {
        this.type = Type.values()[parcel.readInt()];
        this.limitLo = parcel.readDouble();
        this.limitHi = parcel.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type.ordinal());
        dest.writeDouble(limitLo);
        dest.writeDouble(limitHi);
    }


    @Override
    public Object getLeftValue() {
        return getMinValue();
    }

    @Override
    public Object getRightValue() {
        return getMaxValue();
    }


    public static final Parcelable.Creator<StrikeFilter> CREATOR
            = new Parcelable.Creator<StrikeFilter>() {
        public StrikeFilter createFromParcel(Parcel in) {
            return new StrikeFilter(in);
        }

        public StrikeFilter[] newArray(int size) {
            return new StrikeFilter[size];
        }
    };


}
