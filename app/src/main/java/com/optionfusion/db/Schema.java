package com.optionfusion.db;

import android.content.ContentValues;

import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.util.Util;

import java.util.ArrayList;
import java.util.List;

import static com.optionfusion.db.Schema.DataType.INTEGER;
import static com.optionfusion.db.Schema.DataType.REAL;
import static com.optionfusion.db.Schema.DataType.TEXT;
import static com.optionfusion.db.Schema.DbConstraint.DEFAULT_0;
import static com.optionfusion.db.Schema.DbConstraint.NOT_NULL;
import static com.optionfusion.db.Schema.DbConstraint.PRIMARY_KEY;

public class Schema {

    public final static int SCHEMA_VERSION = 8;
    public static final String DB_NAME = "optionfusion.db";

    enum DbConstraint {
        NOT_NULL, DEFAULT_0, PRIMARY_KEY, UNIQUE, NONE
    }

    public enum DataType {
        TEXT, INTEGER, REAL
    }

    public interface DbColumn {
        String name();

        DataType dataType();

        DbConstraint[] constraints();
    }

    public static class ContentValueBuilder {

        ContentValues cv = new ContentValues();

        public ContentValueBuilder put(DbColumn key, String value) {
            cv.put(key.name(), value);
            return this;
        }

        public ContentValueBuilder put(DbColumn key, int value) {
            cv.put(key.name(), value);
            return this;
        }

        public ContentValueBuilder put(DbColumn key, double value) {
            cv.put(key.name(), value);
            return this;
        }

        public ContentValues build() {
            return cv;
        }
    }

    public enum StockQuotes implements DbColumn {
        SYMBOL(TEXT, PRIMARY_KEY),
        LAST(REAL),
        CHANGE(REAL),
        VOLUME(INTEGER),
        CHANGE_PERCENT(REAL),
        TIMESTAMP(INTEGER),
        TIMESTAMP_UPDATED(INTEGER),
        DESCRIPTION(TEXT);

        private final DataType datatype;
        private final DbConstraint[] constraints;

        StockQuotes(DataType datatype, DbConstraint... constraints) {
            this.datatype = datatype;
            this.constraints = resolveConstraints(datatype, constraints);
        }

        public DataType dataType() {
            return datatype;
        }

        public DbConstraint[] constraints() {
            return constraints;
        }

        public static String getTableName() { return StockQuotes.class.getSimpleName(); }
    }

    public enum Options implements DbColumn {
        SYMBOL(TEXT, PRIMARY_KEY),
        UNDERLYING_SYMBOL(TEXT),
        UNDERLYING_PRICE(REAL),
        BID(REAL),
        ASK(REAL),
        STRIKE(REAL),
        EXPIRATION(INTEGER),
        DAYS_TO_EXPIRATION(INTEGER),
        IV(REAL),
        THEORETICAL_VALUE(REAL),
        OPTION_TYPE(TEXT),
        OPEN_INTEREST(INTEGER),

        TIMESTAMP_QUOTE(INTEGER),
        TIMESTAMP_FETCH(INTEGER);

        private final DataType datatype;
        private final DbConstraint[] constraints;

        Options(DataType datatype, DbConstraint... constraints) {
            this.datatype = datatype;
            this.constraints = resolveConstraints(datatype, constraints);
        }

        public DataType dataType() {
            return datatype;
        }

        public DbConstraint[] constraints() {
            return constraints;
        }

        public static String getKey(String underlying, long expiration, Interfaces.OptionType optionType, Double strike) {
            return getKey(underlying, expiration, optionType == Interfaces.OptionType.CALL, strike);
        }

        public static String getKey(String underlying, long expiration, OptionChainProto.OptionQuote.OptionType optionType, Double strike) {
            return getKey(underlying, expiration, optionType == OptionChainProto.OptionQuote.OptionType.CALL, strike);
        }

        private static String getKey(String underlying, long expiration, boolean isCall, Double strike) {
            return String.format("%s%d%s%s", underlying, expiration, isCall ? "C" : "P", Util.formatDollars(strike));
        }

        public static String getTableName() {
            return Options.class.getSimpleName();
        }
    }


    public enum VerticalSpreads implements DbColumn {
        UNDERLYING_SYMBOL(TEXT),
        UNDERLYING_PRICE(REAL),
        BUY_SYMBOL(TEXT),
        SELL_SYMBOL(TEXT),
        BUY_STRIKE(REAL),
        SELL_STRIKE(REAL),
        SPREAD_TYPE(INTEGER),
        IS_BULLISH(INTEGER),
        IS_CREDIT(INTEGER),
        NET_ASK(REAL),
        NET_BID(REAL),
        EXPIRATION(INTEGER),
        DAYS_TO_EXPIRATION(INTEGER),

        // calculations
        PRICE_AT_MAX_GAIN(REAL),
        PRICE_AT_MAX_LOSS(REAL),
        MAX_GAIN_ABSOLUTE(REAL),
        MAX_GAIN_PERCENT(REAL),
//        MAX_GAIN_MONTHLY(REAL),
        MAX_GAIN_ANNUALIZED(REAL),
        MAX_VALUE_AT_EXPIRATION(REAL),
        CAPITAL_AT_RISK(REAL),
        CAPITAL_AT_RISK_PERCENT(REAL),

        // Used when sorting by risk; offers a smallish profitability factor
        RISK_AVERSION_SCORE(REAL),

        // how much the price can change before cutting into max profit
        BUFFER_TO_MAX_GAIN(REAL),
        BUFFER_TO_MAX_GAIN_PERCENT(REAL),

        // how much the price can change before negative profit
        PRICE_AT_BREAK_EVEN(REAL),
        BUFFER_TO_BREAK_EVEN(REAL),
        BUFFER_TO_BREAK_EVEN_PERCENT(REAL),

        TIMESTAMP_QUOTE(INTEGER),
        TIMESTAMP_FETCH(INTEGER);

        public final DataType datatype;
        public final DbConstraint[] constraints;

        VerticalSpreads(DataType datatype, DbConstraint... constraints) {
            this.datatype = datatype;
            this.constraints = resolveConstraints(datatype, constraints);
        }

        public DataType dataType() {
            return datatype;
        }

        public DbConstraint[] constraints() {
            return constraints;
        }

        public static String getTableName() {
            return VerticalSpreads.class.getSimpleName();
        }
    }

    private static DbConstraint[] resolveConstraints(DataType datatype, DbConstraint[] constraints) {
        if (constraints.length == 0) {
            switch (datatype) {
                case INTEGER:
                case REAL:
                    return new DbConstraint[]{NOT_NULL, DEFAULT_0};
                default:
                    return new DbConstraint[]{};
            }
        } else if (constraints.length == 1 && constraints[0] == DbConstraint.NONE) {
            return new DbConstraint[]{};
        } else {
            return constraints;
        }
    }

    public static List<String> getColumnNames(DbColumn[] columns) {
        List<String> names = new ArrayList<>();
        for (DbColumn e : columns) {
            names.add(e.name());
        }
        return names;
    }

    public static List<String> getEnumNames(Enum<?>[] enums) {
        List<String> names = new ArrayList<>();
        for (Enum<?> e : enums) {
            names.add(e.name());
        }
        return names;
    }

    public static String[] getProjection(DbColumn ... columns) {
        return getColumnNames(columns).toArray(new String[columns.length]);
    }
}
