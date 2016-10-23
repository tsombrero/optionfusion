package com.optionfusion.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.gson.Gson;
import com.optionfusion.com.backend.optionFusion.model.JsonMap;
import com.optionfusion.com.backend.optionFusion.model.Position;
import com.optionfusion.common.OptionKey;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.VerticalSpreads;
import com.optionfusion.events.FavoritesUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.optionfusion.common.OptionFusionUtils.roundToNearestFriday;

public class DbSpread implements VerticalSpread, Parcelable {

    private ConcurrentHashMap<String, String> columnValues = new ConcurrentHashMap<>();

    private static final String TAG = "DbSpread";

    private DbSpread() {
    }

    public DbSpread(Cursor cursor) {
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String colName = cursor.getColumnName(i);
            VerticalSpreads col = null;
            try {
                col = VerticalSpreads.valueOf(colName);
            } catch (Exception e) {
            }
            if (col == null) {
                continue;
            }
            putValue(col, cursor, i);
        }
    }

    protected void putValue(Schema.DbColumn col, Cursor cursor, int colIndex) {
        switch (col.dataType()) {
            case TEXT:
            case INTEGER:
                columnValues.put(col.name(), cursor.getString(colIndex));
                break;
            case REAL:
                columnValues.put(col.name(), String.valueOf(cursor.getDouble(colIndex)));
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
        return new DateTime(roundToNearestFriday(getLong(VerticalSpreads.EXPIRATION)));
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
        return getBoolean(VerticalSpreads.IS_FAVORITE);
    }

    @Override
    public void setIsFavorite(boolean isFavorite) {
        columnValues.put(VerticalSpreads.IS_FAVORITE.name(), isFavorite ? "1" : "0");
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

    public long getQuoteTimestamp() {
        return getLong(VerticalSpreads.TIMESTAMP_QUOTE);
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

    protected double getDouble(Schema.DbColumn col) {
        try {
            return Double.valueOf(columnValues.get(col.name()));
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col.name()) + " to double ; " + col);
            return 0D;
        }
    }

    protected String getString(Schema.DbColumn col) {
        try {
            return columnValues.get(col.name());
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col.name()) + " to String ; " + col);
            return null;
        }
    }

    protected boolean getBoolean(Schema.DbColumn col) {
        return getLong(col) != 0;
    }

    protected long getLong(Schema.DbColumn col) {
        try {
            return Long.valueOf(columnValues.get(col.name()));
        } catch (Exception e) {
            Log.w(TAG, "Failed converting " + columnValues.get(col.name()) + " to long ; " + col);
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
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : columnValues.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<VerticalSpread> CREATOR = new SpreadCreator();

    public void unFavorite(SQLiteDatabase db, EventBus bus) {
        setIsFavorite(false);
        db.beginTransaction();
        try {
            // Only one favorite can be in the 'deleted' state (one-level undo)
            clearDeletedFavorites(db);

            String selection = Schema.Favorites.BUY_SYMBOL + "=? AND " + Schema.Favorites.SELL_SYMBOL + "=?";
            String[] selectionArgs = new String[]{getBuySymbol(), getSellSymbol()};
            ContentValues cv = new Schema.ContentValueBuilder().put(Schema.Favorites.IS_DELETED, 1).build();
            db.update(Schema.Favorites.TABLE_NAME, cv, selection, selectionArgs);

            // VerticalSpreads IS_FAVORITE=0
            selection = VerticalSpreads.BUY_SYMBOL + "=? AND " + VerticalSpreads.SELL_SYMBOL + "=?";
            ContentValues cvUpdate = new Schema.ContentValueBuilder()
                    .put(VerticalSpreads.IS_FAVORITE, 0)
                    .build();
            db.update(VerticalSpreads.TABLE_NAME, cvUpdate, selection, selectionArgs);
            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
        bus.post(new FavoritesUpdatedEvent());
    }

    public static void clearDeletedFavorites(SQLiteDatabase db) {
        // Only one favorite can be in the 'deleted' state (one-level undo)
        db.delete(Schema.Favorites.TABLE_NAME, Schema.Favorites.IS_DELETED + "=1", null);
    }

    public void saveAsFavorite(SQLiteDatabase db, EventBus bus) {
        setIsFavorite(true);
        db.beginTransaction();
        try {
            // Favorites table upsert
            ContentValues cvInsert = new Schema.ContentValueBuilder()
                    .put(Schema.Favorites.UNDERLYING_SYMBOL, getUnderlyingSymbol())
                    .put(Schema.Favorites.BUY_QUANTITY, 1)
                    .put(Schema.Favorites.SELL_QUANTITY, 1)
                    .put(Schema.Favorites.BUY_SYMBOL, getBuySymbol())
                    .put(Schema.Favorites.SELL_SYMBOL, getSellSymbol())
                    .put(Schema.Favorites.CURRENT_ASK, getAsk())
                    .put(Schema.Favorites.CURRENT_BID, getBid())
                    .put(Schema.Favorites.PRICE_ACQUIRED, getAsk() + getBid() / 2)
                    .put(Schema.Favorites.TIMESTAMP_ACQUIRED, System.currentTimeMillis())
                    .put(Schema.Favorites.TIMESTAMP_EXPIRATION, getExpiresDate().getMillis())
                    .put(Schema.Favorites.TIMESTAMP_QUOTE, getQuoteTimestamp())
                    .build();

            db.insertWithOnConflict(Schema.Favorites.TABLE_NAME, null, cvInsert, SQLiteDatabase.CONFLICT_IGNORE);

            ContentValues cvUpdate = new Schema.ContentValueBuilder()
                    .put(Schema.Favorites.CURRENT_ASK, getAsk())
                    .put(Schema.Favorites.CURRENT_BID, getBid())
                    .put(Schema.Favorites.IS_DELETED, 0)
                    .build();

            String selection = Schema.Favorites.BUY_SYMBOL + "=? AND " + Schema.Favorites.SELL_SYMBOL + "=?";
            String[] selectionArgs = new String[]{getBuySymbol(), getSellSymbol()};
            db.update(Schema.Favorites.TABLE_NAME, cvUpdate, selection, selectionArgs);

            // VerticalSpreads IS_FAVORITE=1
            selection = VerticalSpreads.BUY_SYMBOL + "=? AND " + VerticalSpreads.SELL_SYMBOL + "=?";
            cvUpdate = new Schema.ContentValueBuilder()
                    .put(VerticalSpreads.IS_FAVORITE, 1)
                    .build();
            db.update(VerticalSpreads.TABLE_NAME, cvUpdate, selection, selectionArgs);

            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
        bus.post(new FavoritesUpdatedEvent());
    }

    public Position getPosition() {
        OptionKey buy = new OptionKey(
                getUnderlyingSymbol(),
                getExpiresDate().getMillis(),
                getOptionType() == Interfaces.OptionType.CALL,
                getBuyStrike());

        OptionKey sell = new OptionKey(
                getUnderlyingSymbol(),
                getExpiresDate().getMillis(),
                getOptionType() == Interfaces.OptionType.CALL,
                getSellStrike());

        Position ret = new Position();

        JsonMap jsonMap = new JsonMap();
        jsonMap.put(buy.toString(), 1);
        jsonMap.put(sell.toString(), -1);
        ret.setLegs(jsonMap);

        return ret;
    }

    public static class SpreadCreator implements Parcelable.Creator<VerticalSpread> {
        public VerticalSpread createFromParcel(Parcel in) {
            DbSpread ret = new DbSpread();
            Bundle bundle = in.readBundle();
            for (String key : bundle.keySet()) {
                ret.columnValues.put(key, bundle.getString(key));
            }
            return ret;
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
