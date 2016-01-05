package com.mosoft.optionfusion.model;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.mosoft.optionfusion.model.filter.Filter;
import com.mosoft.optionfusion.model.filter.RoiFilter;
import com.mosoft.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FilterSet implements Parcelable {

    private static final String SORT_MAX_RETURN = "_sort_max_return";
    List<Filter> filters = new ArrayList<>();

    Comparator<Spread> comparator = new Spread.AscendingRiskComparator();
    private int activeButton;

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

    public boolean pass(Interfaces.OptionDate optionDate) {
        if (optionDate == null)
            return false;

        for (Filter filter : filters) {
            if (!filter.pass(optionDate))
                return false;
        }
        return true;
    }

    public boolean pass(Interfaces.OptionQuote optionQuote) {
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

    public void addFilter(Filter filter) {
        if (filter == null)
            return;

        for (int i = filters.size() - 1; i >= 0; i--) {
            if (filter.shouldReplace(filters.get(i)))
                filters.remove(i);
        }
        filters.add(filter);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean removeFilter(Filter filter) {
        return filters.remove(filter);
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    public Filter getFilterMatching(Filter match) {
        for (Filter filter : filters) {
            if (match.shouldReplace(filter)) {
                return filter;
            }
        }
        return null;
    }

    public boolean removeFilterMatching(Filter match) {
        return removeFilter(getFilterMatching(match));
    }

    public void setComparator(Comparator<Spread> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Spread> getComparator() {
        return comparator;
    }

    public int getActiveButton() {
        return activeButton;
    }

    public void setActiveButton(int activeButton) {
        this.activeButton = activeButton;
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

    public static FilterSet loadForSymbol(String symbol, Gson gson, SharedPreferences sharedPreferences) {
        FilterSet filterSet = null;
        for (Filter.FilterType type : Filter.FilterType.values()) {
            Set<String> jsonFilters = sharedPreferences.getStringSet(getPreferencesKey(symbol, type), null);
            if (jsonFilters != null)
                for (String jsonFilter : jsonFilters) {
                    if (filterSet == null)
                        filterSet = new FilterSet();
                    filterSet.addFilter(Filter.fromJson(gson, type, jsonFilter));
                }
        }

        if (filterSet == null) {
            filterSet = new FilterSet();
            filterSet.addFilter(new RoiFilter(.10));
        }

        if (sharedPreferences.getBoolean(symbol + SORT_MAX_RETURN, false)) {
            filterSet.setComparator(new Spread.DescendingMaxReturnComparator());
        }

        return filterSet;
    }

    public void writeToPreferences(String symbol, Gson gson, SharedPreferences sharedPreferences) {
        HashMap<Filter.FilterType, HashSet<String>> stringsByFilterType = new HashMap<>();
        for (Filter filter : getFilters()) {
            if (!stringsByFilterType.containsKey(filter.getFilterType()))
                stringsByFilterType.put(filter.getFilterType(), new HashSet<String>());
            stringsByFilterType.get(filter.getFilterType()).add(filter.toJson(gson));
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        for (Filter.FilterType filterType : Filter.FilterType.values()) {
            editor.putStringSet(getPreferencesKey(symbol, filterType), stringsByFilterType.get(filterType));
        }

        if (comparator instanceof Spread.DescendingMaxReturnComparator) {
            editor.putBoolean(symbol + SORT_MAX_RETURN, true);
        } else {
            editor.remove(symbol + "_sort");
        }

        editor.apply();
    }

    private static String getPreferencesKey(String symbol, Filter.FilterType filterType) {
        return symbol + "_filters_" + filterType.name();
    }
}
