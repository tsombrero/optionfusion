package com.mosoft.momomentum.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.mosoft.momomentum.model.filter.Filter;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FilterSet implements Parcelable {

    List<Filter> filters = new ArrayList<>();

    Comparator<Spread> comparator = new Spread.DescendingBreakEvenDepthComparator();

    public FilterSet() {
    }

    public boolean pass(Spread spread) {
        if (spread == null)
            return false;

        for (Filter filter : filters) {
            if (!filter.pass(spread))
                return false;
        }
        return true;
    }

    public boolean pass(OptionChain.OptionDate optionDate) {
        if (optionDate == null)
            return false;

        for (Filter filter : filters) {
            if (!filter.pass(optionDate))
                return false;
        }
        return true;
    }

    public boolean pass(OptionChain.OptionQuote optionQuote) {
        if (optionQuote == null)
            return false;

        for (Filter filter : filters) {
            if (!filter.pass(optionQuote))
                return false;
        }
        return true;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public int getCount() {
        return filters.size();
    }

    public Filter getFilter(int i) {
        if (i < 0 || i >= getCount())
            return null;

        return filters.get(i);
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(filters);
    }

    // not needed unless we decide to do an array of filtersets
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
        filters = in.createTypedArrayList(Filter.CREATOR);
    }

    public void addFilter(Filter filter) {
        if (filter == null)
            return;

        for (int i = filters.size() - 1; i>=0; i--) {
            if (filter.shouldReplace(filters.get(i)))
                filters.remove(i);
        }
        filters.add(filter);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void removeFilter(Filter filter) {
        filters.remove(filter);
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    public void removeFilterMatching(Filter match) {
        for (Filter filter : filters) {
            if (match.shouldReplace(filter)) {
                removeFilter(filter);
                break;
            }
        }
    }

    public void setComparator(Comparator<Spread> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Spread> getComparator() {
        return comparator;
    }
}
