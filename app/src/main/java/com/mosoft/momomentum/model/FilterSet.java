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

public class FilterSet implements Parcelable {

    Map<Filter, Double> filters = new HashMap<>();

    public FilterSet() {
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

    public Map<Filter, Double> getActiveFilters() {
        return Collections.unmodifiableMap(filters);
    }

    public List<Filter> getInactiveFilters() {
        List<Filter> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(Filter.values()));
        ret.removeAll(filters.keySet());
        return ret;
    }

    public enum Filter {
        MinDaysToExp(R.string.expires_before),
        MaxDaysToExp(R.string.expires_after),
        BreakEvenTolerance(R.string.change_to_break_even),
        MaxReturnTolerance(R.string.change_to_max_return),
        AnnualizedReturn(R.string.max_return_annualized),
        MinAskPrice(R.string.min_position_cost),
        MaxAskPrice(R.string.max_position_cost);

        int stringResource;

        Filter(int str) {
            stringResource = str;
        }

        public int getStringRes() {
            return stringResource;
        }

        public String formatValue(double value) {
            switch (this) {
                case MinDaysToExp:
                case MaxDaysToExp:
                    return Util.getFormattedOptionDate((int) value);
                case BreakEvenTolerance:
                case MaxReturnTolerance:
                case AnnualizedReturn:
                    return Util.formatPercent(value);
                case MinAskPrice:
                case MaxAskPrice:
                    return Util.formatDollars(value);
            }
            return "<error>";
        }

        public boolean isPercentage() {
            switch (this) {
                case BreakEvenTolerance:
                case MaxReturnTolerance:
                case AnnualizedReturn:
                    return true;
            }
            return false;
        }

        public boolean isCurrency() {
            switch(this) {
                case MinAskPrice:
                case MaxAskPrice:
                    return true;
            }
            return false;
        }
    }



    public static final Parcelable.Creator<FilterSet> CREATOR
            = new Parcelable.Creator<FilterSet>() {
        public FilterSet createFromParcel(Parcel in) {
            return new FilterSet(in);
        }

        public FilterSet[] newArray(int size) {
            return new FilterSet[size];
        }
    };

    public FilterSet(Parcel in) {
        int mapSize = in.readInt();
        for (int i = 0; i < mapSize; i++) {
            filters.put(Filter.values()[in.readInt()], in.readDouble());
        }
    }

}
