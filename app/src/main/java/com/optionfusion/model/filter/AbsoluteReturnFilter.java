package com.optionfusion.model.filter;

import android.os.Parcel;

import com.optionfusion.db.Schema;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.util.Util;

import java.util.ArrayList;

public class AbsoluteReturnFilter extends Filter {

    private final Double absoluteReturn;

    public AbsoluteReturnFilter(Double absoluteReturn) {
        this.absoluteReturn = absoluteReturn;
    }

    public AbsoluteReturnFilter(Parcel parcel) {
        this(parcel.readDouble());
    }

    @Override
    public void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs) {
        selections.add(" ( " + Schema.VerticalSpreads.MAX_GAIN_ABSOLUTE + " >= " + absoluteReturn + " ) ");
    }

    @Override
    public boolean pass(VerticalSpread spread) {
        return spread.getMaxReturn() >= absoluteReturn;
    }

    @Override
    public boolean pass(Interfaces.OptionDate optionDate) {
        return true;
    }

    @Override
    public boolean pass(Interfaces.OptionQuote optionQuote) {
        return true;
    }

    @Override
    public String getPillText() {
        return String.format("Potential Gain: %s", Util.formatDollars(absoluteReturn, 10));
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof AbsoluteReturnFilter;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.ABSOLUTE_RETURN;
    }

    //Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(FilterType.ABSOLUTE_RETURN.ordinal());
        dest.writeDouble(absoluteReturn);
    }

    public static final Creator<AbsoluteReturnFilter> CREATOR
            = new Creator<AbsoluteReturnFilter>() {
        public AbsoluteReturnFilter createFromParcel(Parcel in) {
            return new AbsoluteReturnFilter(in);
        }

        public AbsoluteReturnFilter[] newArray(int size) {
            return new AbsoluteReturnFilter[size];
        }
    };

    @Override
    public boolean isRedundant(Filter filter) {
        return filter != null && filter instanceof AbsoluteReturnFilter && ((AbsoluteReturnFilter)filter).getValue() == getValue();
    }


    public Double getValue() {
        return absoluteReturn;
    }
}
