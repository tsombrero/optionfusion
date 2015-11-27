package com.mosoft.optionfusion.model.filter;

import android.os.Parcel;

import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.util.Util;

public class RoiFilter extends Filter {

    private final Double roi;

    public RoiFilter(Double roi) {
        this.roi = roi;
    }

    public RoiFilter(Parcel parcel) {
        this(parcel.readDouble());
    }

    @Override
    public boolean pass(Spread spread) {
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
        return String.format("Potential Return: %s", Util.formatPercentCompact(roi));
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof RoiFilter;
    }

    //Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(FilterType.ROI.ordinal());
        dest.writeDouble(roi);
    }
}
