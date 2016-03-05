package com.optionfusion.backend.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;

public class Util {

    public static String[] getEnumNamesArray(Enum<?>[] values) {
        ArrayList<String> list = new ArrayList<>();
        for (Enum column : values) {
            list.add(column.name());
        }
        return list.toArray(new String[]{});
    }

    public static DateTime getEodDateTime() {
        return DateTime.now(DateTimeZone.forID("America/New_York"))
                .withTime(16, 0, 0, 0);
    }

    public static DateTime getEodDateTime(DateTime dateTime) {
        return dateTime
                .withTime(16, 0, 0, 0)
                .withZoneRetainFields(DateTimeZone.forID("America/New_York"));
    }
}
