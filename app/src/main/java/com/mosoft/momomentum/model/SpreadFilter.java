package com.mosoft.momomentum.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpreadFilter implements Parcelable {

    Map<Filter, Double> filters = new HashMap<>();

    public SpreadFilter() {
    }

    public boolean pass(Spread spread) {
        if (spread == null)
            return false;

        // TODO a property map
        if (filters.containsKey(Filter.AnnualizedReturn) && spread.getMaxReturnAnnualized() < filters.get(Filter.AnnualizedReturn))
            return false;

        return true;
    }

    public void putFilter(Filter filter, Double value) {
        filters.put(filter, value);
    }

    public void setMinMonthlyReturn(double pct) {
        filters.put(Filter.AnnualizedReturn, Util.compoundGrowth(pct, 12));
    }

    public int getCount() {
        return Filter.values().length;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(filters.size());
        for (Filter f : filters.keySet()) {
            dest.writeInt(f.ordinal());
            dest.writeDouble(filters.get(f));
        }
    }

    public enum FilterType {
        MAX, MIN
    }

    public Map<Filter, Double> getActiveFilters() {
        return Collections.unmodifiableMap(filters);
    }

    public List<Filter> getInactiveFilters() {
        List <Filter> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(Filter.values()));
        ret.removeAll(filters.keySet());
        return ret;
    }

    public enum Filter {
        MinDaysToExp(R.string.expires_before, FilterType.MIN),
        MaxDaysToExp(R.string.expires_after, FilterType.MAX),
        BreakEvenTolerance(R.string.change_to_break_even, FilterType.MAX),
        MaxReturnTolerance(R.string.change_to_max_return, FilterType.MAX),
        AnnualizedReturn(R.string.max_return_annualized, FilterType.MIN);

        int stringResource;
        FilterType type;

        Filter(int str, FilterType type) {
            stringResource = str;
            this.type = type;
        }

        public int getStringRes() {
            return stringResource;
        }

        public String formatValue(double value) {
            switch (this) {
                case MinDaysToExp:
                case MaxDaysToExp:
                    return Util.getFormattedOptionDate((int)value);
                case BreakEvenTolerance:
                case MaxReturnTolerance:
                case AnnualizedReturn:
                    return Util.formatPercent(value);
            }
            return "<error>";
        }
    }

    public static final Parcelable.Creator<SpreadFilter> CREATOR
            = new Parcelable.Creator<SpreadFilter>() {
        public SpreadFilter createFromParcel(Parcel in) {
            return new SpreadFilter(in);
        }

        public SpreadFilter[] newArray(int size) {
            return new SpreadFilter[size];
        }
    };

    public SpreadFilter(Parcel in) {
        int mapSize = in.readInt();
        for (int i = 0; i < mapSize; i++) {
            filters.put(Filter.values()[in.readInt()], in.readDouble());
        }
    }

}
