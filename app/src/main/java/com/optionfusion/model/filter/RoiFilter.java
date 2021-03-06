package com.optionfusion.model.filter;

import android.os.Parcel;
import android.os.Parcelable;

import com.optionfusion.db.Schema;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.util.Util;

import java.util.ArrayList;

public class RoiFilter extends Filter {

    private final Double roi;

    public RoiFilter(Double roi) {
        this.roi = roi;
    }

    public RoiFilter(Parcel parcel) {
        this(parcel.readDouble());
    }

    @Override
    public void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs) {
        selections.add(" ( " + Schema.VerticalSpreads.MAX_GAIN_ANNUALIZED + " >= " + roi + " ) ");
    }

    @Override
    public boolean pass(VerticalSpread spread) {
        return spread.getMaxReturnAnnualized() >= roi;
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
        return String.format("Potential Gain: %s / year", Util.formatPercentCompact(roi));
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof RoiFilter;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.ROI;
    }

    //Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean isRedundant(Filter filter) {
        return filter != null && filter instanceof RoiFilter && Util.equals(((RoiFilter)filter).getValue(), getValue());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(FilterType.ROI.ordinal());
        dest.writeDouble(roi);
    }

    public static final Parcelable.Creator<RoiFilter> CREATOR
            = new Parcelable.Creator<RoiFilter>() {
        public RoiFilter createFromParcel(Parcel in) {
            return new RoiFilter(in);
        }

        public RoiFilter[] newArray(int size) {
            return new RoiFilter[size];
        }
    };

    public Double getValue() {
        return roi;
    }
}
