package com.optionfusion.model.filter;

import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

import com.optionfusion.db.Schema;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

public class SpreadTypeFilter extends Filter {

    private static final String TAG = "SpreadTypeFilter";
    EnumSet<VerticalSpread.SpreadType> includeTypes = EnumSet.allOf(VerticalSpread.SpreadType.class);

    public SpreadTypeFilter() {
    }

    public SpreadTypeFilter(EnumSet<VerticalSpread.SpreadType> includeTypes) {
        if (includeTypes != null)
            this.includeTypes = includeTypes;
    }

    public SpreadTypeFilter(Parcel parcel) {
        List<String> enumNames = new ArrayList<>();
        parcel.readStringList(enumNames);
        try {
            for (String enumName : enumNames) {
                includeTypes.add(VerticalSpread.SpreadType.valueOf(enumName));
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed inflating filter from parcel");
        }
    }

    @Override
    public void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs) {
        if (includeTypes.isEmpty() || includeTypes.size() == VerticalSpread.SpreadType.values().length)
            return;

        ArrayList<Integer> includedOrdinals = new ArrayList<>();
        Iterator<VerticalSpread.SpreadType> iter = includeTypes.iterator();
        while (iter.hasNext()) {
            includedOrdinals.add(iter.next().ordinal());
        }

        String selection = "( " + Schema.VerticalSpreads.SPREAD_TYPE + " IN (" + TextUtils.join(",", includedOrdinals) + " ) )";
        selections.add(selection);
    }

    @Override
    public boolean pass(VerticalSpread spread) {
        return includeTypes.contains(spread.getSpreadType());
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
        if (includeTypes.size() == 0) {
            return "All Spread Types Excluded!";
        }

        if (includeTypes.size() == 1) {
            return includeTypes.iterator().next() + " spreads only";
        }

        if (includeTypes.size() == 2) {
            Iterator<VerticalSpread.SpreadType> iter = includeTypes.iterator();
            VerticalSpread.SpreadType a = iter.next();
            VerticalSpread.SpreadType b = iter.next();
            if (a.bullish == b.bullish) {
                return (a.bullish ? "Bullish" : "Bearish") + " spreads only";
            }
            if (a.credit == b.credit) {
                return (a.credit ? "Credit" : "Debit") + " spreads only";
            }
            if (a.call == b.call) {
                return (a.call ? "Call" : "Put") + " spreads only";
            }
            return String.format("%s and %s spreads only", a.toString(), b.toString());
        }

        if (includeTypes.size() == 3) {
            for (VerticalSpread.SpreadType spreadType : VerticalSpread.SpreadType.values()) {
                if (!includeTypes.contains(spreadType))
                    return String.format("Exclude %s spreads", spreadType.toString());
            }
        }
        return null;
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof SpreadTypeFilter;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.SPREAD_TYPE;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ArrayList<String> enumNames = new ArrayList<>();

        for (VerticalSpread.SpreadType spreadType : includeTypes) {
            enumNames.add(spreadType.toString());
        }
        dest.writeStringList(enumNames);
    }

    public boolean isIncluded(VerticalSpread.SpreadType spreadType) {
        return includeTypes.contains(spreadType);
    }

    public void includeSpreadType(VerticalSpread.SpreadType spreadType, boolean include) {
        if (include)
            includeTypes.add(spreadType);
        else
            includeTypes.remove(spreadType);
    }
}
