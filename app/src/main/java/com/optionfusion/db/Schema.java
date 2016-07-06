package com.optionfusion.db;

import android.content.ContentValues;
import android.text.TextUtils;

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

    public final static int SCHEMA_VERSION = 1;
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

        String tableName();
    }

    public static class ContentValueBuilder {

        ContentValues cv = new ContentValues();

        public ContentValueBuilder put(DbColumn key, String value) {
            cv.put(key.name(), value);
            return this;
        }

        public ContentValueBuilder put(DbColumn key, long value) {
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

        public final static String TABLE_NAME = StockQuotes.class.getSimpleName();

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

        public String tableName() { return TABLE_NAME; }
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

        public final static String TABLE_NAME = Options.class.getSimpleName();

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

        public String tableName() {
            return TABLE_NAME;
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
        TIMESTAMP_FETCH(INTEGER),

        IS_FAVORITE(INTEGER, NOT_NULL, DEFAULT_0);

        public final DataType datatype;
        public final DbConstraint[] constraints;

        public static final String TABLE_NAME = VerticalSpreads.class.getSimpleName();

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

        public String tableName() {
            return TABLE_NAME;
        }
    }

    public enum Favorites implements DbColumn {
        UNDERLYING_SYMBOL(TEXT, NOT_NULL),
        BUY_SYMBOL(TEXT, NOT_NULL),
        SELL_SYMBOL(TEXT, NOT_NULL),
        BUY_QUANTITY(INTEGER),
        SELL_QUANTITY(INTEGER),
        TIMESTAMP_QUOTE(INTEGER),
        TIMESTAMP_ACQUIRED(INTEGER),
        TIMESTAMP_EXPIRATION(INTEGER),
        TIMESTAMP_CLOSED(INTEGER),
        TIMESTAMP_ARCHIVED(INTEGER),
        PRICE_ACQUIRED(REAL),
        CURRENT_BID(REAL),
        CURRENT_ASK(REAL);

        public final DataType datatype;
        public final DbConstraint[] constraints;

        public static final String TABLE_NAME = Favorites.class.getSimpleName();

        Favorites(DataType datatype, DbConstraint... constraints) {
            this.datatype = datatype;
            this.constraints = resolveConstraints(datatype, constraints);
        }

        public DataType dataType() {
            return datatype;
        }

        public DbConstraint[] constraints() {
            return constraints;
        }

        public String tableName() {
            return TABLE_NAME;
        }
    }

    public interface ViewColumn extends DbColumn {
        String getViewSql();

        DbColumn sourceColumn();
    }

    public enum vw_Favorites implements ViewColumn {
        UNDERLYING_SYMBOL(VerticalSpreads.UNDERLYING_SYMBOL),
        UNDERLYING_PRICE(VerticalSpreads.UNDERLYING_PRICE),
        BUY_SYMBOL(VerticalSpreads.BUY_SYMBOL),
        SELL_SYMBOL(VerticalSpreads.SELL_SYMBOL),
        BUY_STRIKE(VerticalSpreads.BUY_STRIKE),
        SELL_STRIKE(VerticalSpreads.SELL_STRIKE),
        SPREAD_TYPE(VerticalSpreads.SPREAD_TYPE),
        IS_BULLISH(VerticalSpreads.IS_BULLISH),
        IS_CREDIT(VerticalSpreads.IS_CREDIT),
        NET_ASK(VerticalSpreads.NET_ASK),
        NET_BID(VerticalSpreads.NET_BID),
        EXPIRATION(VerticalSpreads.EXPIRATION),
        DAYS_TO_EXPIRATION(VerticalSpreads.DAYS_TO_EXPIRATION),

        // calculations
        PRICE_AT_MAX_GAIN(VerticalSpreads.PRICE_AT_MAX_GAIN),
        PRICE_AT_MAX_LOSS(VerticalSpreads.PRICE_AT_MAX_LOSS),
        MAX_GAIN_ABSOLUTE(VerticalSpreads.MAX_GAIN_ABSOLUTE),
        MAX_GAIN_PERCENT(VerticalSpreads.MAX_GAIN_PERCENT),
        MAX_GAIN_ANNUALIZED(VerticalSpreads.MAX_GAIN_ANNUALIZED),
        MAX_VALUE_AT_EXPIRATION(VerticalSpreads.MAX_VALUE_AT_EXPIRATION),
        CAPITAL_AT_RISK(VerticalSpreads.CAPITAL_AT_RISK),
        CAPITAL_AT_RISK_PERCENT(VerticalSpreads.CAPITAL_AT_RISK_PERCENT),

        // Used when sorting by risk; offers a smallish profitability factor
        RISK_AVERSION_SCORE(VerticalSpreads.RISK_AVERSION_SCORE),

        // how much the price can change before cutting into max profit
        BUFFER_TO_MAX_GAIN(VerticalSpreads.BUFFER_TO_MAX_GAIN),
        BUFFER_TO_MAX_GAIN_PERCENT(VerticalSpreads.BUFFER_TO_MAX_GAIN_PERCENT),

        // how much the price can change before negative profit
        PRICE_AT_BREAK_EVEN(VerticalSpreads.PRICE_AT_BREAK_EVEN),
        BUFFER_TO_BREAK_EVEN(VerticalSpreads.BUFFER_TO_BREAK_EVEN),
        BUFFER_TO_BREAK_EVEN_PERCENT(VerticalSpreads.BUFFER_TO_BREAK_EVEN_PERCENT),

        TIMESTAMP_QUOTE(VerticalSpreads.TIMESTAMP_QUOTE),
        TIMESTAMP_FETCH(VerticalSpreads.TIMESTAMP_FETCH),

        // Favorites cols
        BUY_QUANTITY(Favorites.BUY_QUANTITY),
        SELL_QUANTITY(Favorites.SELL_QUANTITY),
        TIMESTAMP_ACQUIRED(Favorites.TIMESTAMP_ACQUIRED),
        TIMESTAMP_EXPIRATION(Favorites.TIMESTAMP_EXPIRATION),
        TIMESTAMP_CLOSED(Favorites.TIMESTAMP_CLOSED),
        TIMESTAMP_ARCHIVED(Favorites.TIMESTAMP_ARCHIVED),
        PRICE_ACQUIRED(Favorites.PRICE_ACQUIRED);

        public DbColumn sourceCol;
        public static final String VIEW_NAME = vw_Favorites.class.getSimpleName();

        vw_Favorites(DbColumn _sourceCol) {
            sourceCol = _sourceCol;
        }

        @Override
        public String getViewSql() {
            return "CREATE VIEW " + VIEW_NAME + " AS SELECT " + getTableColumns(values())
                    + " FROM VerticalSpreads, Favorites "
                    + " WHERE VerticalSpreads.BUY_SYMBOL = Favorites.BUY_SYMBOL AND VerticalSpreads.SELL_SYMBOL = Favorites.SELL_SYMBOL";
        }

        @Override
        public String tableName() {
            return VIEW_NAME;
        }

        @Override
        public DbColumn sourceColumn() {
            return sourceCol;
        }

        @Override
        public DataType dataType() {
            return sourceCol.dataType();
        }

        @Override
        public DbConstraint[] constraints() {
            return new DbConstraint[0];
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


    // get comma delimited table.column list from array of view columns
    static String getTableColumns(ViewColumn[] viewCols) {
        ArrayList<String> columns = new ArrayList<>(viewCols.length);

        for (ViewColumn viewCol : viewCols) {
            if (viewCol != null && viewCol.sourceColumn() != null)
                columns.add(viewCol.sourceColumn().tableName() + "." + viewCol.sourceColumn().name() + " as " + viewCol.name());
        }

        return TextUtils.join(",", columns);
    }

    // get comma delimited list from array of table columns
    static String getTableColumns(DbColumn[] cols) {
        ArrayList<String> columns = new ArrayList<>(cols.length);

        for (DbColumn col : cols) {
            if (col != null)
                columns.add(col.name());
        }

        return TextUtils.join(",", columns);
    }

}
