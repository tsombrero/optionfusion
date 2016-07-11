package com.optionfusion.backend.utils;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.googlecode.objectify.Key;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.models.StockQuote;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

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

    public static Key<StockQuote> getStockQuoteKey(String symbol, long millis) {
        return getStockQuoteKey(symbol, new DateTime(millis));
    }

    public static Key<StockQuote> getStockQuoteKey(String symbol, DateTime quoteTime) {
        return Key.create(getEquityKey(symbol), StockQuote.class, getEodDateTime(quoteTime).getMillis());
    }

    public static Key<OptionChain> getOptionChainKey(String symbol, long millis) {
        return getOptionChainKey(symbol, new DateTime(millis));
    }

    public static Key<OptionChain> getOptionChainKey(String symbol, DateTime quoteTime) {
        return Key.create(getEquityKey(symbol), OptionChain.class, getEodDateTime(quoteTime).getMillis());
    }

    public static Key<Equity> getEquityKey(String symbol) {
        return Key.create(Equity.class, symbol);
    }

    public static String getDateTimeStringForFilename(DateTime date) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendMonthOfYear(2)
                .appendDayOfMonth(2)
                .toFormatter();

        return date.toString(dateTimeFormatter);
    }

    public static String getOptionsFileName(DateTime date) {
        return "csv/options_" + getDateTimeStringForFilename(date) + ".csv.zip";
    }

    public static String getStockQuotesFileName(DateTime date) {
        return "csv/stockquotes_" + getDateTimeStringForFilename(date) + ".csv.zip";
    }

    public static Blob getBlobFromStorage(String fileName) {
        Storage storage = StorageOptions.defaultInstance().service();
        BlobId blobId = BlobId.of("optionfusion_com", fileName);
        return storage.get(blobId);
    }
}
