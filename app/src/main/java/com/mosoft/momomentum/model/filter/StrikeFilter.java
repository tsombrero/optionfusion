package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.ui.widgets.rangebar.RangeBar;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.util.Util;

public class StrikeFilter extends Filter implements RangeBar.RangeBarDataProvider {

    private final double limitLo;
    private final double limitHi;

    public static final StrikeFilter EMPTY_BULLISH = new StrikeFilter(0, Double.MAX_VALUE, Type.BULLISH);
    public static final StrikeFilter EMPTY_BEARISH = new StrikeFilter(0, Double.MAX_VALUE, Type.BEARISH);

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
    public boolean pass(OptionChain.OptionDate optionDate) {
        return true;
    }

    @Override
    public boolean pass(Interfaces.OptionQuote optionQuote) {
        return pass(optionQuote.getAsk());
    }

    @Override
    public String getPillText() {
        return (type == Type.BULLISH
                ? "Bullish price target: "
                : "Bearish price target: ")
                + Util.formatDollarRange(limitLo, limitHi);
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof StrikeFilter &&
                ((StrikeFilter) filter).type == this.type;
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



}
