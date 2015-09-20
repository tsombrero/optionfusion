package com.mosoft.momomentum.util;

public class Util {
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
}
