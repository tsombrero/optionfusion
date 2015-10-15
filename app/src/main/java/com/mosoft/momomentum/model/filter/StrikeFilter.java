package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.util.Util;

public class StrikeFilter extends Filter {

    public enum Type {BULLISH, BEARISH}

    private final double limit;
    private final Type type;

    public StrikeFilter(double limit, Type type) {
        this.limit = limit;
        this.type = type;
    }

    @Override
    public boolean pass(Spread spread) {
        return pass(spread.getPrice_MaxReturn());
    }

    private boolean pass(double limit) {
        if (type == Type.BULLISH) {
            return limit >= this.limit;
        }

        return limit <= this.limit;
    }

    @Override
    public boolean pass(OptionChain.OptionDate optionDate) {
        return true;
    }

    @Override
    public boolean pass(OptionChain.OptionQuote optionQuote) {
        return pass(optionQuote.getAsk());
    }

    @Override
    public String getPillText() {
        if (type == Type.BULLISH && limit == 0) {
            return "No Bullish trades";
        } else if (type == Type.BULLISH) {
            return "Bullish: stock is above " + Util.formatDollars(limit);
        } else if (type == Type.BEARISH && limit == Double.MAX_VALUE) {
            return "No Bearish trades";
        }
        return "Bearish: stock is below "+ Util.formatDollars(limit);
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
        this.limit = parcel.readDouble();
        this.type = Type.values()[parcel.readInt()];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(limit);
        dest.writeInt(type.ordinal());
    }


}
