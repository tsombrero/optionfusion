package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.appyvet.rangebar.RangeBar;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.util.Util;

import java.util.Date;

public class TimeFilter extends Filter implements RangeBar.RangeBarDataProvider {
    private Date maxExpDate;
    private Date minExpDate;

    private int minDaysToExp = 0;
    private int maxDaysToExp = Integer.MAX_VALUE;

    public static final TimeFilter EMPTY_FILTER = new TimeFilter(new Date(0), new Date(0));

    public TimeFilter(Date minExpDate, Date maxExpDate) {
        if (maxExpDate == null && minExpDate == null)
            throw new IllegalArgumentException("No max or min date provided");

        if (maxExpDate != null && minExpDate != null && maxExpDate.getTime() < minExpDate.getTime()) {
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
        this(new Date(parcel.readLong()), new Date(parcel.readLong()));
    }

    @Override
    public boolean pass(Spread spread) {
        return pass(spread.getDaysToExpiration());
    }

    @Override
    public boolean pass(OptionChain.OptionDate optionDate) {
        return pass(optionDate.getDaysToExpiration());
    }

    @Override
    public boolean pass(OptionChain.OptionQuote optionQuote) {
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
        dest.writeLong(minExpDate.getTime());
        dest.writeLong(maxExpDate.getTime());
    }

    public Date getMinExpDate() {
        return minExpDate;
    }

    public Date getMaxExpDate() {
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
}
