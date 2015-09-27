package com.mosoft.momomentum.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class Util {

    public static final String TAG="Mo";

    private static final DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

    public static long getCentsFromCurrencyString(String str) {
        try {
            String parts[] = str.split(".");
            long ret = Long.valueOf(parts[0]) * 100;
            if (parts.length > 1) {
                ret += Long.valueOf(parts[1]);
            }
            return ret;
        } catch (Exception e) {
            return -1;
        }
    }

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

//        Log.d(TAG, String.format("%2.1f%% compounded monthly for %.2f months is %2.1f%%", periodicGrowth * 100d, periodCount, (ret * 100d) - 100d));

        return ret - 1d;
    }

    public static String formatPercent(Double pct) {
        if (pct > 1d)
            return String.format("%d%%", (int)(100d * pct));
        if (pct > 0.1d)
            return String.format("%.1f%%", 100d * pct);
        if (pct > 0d)
            return String.format("%.2f%%", 100d * pct);
        if (pct < -1d)
            return String.format("(%d%%)", (int)(-100d * pct));
        if (pct < -0.1d)
            return String.format("(%.2f%%)", -100d * pct);
        if (pct < 0d)
            return String.format("(%.1f%%)", -100d * pct);

        return "0";
    }

    public static String getFormattedOptionDate(Date date) {
        return dateFormat.format(date);
    }
}
