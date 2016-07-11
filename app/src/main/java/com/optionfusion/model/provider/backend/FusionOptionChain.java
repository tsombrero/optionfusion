package com.optionfusion.model.provider.backend;

import android.database.Cursor;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.optionfusion.common.protobuf.OptionChainProto;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class FusionOptionChain implements Interfaces.OptionChain {

    List<Double> strikePriceTicks;
    List<DateTime> expirationDates = new ArrayList<>();

    DbHelper dbHelper;

    private final long lastUpdatedLocalTimestamp = System.currentTimeMillis();
    private long quoteDataTimestamp;
    private String symbol;
    private double underlyingPrice;

    public FusionOptionChain(OptionChainProto.OptionChain protoChain, DbHelper dbHelper) {
        this.dbHelper = dbHelper;
        for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
            expirationDates.add(new DateTime(dateChain.getExpiration()));
        }
        symbol = protoChain.getSymbol();
        underlyingPrice = protoChain.getUnderlyingPrice();
        quoteDataTimestamp = protoChain.getTimestamp();
        getStrikePrices();
    }

    public FusionOptionChain(String symbol, DbHelper dbHelper) {
        this.symbol = symbol;
        this.dbHelper = dbHelper;

    }

    @Override
    public double getUnderlyingPrice() {
        return underlyingPrice;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public List<DateTime> getExpirationDates() {
        return expirationDates;
    }

    @Override
    public List<Double> getStrikePrices() {
        if (strikePriceTicks == null) {
            String sql = "select min(strike), max(strike) " +
                    " FROM " + Schema.Options.TABLE_NAME +
                    " WHERE " + Schema.Options.UNDERLYING_SYMBOL + " =?";

            Cursor c = dbHelper.getReadableDatabase()
                    .rawQuery(sql, new String[]{symbol});

            if (c.moveToFirst()) {
                Double minStrike = c.getDouble(0);
                Double maxStrike = c.getDouble(1);
                strikePriceTicks = com.optionfusion.util.Util.getStrikeTicks(minStrike, maxStrike);
            }
        }
        return strikePriceTicks;
    }

    @Override
    public List<VerticalSpread> getAllSpreads(FilterSet filterSet) {
        //FIXME DB call on the main thread
        String orderBy = Schema.VerticalSpreads.MAX_GAIN_ANNUALIZED + " DESC";
        ArrayList<String> selections = new ArrayList<>();
        ArrayList<String> selectionArgs = new ArrayList<>();

        selections.add("(" + Schema.VerticalSpreads.UNDERLYING_SYMBOL + "=?)");
        selectionArgs.add(symbol);

        for (Filter filter : filterSet.getFilters()) {
            filter.addDbSelection(selections, selectionArgs);
            if (filter.getFilterType() == Filter.FilterType.ROI) {
                orderBy = Schema.VerticalSpreads.RISK_AVERSION_SCORE + " DESC";
            }
        }

        String selection = TextUtils.join(" AND ", selections);

        Cursor c = dbHelper.getReadableDatabase()
                .query(Schema.VerticalSpreads.TABLE_NAME,
                        Schema.getProjection(Schema.VerticalSpreads.values()),
                        selection, selectionArgs.toArray(new String[]{}),
                        null, null, orderBy + " LIMIT 50");

        List<VerticalSpread> ret = new ArrayList<>();
        while (c != null && c.moveToNext()) {
            ret.add(new DbSpread(c));
        }
        return ret;

    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.OPTION_FUSION_BACKEND;
    }

    @Override
    public long getLastUpdatedLocalTimestamp() {
        return lastUpdatedLocalTimestamp;
    }

    @Override
    public long getQuoteTimestamp() {
        return quoteDataTimestamp;
    }

    @Override
    public boolean succeeded() {
        return !strikePriceTicks.isEmpty() && !expirationDates.isEmpty();
    }

    @Override
    public String getError() {
        return null;
    }
}
