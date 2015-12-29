package com.mosoft.optionfusion.model.provider.goog;

import android.util.Log;

import com.mosoft.optionfusion.model.HistoricalQuote;
import com.squareup.okhttp.ResponseBody;

import java.io.EOFException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import retrofit.Converter;

public class GoogPriceHistoryConverter implements Converter<ResponseBody, GoogPriceHistory> {

    private static final String TAG = "GoogPriceHistoryConverter";

    public static class GoogPriceHistoryConverterFactory extends Converter.Factory {

        @Override
        public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
            if (type == GoogPriceHistory.class)
                return new GoogPriceHistoryConverter();
            return super.fromResponseBody(type, annotations);
        }
    }

    enum HistoryResponseColumn {
        DATE,CLOSE,HIGH,LOW,OPEN,VOLUME;
    }

    @Override
    public GoogPriceHistory convert(ResponseBody value) throws IOException {
        try {
            String line;
            long interval = 0;
            GoogPriceHistory ret = new GoogPriceHistory();
            Map<HistoryResponseColumn, Integer> columnIndex = null;
            long timezoneOffset = 0;
            long runningTimestamp = 0;

            do {
                line = value.source().readUtf8LineStrict();
                if (interval == 0 && line.startsWith("INTERVAL=")) {
                    interval = Long.valueOf(line.split("=")[1]);
                    continue;
                }
                if (columnIndex == null && line.startsWith("COLUMNS=")) {
                    columnIndex = parseColumns(line.split("=")[1]);
                    continue;
                }
                if (interval == 0 && line.startsWith("TIMEZONE_OFFSET=")) {
                    timezoneOffset = Long.valueOf(line.split("=")[1]);
                    continue;
                }

                String[] values = line.split(",");
                HistoricalQuote quote = new HistoricalQuote();
                quote.setClose(getDoubleValue(HistoryResponseColumn.CLOSE, columnIndex, values));
                quote.setHi(getDoubleValue(HistoryResponseColumn.HIGH, columnIndex, values));
                quote.setLo(getDoubleValue(HistoryResponseColumn.LOW, columnIndex, values));
                quote.setOpen(getDoubleValue(HistoryResponseColumn.OPEN, columnIndex, values));
                quote.setVolume(getLongValue(HistoryResponseColumn.VOLUME, columnIndex, values));
                String date = values[columnIndex.get(HistoryResponseColumn.DATE)];
                if (date.startsWith("a")) {
                    runningTimestamp = Long.valueOf(date.substring(1)) + timezoneOffset;
                    quote.setDate(runningTimestamp);
                } else {
                    quote.setDate(runningTimestamp + (Integer.valueOf(date) * interval));
                }
                ret.addQuote(quote);

            } while (line != null);
            return ret;
        } catch (EOFException e) {
            Log.e(TAG, "Truncated history", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed parsing history", e);
        }
        return null;
    }

    private double getDoubleValue(HistoryResponseColumn col, Map<HistoryResponseColumn, Integer> index, String[] values) {
        if (index.containsKey(col))
            return Double.valueOf(values[index.get(col)]);
        return 0d;
    }

    private long getLongValue(HistoryResponseColumn col, Map<HistoryResponseColumn, Integer> index, String[] values) {
        if (index.containsKey(col))
            return Long.valueOf(values[index.get(col)]);
        return 0;
    }

    private Map<HistoryResponseColumn, Integer> parseColumns(String line) {
        Map<HistoryResponseColumn, Integer> ret = new HashMap<>();
        String[] colNames = line.split(",");
        for (int i = 0; i < colNames.length; i++) {
            try {
                HistoryResponseColumn col = HistoryResponseColumn.valueOf(colNames[i]);
                ret.put(col, i);
            } catch (Exception e) {
                Log.w(TAG, "Failed parsing column " + colNames[i]);
            }
        }
        return ret;
    }

}
