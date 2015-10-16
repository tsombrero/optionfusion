package com.mosoft.momomentum.util;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class Util {

    public static final String TAG="Mo";

    private static final DateFormat dateFormat = new SimpleDateFormat("MM/d/yy");
    private static final DateFormat dateFormatFar = new SimpleDateFormat("MMM yyyy");
    private static final DateFormat dateFormatNear = new SimpleDateFormat("d MMM");
    private static final GregorianCalendar calendar = new GregorianCalendar();

    public static String formatDollars(Double val) {
        if (val >= 0) {
            return String.format("$%.2f", val);
        }

        return String.format("($%.2f)", val);
    }

    //TODO there's a formula for this!
    public static double compoundGrowth(final double periodicGrowth, final double periodCount) {
        double ret = 1d;
        double periodRemaining = periodCount;

        while (periodRemaining > 1d) {
            ret *= (1d + periodicGrowth);
            periodRemaining -= 1d;
        }

        ret *= (1d + (periodicGrowth * periodRemaining));

        return ret - 1d;
    }

    public static String formatPercent(double pct) {
        if (pct > 100d)
            return String.format("%dx", (int)pct);
        if (pct > 1d)
            return String.format("%d%%", (int)(100d * pct));
        if (pct > 0.1d)
            return String.format("%.1f%%", 100d * pct);
        if (pct > 0d)
            return String.format("%.2f%%", 100d * pct);
        if (pct < 100d)
            return String.format("-%dx", (int)pct);
        if (pct < -1d)
            return String.format("-%d%%", (int)(-100d * pct));
        if (pct < -0.1d)
            return String.format("-%.2f%%", -100d * pct);
        if (pct < 0d)
            return String.format("-%.1f%%", -100d * pct);

        return "0";
    }

    public static String formatPercentCompact(double pct) {
        return formatPercent(pct)
                .replace(".00%", "%")
                .replace(".0%", "%");
    }

    public static String getFormattedOptionDate(Date date) {
        synchronized (calendar) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.add(Calendar.MONTH, 6);

            if (calendar.getTime().after(date)) {
                return dateFormatNear.format(date);
            }
            return dateFormatFar.format(date);
        }
    }

    public static String getFormattedOptionDateCompact(Date date) {
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    public static int getDaysFromNow(Date date) {
        return Days.daysBetween(new LocalDate(), new LocalDate(date.getTime())).getDays();
    }

    public static String formatDollarsCompact(Double strike) {
        return formatDollars(strike).replace(".00", "");
    }

    public static String formatDollarRange(double limitLo, double limitHi) {
        if (limitHi == limitLo && (limitLo == Double.MAX_VALUE || limitLo == 0d))
            return "None";

        if (limitLo == 0d && limitHi == Double.MAX_VALUE)
            return "All";

        if (limitLo > 0d)
            limitLo = Math.round(limitLo);

        if (limitHi < Double.MAX_VALUE)
            limitHi = Math.round(limitHi);

        if (limitHi == Double.MAX_VALUE)
            return Util.formatDollarsCompact(limitLo) + " or higher";

        if (limitLo == 0d)
            return "Lower than " + Util.formatDollarsCompact(limitHi);

        return Util.formatDollarsCompact(limitLo) + " - " + Util.formatDollarsCompact(limitHi);
    }

    public static String formatDateRange(Date startDate, Date endDate) {
        if (startDate == null && endDate == null) {
            return "All";
        }
        if (startDate == null) {
            return "Before " + getFormattedOptionDate(endDate);
        }
        if (endDate == null) {
            return getFormattedOptionDate(startDate) + " or later";
        }
        return getFormattedOptionDate(startDate) + " - " + getFormattedOptionDate(endDate);
    }

    // sometimes there are too many strike prices, limit ticks
    private static List<Double> limitStrikeTicks(List<Double> strikePrices) {
        double targetTicks = 20;
        if (strikePrices.size() <= targetTicks) {
            return strikePrices;
        }

        double interval = (strikePrices.get(strikePrices.size() - 1) - strikePrices.get(0)) / targetTicks;

        double intervals[] = new double[]{0.01d, 0.05d, 0.10d, .25d, 0.5d, 1d, 2d, 2.50d, 5d, 10d, 20d, 50d, 100d, 250d};

        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] > interval) {
                interval = intervals[i - 1];
                break;
            }
        }

        double start = Math.round(strikePrices.get(0) * 100d) / 100d;

        ArrayList<Double> ret = new ArrayList<>();

        for (double strike = start; strike <= strikePrices.get(strikePrices.size() - 1); strike += interval) {
            ret.add(strike);
        }

        return ret;
    }

}
