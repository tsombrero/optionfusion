package com.optionfusion.model.filter;

import android.os.Parcel;
import android.os.Parcelable;

import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.ui.widgets.rangebar.RangeBar;
import com.optionfusion.util.Util;

import org.joda.time.DateTime;

import java.util.ArrayList;

import static com.optionfusion.db.Schema.VerticalSpreads.DAYS_TO_EXPIRATION;

public class TimeFilter extends Filter implements RangeBar.RangeBarDataProvider {
    private DateTime maxExpDate;
    private DateTime minExpDate;

    private int minDaysToExp = 0;
    private int maxDaysToExp = Integer.MAX_VALUE;

    public transient static final TimeFilter EMPTY_FILTER = new TimeFilter(new DateTime(0), new DateTime(0));

    public TimeFilter(DateTime minExpDate, DateTime maxExpDate) {
        if (maxExpDate == null && minExpDate == null)
            throw new IllegalArgumentException("No max or min date provided");

        if (maxExpDate != null && minExpDate != null && maxExpDate.isBefore(minExpDate)) {
            throw new IllegalArgumentException("Max date is before min date");
        }

        this.maxExpDate = maxExpDate;
        this.minExpDate = minExpDate;

        if (maxExpDate != null)
            maxDaysToExp = Util.getDaysFromNow(maxExpDate);

        if (minExpDate != null)
            minDaysToExp = Util.getDaysFromNow(minExpDate);
    }

    public TimeFilter(Parcel parcel) {
        this(new DateTime(parcel.readLong()), new DateTime(parcel.readLong()));
    }

    @Override
    public void addDbSelection(ArrayList<String> selections, ArrayList<String> selectionArgs) {
        selections.add("( " + DAYS_TO_EXPIRATION + " > ? AND " + DAYS_TO_EXPIRATION + " < ? )");
        selectionArgs.add(String.valueOf(minDaysToExp));
        selectionArgs.add(String.valueOf(maxDaysToExp));
    }

    @Override
    public boolean pass(VerticalSpread spread) {
        return pass(spread.getDaysToExpiration());
    }

    @Override
    public boolean pass(Interfaces.OptionDate optionDate) {
        return pass(optionDate.getDaysToExpiration());
    }

    @Override
    public boolean pass(Interfaces.OptionQuote optionQuote) {
        return pass(optionQuote.getDaysUntilExpiration());
    }

    @Override
    public String getPillText() {
        if (minDaysToExp > 0 && maxDaysToExp < Integer.MAX_VALUE) {
            return String.format("Expires between %s and %s", Util.getFormattedOptionDate(minExpDate), Util.getFormattedOptionDate(maxExpDate));
        }
        if (minDaysToExp > 0) {
            return String.format("Expires after %s", Util.getFormattedOptionDate(minExpDate));
        }
        if (maxDaysToExp < Integer.MAX_VALUE) {
            return String.format("Expires before %s", Util.getFormattedOptionDate(maxExpDate));
        }
        return "<invalid>";
    }

    @Override
    public boolean shouldReplace(Filter filter) {
        return filter instanceof TimeFilter;
    }

    @Override
    public FilterType getFilterType() {
        return FilterType.TIME;
    }

    private boolean pass(int daysToExp) {
        return daysToExp >= minDaysToExp && daysToExp <= maxDaysToExp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(FilterType.TIME.ordinal());
        dest.writeLong(minExpDate.toDate().getTime());
        dest.writeLong(maxExpDate.toDate().getTime());
    }

    public DateTime getMinExpDate() {
        return minExpDate;
    }

    public DateTime getMaxExpDate() {
        return maxExpDate;
    }

    @Override
    public Object getLeftValue() {
        return getMinExpDate();
    }

    @Override
    public Object getRightValue() {
        return getMaxExpDate();
    }

    public static final Parcelable.Creator<TimeFilter> CREATOR
            = new Parcelable.Creator<TimeFilter>() {
        public TimeFilter createFromParcel(Parcel in) {
            return new TimeFilter(in);
        }

        public TimeFilter[] newArray(int size) {
            return new TimeFilter[size];
        }
    };
}
