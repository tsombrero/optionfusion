package com.mosoft.momomentum.util;

public class Util {

    public static final String TAG="Mo";

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

    public static String Dollars(Double val) {
        if (val >= 0) {
            return String.format("$%.2f", val);
        }

        return String.format("($%.2f)", val);
    }
}
