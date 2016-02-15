package com.optionfusion.backend.utils;

import java.util.ArrayList;

public class Util {

    public static String[] getEnumNamesArray(Enum<?>[] values) {
        ArrayList<String> list = new ArrayList<>();
        for (Enum column : values) {
            list.add(column.name());
        }
        return list.toArray(new String[]{});
    }
}
