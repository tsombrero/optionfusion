package com.mosoft.optionfusion.model.filter;

import android.os.Parcel;
import android.os.Parcelable;

import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;

abstract public class Filter implements Parcelable {
    public abstract boolean pass(Spread spread);

    public abstract boolean pass(Interfaces.OptionDate optionDate);

    public abstract boolean pass(Interfaces.OptionQuote optionQuote);

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
        TIME
    }
}
