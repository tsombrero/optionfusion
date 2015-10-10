package com.mosoft.momomentum.model.filter;

import android.os.Parcel;
import android.os.Parcelable;

import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

abstract public class Filter implements Parcelable {
    public abstract boolean pass(Spread spread);

    public abstract boolean pass(OptionChain.OptionDate optionDate);

    public abstract boolean pass(OptionChain.OptionQuote optionQuote);

    public abstract String getPillText();

    public abstract boolean shouldReplace(Filter filter);


    public static final Parcelable.Creator<Filter> CREATOR
            = new Parcelable.Creator<Filter>() {
        public Filter createFromParcel(Parcel in) {
            FilterType type = FilterType.values()[in.readInt()];
            switch (type) {
                case ROI:
                    return new RoiFilter(in);
                case TIME:
                    return new TimeFilter(in);
            }
            return null;
        }

        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };

    public enum FilterType {
        ROI,
        TIME;
    }
}
