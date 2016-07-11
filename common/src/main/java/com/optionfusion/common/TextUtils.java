package com.optionfusion.common;

public class TextUtils {
    public static boolean equals(String a, String b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static boolean isEmpty(String a) {
        return a == null || a.length() == 0;
    }

    public static int compare(String a, String b) {
        if (a == b)
            return 0;

        if (a == null)
            return 1;

        if (b == null)
            return -1;

        return a.compareTo(b);
    }
}
