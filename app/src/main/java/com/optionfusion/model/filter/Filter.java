package com.optionfusion.model.filter;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;

import java.util.ArrayList;

abstract public class Filter implements Parcelable {

    private static final String TAG = "Filter";

    public abstract void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs);

    public abstract boolean pass(VerticalSpread spread);

    public abstract boolean pass(Interfaces.OptionDate optionDate);

    public abstract boolean pass(Interfaces.OptionQuote optionQuote);

    public abstract String getPillText();

    public abstract boolean shouldReplace(Filter filter);

    public abstract FilterType getFilterType();

    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    public static Filter fromJson(Gson gson, FilterType filterType, String json) {
        try {
            switch (filterType) {
                case ROI:
                    return gson.fromJson(json, RoiFilter.class);
                case TIME:
                    return gson.fromJson(json, TimeFilter.class);
                case STRIKE:
                    return gson.fromJson(json, StrikeFilter.class);
            }
        } catch (Throwable t) {
            Log.w(TAG, "fromJson: Failed parsing filter " + json, t);
        }
        return null;
    }

    public static final Parcelable.Creator<Filter> CREATOR
            = new Parcelable.Creator<Filter>() {
        public Filter createFromParcel(Parcel in) {
            FilterType type = FilterType.values()[in.readInt()];
            switch (type) {
                case ROI:
                    return new RoiFilter(in);
                case TIME:
                    return new TimeFilter(in);
                case STRIKE:
                    return new StrikeFilter(in);
            }
            return null;
        }

        public Filter[] newArray(int size) {
            return new Filter[size];
        }
    };

    public enum FilterType {
        ROI,
        TIME,
        STRIKE
    }
}
