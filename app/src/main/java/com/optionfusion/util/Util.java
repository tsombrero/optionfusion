package com.optionfusion.util;

import android.app.Activity;
import android.view.inputmethod.InputMethodManager;

import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class Util {

    public static final String TAG = "Mo";

    private static final DateFormat dateFormat = new SimpleDateFormat("MM/d/yy");
    private static final DateFormat dateFormatFar = new SimpleDateFormat("MMM yyyy");
    private static final DateFormat dateFormatNear = new SimpleDateFormat("d MMM");
    private static final DecimalFormat dollarFormat = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat dollarFormatNoFraction = new DecimalFormat("$#,##0");
    private static final DecimalFormat dollarChangeFormat = new DecimalFormat("#,##0.00");
    private static final DecimalFormat dollarChangeFormatNoFraction = new DecimalFormat("#,##0");
    private static final DecimalFormat percentFormatBig = new DecimalFormat("@@E0x");
    private static final DecimalFormat percentFormat = new DecimalFormat("@@%");

    public static final float MAX_PERCENT_NORMAL_FORMAT = 100F;

    private static final GregorianCalendar calendar = new GregorianCalendar();

    public static String formatDollars(Double val) {
        if (val < 0) {
            return "CR " + dollarFormat.format(Math.abs(val));
        }
        return dollarFormat.format(val);
    }

    public static String formatDollars(Double val, int roundIfAbove) {
        if (Math.abs(val) >= roundIfAbove)
            return dollarFormatNoFraction.format(val);

        return formatDollars(val);
    }

    public static String formatDollarChange(Double val) {
        if (val == null)
            return "$0";
        return dollarChangeFormat.format(val);
    }

    public static String formatDollarChange(Double val, int roundIfAbove) {
        if (val == null)
            return "$0";

        if (Math.abs(val) >= roundIfAbove)
            return dollarChangeFormatNoFraction.format(val);

        return dollarChangeFormat.format(val);
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
        if (Math.abs(pct) < MAX_PERCENT_NORMAL_FORMAT)
            return percentFormat.format(pct);

        return percentFormatBig.format(pct);
    }

    public static String formatPercentCompact(double pct) {
        return formatPercent(pct)
                .replace(".00%", "%")
                .replace(".0%", "%");
    }

    public static String getFormattedOptionDate(DateTime date) {
        synchronized (calendar) {
            DateTime nearDateLimit = DateTime.now().plusMonths(6);

            //TODO don't use dateformat, use joda
            if (date.isBefore(nearDateLimit)) {
                return dateFormatNear.format(date.toDate());
            }
            return dateFormatFar.format(date.toDate());
        }
    }

    public static String getFormattedOptionDateCompact(Date date) {
        synchronized (dateFormat) {
            return dateFormat.format(date);
        }
    }

    public static String getFormattedOptionDateCompact(DateTime expiresDate) {
        synchronized (dateFormat) {
            return dateFormat.format(expiresDate.toDate());
        }
    }

    public static int getDaysFromNow(DateTime date) {
        return Days.daysBetween(DateTime.now(), roundToNearestFriday(date)).getDays();
    }

    public static String formatDollarsCompact(Double val) {
        return formatDollars(val).replace(".00", "");
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

    public static String formatDateRange(DateTime startDate, DateTime endDate) {
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

    public static DateTime roundToNearestFriday(DateTime date) {
        DateTime t1 = date.withDayOfWeek(DateTimeConstants.FRIDAY);
        if (t1.isBefore(date.minusDays(3))) return t1.plusWeeks(1);
        else if (t1.isAfter(date.plusDays(3))) return t1.minusWeeks(1);
        else return t1;
    }

    static final double intervals[] = new double[]{0.01d, 0.05d, 0.10d, .25d, 0.5d, 1d, 2d, 2.50d, 5d, 10d, 20d, 50d, 100d, 250d};

    // sometimes there are too many strike prices, limit ticks
    private static List<Double> limitStrikeTicks(List<Double> strikePrices) {
        double targetTicks = 20;
        if (strikePrices.size() <= targetTicks) {
            return strikePrices;
        }

        double interval = (strikePrices.get(strikePrices.size() - 1) - strikePrices.get(0)) / targetTicks;


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

    public static List<Double> getStrikeTicks(Double lo, Double hi) {
        double chosenInterval = 250d;
        for (Double interval : intervals) {
            if ((hi - lo) / interval > 25)
                continue;

            chosenInterval = interval;
            break;
        }

        lo -= lo % chosenInterval;
        hi += hi % chosenInterval;

        List<Double> ret = new ArrayList<>();
        double i = lo;
        while (i <= hi) {
            ret.add(new Double(i));
            i += chosenInterval;
        }
        return ret;
    }

    public static void hideSoftKeyboard(Activity activity) {
        if (activity.getCurrentFocus() == null)
            return;

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static void showSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static double[] toArray(Collection<Double> doubles) {
        double[] ret = new double[doubles.size()];
        int i = 0;
        for (Double val : doubles) {
            ret[i++] = val;
        }
        return ret;
    }

    public static int roundUp(long msInput, long msUnit, int... multiples) {
        long units = msUnit / msInput;
        for (int multiple : multiples) {
            if (units <= multiple)
                return multiple;
        }
        return multiples[multiples.length - 1];
    }


    public static DateTime getEodDateTime(int year, int month, int day) {
        return new DateTime(year, month, day, 16, 0, DateTimeZone.forID("America/New_York"));
    }

    public static Interfaces.StockQuote bestOf(Interfaces.StockQuote a, Interfaces.StockQuote b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        if (a.getProvider() == OptionFusionApplication.Provider.DUMMY)
            return b;
        if (b.getProvider() == OptionFusionApplication.Provider.DUMMY)
            return a;
        if (a.getQuoteTimestamp() > b.getQuoteTimestamp())
            return a;
        return b;
    }

    public static List<String> getSymbols(List<Interfaces.StockQuote> stockQuotes) {
        if (stockQuotes == null)
            return Collections.EMPTY_LIST;

        List<String> symbols = new ArrayList<>();
        for (Interfaces.StockQuote stockQuote : stockQuotes) {
            symbols.add(stockQuote.getSymbol());
        }
        return symbols;
    }

}
