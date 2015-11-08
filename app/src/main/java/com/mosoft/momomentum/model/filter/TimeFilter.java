package com.mosoft.momomentum.model.filter;

import android.os.Parcel;

import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.ui.widgets.rangebar.RangeBar;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.amtd.AmeritradeOptionChain;
import com.mosoft.momomentum.util.Util;

import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;

import java.util.Date;

public class TimeFilter extends Filter implements RangeBar.RangeBarDataProvider {
    private LocalDate maxExpDate;
    private LocalDate minExpDate;

    private int minDaysToExp = 0;
    private int maxDaysToExp = Integer.MAX_VALUE;

    public static final TimeFilter EMPTY_FILTER = new TimeFilter(new LocalDate(0), new LocalDate(0));

    public TimeFilter(LocalDate minExpDate, LocalDate maxExpDate) {
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
        this(new LocalDate(parcel.readLong()), new LocalDate(parcel.readLong()));
    }

    @Override
    public boolean pass(Spread spread) {
        return pass(spread.getDaysToExpiration());
    }

    @Override
    public boolean pass(AmeritradeOptionChain.AmtdOptionDate optionDate) {
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

    public LocalDate getMinExpDate() {
        return minExpDate;
    }

    public LocalDate getMaxExpDate() {
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
