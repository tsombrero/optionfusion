package com.mosoft.momomentum.util;

import android.app.ActionBar;
import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.joda.time.DateTimeConstants;
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

    public static final String TAG = "Mo";

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
            return String.format("%dx", (int) pct);
        if (pct > 1d)
            return String.format("%d%%", (int) (100d * pct));
        if (pct > 0.1d)
            return String.format("%.1f%%", 100d * pct);
        if (pct > 0d)
            return String.format("%.2f%%", 100d * pct);
        if (pct < 100d)
            return String.format("-%dx", (int) pct);
        if (pct < -1d)
            return String.format("-%d%%", (int) (-100d * pct));
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

    public static String getFormattedOptionDate(LocalDate date) {
        synchronized (calendar) {
            LocalDate nearDateLimit = LocalDate.now().plusMonths(6);

            if (date.isBefore(nearDateLimit)) {
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

    public static int getDaysFromNow(LocalDate date) {
        return Days.daysBetween(new LocalDate(), date).getDays();
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
            return "Above " + Util.formatDollarsCompact(limitLo);

        if (limitLo == 0d)
            return "Below " + Util.formatDollarsCompact(limitHi);

        return Util.formatDollarsCompact(limitLo) + " - " + Util.formatDollarsCompact(limitHi);
    }

    public static String formatDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "All";
        }
        if (startDate == null) {
            return "Before " + getFormattedOptionDate(endDate);
        }
        if (endDate == null) {
            return "After " + getFormattedOptionDate(startDate);
        }
        return getFormattedOptionDate(startDate) + " - " + getFormattedOptionDate(endDate);
    }

    public static LocalDate roundToNearestFriday(LocalDate localDate) {
        LocalDate t1 = localDate.withDayOfWeek(DateTimeConstants.FRIDAY);
        if (t1.isBefore(localDate.minusDays(3)))       return t1.plusWeeks(1);
        else if (t1.isAfter(localDate.plusDays(3)))    return t1.minusWeeks(1);
        else return t1;
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

    public static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() == null)
            return;
        
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static void goFullscreen(Activity activity) {
        View decorView = activity.getWindow().getDecorView();

        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null)
           actionBar.hide();
    }
}
