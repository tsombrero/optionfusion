package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeOptionChain;
import com.mosoft.momomentum.util.Util;

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
    public boolean pass(AmeritradeOptionChain.AmtdOptionDate optionDate) {
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
