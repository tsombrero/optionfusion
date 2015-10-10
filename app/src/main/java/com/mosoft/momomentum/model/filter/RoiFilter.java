package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
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
    public boolean pass(OptionChain.OptionDate optionDate) {
        return true;
    }

    @Override
    public boolean pass(OptionChain.OptionQuote optionQuote) {
        return true;
    }

    @Override
    public String getPillText() {
        return String.format("Potential Return at least %s", Util.formatPercentCompact(roi));
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
