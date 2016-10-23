package com.optionfusion.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.HashMap;
import java.util.Map;

public class OptionFusionUtils {
    static Map<Long, DateTime> roundingCache = new HashMap<>();

    public static DateTime roundToNearestFriday(DateTime date) {
        DateTime ret = roundingCache.get(date.getMillis());
        if (ret == null) {
            ret = roundToNearestFridayImpl(date.getMillis());
        }
        return ret;
    }

    public static long roundToNearestFriday(long timestamp) {
        DateTime ret = roundingCache.get(timestamp);
        if (ret == null) {
            ret = roundToNearestFridayImpl(timestamp);
            roundingCache.put(timestamp, ret);
        }
        return ret.getMillis();
    }
    private static DateTime roundToNearestFridayImpl(long timestamp) {
        DateTime ret = new DateTime(timestamp).withDayOfWeek(DateTimeConstants.FRIDAY);
        if (ret.isBefore(ret.minusDays(3))) return ret.plusWeeks(1);
        else if (ret.isAfter(ret.plusDays(3))) return ret.minusWeeks(1);
        else return ret;
    }

}
