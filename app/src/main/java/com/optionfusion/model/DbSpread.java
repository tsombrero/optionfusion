package com.optionfusion.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.Batch;
import com.google.gson.Gson;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.VerticalSpreads;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.util.Util;

import org.joda.time.DateTime;

import java.util.concurrent.ConcurrentHashMap;

public class DbSpread implements VerticalSpread, Parcelable {

    ConcurrentHashMap<VerticalSpreads, Object> columnValues = new ConcurrentHashMap<>();

    private static final String TAG = "DbSpread";
    private boolean favorite;

    private DbSpread() {
    }

    public DbSpread(Cursor cursor) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String colName = cursor.getColumnName(i);
            VerticalSpreads col = VerticalSpreads.valueOf(colName);
            if (col == null) {
                Log.d(TAG, "Unknown column name " + col);
                continue;
            }

            switch (col.datatype) {
                case TEXT:
                    columnValues.put(col, cursor.getString(i));
                    break;
                case INTEGER:
                    columnValues.put(col, cursor.getLong(i));
                    break;
                case REAL:
                    columnValues.put(col, cursor.getDouble(i));
            }
        }
    }

    @Override
    public SpreadType getSpreadType() {
        if (isBearSpread()) {
            return isCreditSpread() ? SpreadType.BEAR_CALL : SpreadType.BEAR_PUT;
        } else {
            return isCreditSpread() ? SpreadType.BULL_PUT : SpreadType.BULL_CALL;
        }
    }

    @Override
    public double getAsk() {
        return getDouble(VerticalSpreads.NET_ASK);
    }

    @Override
    public double getBid() {
        return getDouble(VerticalSpreads.NET_BID);
    }

    @Override
    public double getMaxReturn() {
        return getDouble(VerticalSpreads.MAX_GAIN_ABSOLUTE);
    }

    @Override
    public double getMaxValueAtExpiration() {
        return getDouble(VerticalSpreads.MAX_VALUE_AT_EXPIRATION);
    }

    @Override
    public double getPrice_BreakEven() {
        return getDouble(VerticalSpreads.PRICE_AT_BREAK_EVEN);
    }

    @Override
    public double getMaxPercentProfitAtExpiration() {
        return getDouble(VerticalSpreads.MAX_GAIN_PERCENT);
    }

    @Override
    public double getPriceChange_MaxProfit() {
        return getDouble(VerticalSpreads.BUFFER_TO_MAX_GAIN);
    }

    @Override
    public double getPercentChange_MaxProfit() {
        return getDouble(VerticalSpreads.BUFFER_TO_MAX_GAIN_PERCENT);
    }

    @Override
    public DateTime getExpiresDate() {
        return new DateTime(getLong(VerticalSpreads.EXPIRATION));
    }

    @Override
    public double getMaxReturnAnnualized() {
        return getDouble(VerticalSpreads.MAX_GAIN_ANNUALIZED);
    }

    public double getMaxReturnMonthly() {
        return Math.pow(1d + getMaxPercentProfitAtExpiration(), 30d / (double) getDaysToExpiration()) - 1d;
    }

    @Override
    public double getBreakEvenDepth() {
        return getDouble(VerticalSpreads.BUFFER_TO_BREAK_EVEN);
    }

    public double getSellStrike() {
        return getDouble(VerticalSpreads.SELL_STRIKE);
    }

    @Override
    public String getSellSymbol() {
        return getString(VerticalSpreads.SELL_SYMBOL);
    }

    @Override
    public String getBuySymbol() {
        return getString(VerticalSpreads.BUY_SYMBOL);
    }

    @Override
    public Interfaces.OptionQuote getBuy() {
        return new DbOptionQuote(getBuySymbol(), getOptionType(), getBuyStrike(), getExpiresDate());
    }

    @Override
    public Interfaces.OptionQuote getSell() {
        return new DbOptionQuote(getSellSymbol(), getOptionType(), getSellStrike(), getExpiresDate());
    }

    @Override
    public Double getCapitalAtRisk() {
        if (isCreditSpread()) {
            return getMaxValueAtExpiration() + getAsk();
        } else {
            return getAsk();
        }
    }

    @Override
    public boolean isFavorite() {
        return favorite;
    }

    @Override
    public void setIsFavorite(boolean isFavorite) {
        favorite = isFavorite;
    }

    public Interfaces.OptionType getOptionType() {
        return isCall() ? Interfaces.OptionType.CALL : Interfaces.OptionType.PUT;
    }

    public double getBuyStrike() {
        return getDouble(VerticalSpreads.BUY_STRIKE);
    }

    @Override
    public String toString() {
        return columnValues.toString();
    }

    @Override
    public int getDaysToExpiration() {
        return (int) getLong(VerticalSpreads.DAYS_TO_EXPIRATION);
    }

    @Override
    public boolean isInTheMoney_BreakEven() {
        if (isBullSpread()) {
            return getDouble(VerticalSpreads.UNDERLYING_PRICE) > getPrice_BreakEven();
        }
        return getDouble(VerticalSpreads.UNDERLYING_PRICE) < getPrice_BreakEven();
    }

    @Override
    public boolean isInTheMoney_MaxReturn() {
        if (isBullSpread()) {
            return getDouble(VerticalSpreads.UNDERLYING_PRICE) > getPrice_MaxReturn();
        }
        return getDouble(VerticalSpreads.UNDERLYING_PRICE) < getPrice_MaxReturn();
    }

    @Override
    public double getPrice_MaxReturn() {
        return getDouble(VerticalSpreads.PRICE_AT_MAX_GAIN);
    }

    @Override
    public double getPrice_MaxLoss() {
        return getDouble(VerticalSpreads.PRICE_AT_MAX_LOSS);
    }

    @Override
    public boolean isCall() {
        return isBullSpread() != isCreditSpread();
    }

    @Override
    public String getUnderlyingSymbol() {
        return getString(VerticalSpreads.UNDERLYING_SYMBOL);
    }

    @Override
    public boolean isBullSpread() {
        return getBoolean(VerticalSpreads.IS_BULLISH);
    }

    public boolean isCreditSpread() {
        return getBoolean(VerticalSpreads.IS_CREDIT);
    }

    @Override
    public boolean isBearSpread() {
        return !isBullSpread();
    }

    @Override
    public double getWeightedValue() {
        return getDouble(VerticalSpreads.RISK_AVERSION_SCORE);
    }

    private double getDouble(VerticalSpreads col) {
        try {
            return (double) columnValues.get(col);
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col) + " to double");
            return 0D;
        }
    }

    private String getString(VerticalSpreads col) {
        try {
            return (String) columnValues.get(col);
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col) + " to String");
            return null;
        }
    }

    private boolean getBoolean(VerticalSpreads col) {
        return getLong(col) != 0;
    }

    private long getLong(VerticalSpreads col) {
        try {
            return (long) columnValues.get(col);
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col) + " to long");
            return 0L;
        }
    }


    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(columnValues);
    }

    public static final Parcelable.Creator<VerticalSpread> CREATOR = new SpreadCreator();

    public void unFavorite(SQLiteDatabase db) {
        String selection = Schema.Favorites.BUY_SYMBOL + "=? AND " + Schema.Favorites.SELL_SYMBOL + "=?";
        String[] selectionArgs = new String[]{getBuySymbol(), getSellSymbol()};
        db.delete(Schema.Favorites.getTableName(), selection, selectionArgs);
    }

    public void saveAsFavorite(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            ContentValues cvInsert = new Schema.ContentValueBuilder()
                    .put(Schema.Favorites.BUY_QUANTITY, 1)
                    .put(Schema.Favorites.SELL_QUANTITY, 1)
                    .put(Schema.Favorites.BUY_SYMBOL, getBuySymbol())
                    .put(Schema.Favorites.SELL_SYMBOL, getSellSymbol())
                    .put(Schema.Favorites.CURRENT_ASK, getAsk())
                    .put(Schema.Favorites.CURRENT_BID, getBid())
                    .put(Schema.Favorites.PRICE_ACQUIRED, getAsk() + getBid() / 2)
                    .put(Schema.Favorites.TIMESTAMP_ACQUIRED, System.currentTimeMillis())
                    .put(Schema.Favorites.TIMESTAMP_EXPIRATION, getExpiresDate().getMillis())
                    .build();

            db.insertWithOnConflict(Schema.Favorites.getTableName(), null, cvInsert, SQLiteDatabase.CONFLICT_IGNORE);

            ContentValues cvUpdate = new Schema.ContentValueBuilder()
                    .put(Schema.Favorites.CURRENT_ASK, getAsk())
                    .put(Schema.Favorites.CURRENT_BID, getBid())
                    .build();

            String selection = Schema.Favorites.BUY_SYMBOL + "=? AND " + Schema.Favorites.SELL_SYMBOL + "=?";
            String[] selectionArgs = new String[]{getBuySymbol(), getSellSymbol()};
            db.update(Schema.Favorites.getTableName(), cvUpdate, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
    }

    public static class SpreadCreator implements Parcelable.Creator<VerticalSpread> {
        public VerticalSpread createFromParcel(Parcel in) {
            return new DbSpread();
        }

        public DbSpread[] newArray(int size) {
            return new DbSpread[size];
        }

        private OptionFusionApplication.Provider readProvider(Parcel in) {
            return OptionFusionApplication.Provider.values()[in.readInt()];
        }
    }

    public String getDescriptionNoExp() {
        return String.format("%s %.2f/%.2f", getSpreadType().toString(), getBuyStrike(), getSellStrike());
    }

    public String getDescription() {
        return String.format("%s %.2f/%.2f %s", getSpreadType().toString(), getBuyStrike(), getSellStrike(), Util.getFormattedOptionDate(getExpiresDate()));
    }

    public static class DbOptionQuote implements Interfaces.OptionQuote {

        private final String symbol;
        Interfaces.OptionType optionType;
        double strike;
        DateTime expiration;

        // this sucks, query the db
        public DbOptionQuote(String symbol, Interfaces.OptionType type, double strike, DateTime expiration) {
            this.symbol = symbol;
            optionType = type;
            this.strike = strike;
            this.expiration = expiration;
        }

        @Override
        public String getOptionSymbol() {
            return symbol;
        }

        @Override
        public String getDescription() {
            return symbol;
        }

        @Override
        public double getImpliedVolatility() {
            return 0;
        }

        @Override
        public double getTheoreticalValue() {
            return 0;
        }

        @Override
        public double getStrike() {
            return strike;
        }

        @Override
        public int getDaysUntilExpiration() {
            return Util.getDaysFromNow(expiration);
        }

        @Override
        public double getMultiplier() {
            return 0;
        }

        @Override
        public double getAsk() {
            return 0;
        }

        @Override
        public double getBid() {
            return 0;
        }

        @Override
        public int getBidSize() {
            return 1;
        }

        @Override
        public int getAskSize() {
            return 1;
        }

        @Override
        public boolean hasBid() {
            return true;
        }

        @Override
        public boolean hasAsk() {
            return true;
        }

        @Override
        public Interfaces.OptionType getOptionType() {
            return optionType;
        }

        @Override
        public DateTime getExpiration() {
            return expiration;
        }

        @Override
        public boolean isStandard() {
            return true;
        }

        @Override
        public String toJson(Gson gson) {
            return null;
        }

        @Override
        public OptionFusionApplication.Provider getProvider() {
            return OptionFusionApplication.Provider.OPTION_FUSION_BACKEND;
        }
    }
}
