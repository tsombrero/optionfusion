package com.optionfusion.model.provider.backend;

import android.database.Cursor;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.Spread;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class FusionOptionChain implements Interfaces.OptionChain {

    List<Double> strikePriceTicks;
    List<DateTime> expirationDates = new ArrayList<>();

    DbHelper dbHelper;

    FusionStockQuote stockQuote;


    public FusionOptionChain(OptionChainProto.OptionChain protoChain) {
        stockQuote = new FusionStockQuote(protoChain.getStockquote());
        getStrikePrices();
        for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
            expirationDates.add(new DateTime(dateChain.getExpiration()));
        }
    }

    @Override
    public Interfaces.StockQuote getUnderlyingStockQuote() {
        return stockQuote;
    }

    @Override
    public List<DateTime> getExpirationDates() {
        if (expirationDates == null) {

        }
        return expirationDates;
    }

    @Override
    public List<Double> getStrikePrices() {
        if (strikePriceTicks == null) {
            String sql = "select min(strike), max(strike) " +
                    " FROM " + Schema.Options.getTableName() +
                    " WHERE " + Schema.Options.UNDERLYING_SYMBOL + " =?";

            Cursor c = dbHelper.getReadableDatabase()
                    .rawQuery(sql, new String[]{getUnderlyingStockQuote().getSymbol()});

            Double minStrike = c.getDouble(0);
            Double maxStrike = c.getDouble(1);
            strikePriceTicks = com.optionfusion.util.Util.getStrikeTicks(minStrike, maxStrike);
        }
        return strikePriceTicks;
    }

    @Override
    public List<Spread> getAllSpreads(FilterSet filterSet) {
        //FIXME DB call on the main thread
        String orderBy = Schema.VerticalSpreads.MAX_RETURN_ANNUALIZED + " DESC";
        ArrayList<String> selections = new ArrayList<>();
        ArrayList<String> selectionArgs = new ArrayList<>();

        selections.add("(" + Schema.VerticalSpreads.UNDERLYING_SUMBOL + "=?)");
        selectionArgs.add(getUnderlyingStockQuote().getSymbol());

        for (Filter filter : filterSet.getFilters()) {
            filter.addDbSelection(selections, selectionArgs);
            if (filter.getFilterType() == Filter.FilterType.ROI) {
                orderBy = Schema.VerticalSpreads.WEIGHTED_RISK + " ASC";
            }
        }

        String selection = " WHERE " + TextUtils.join(" AND ", selections);

        Cursor c = dbHelper.getReadableDatabase()
                .query(Schema.VerticalSpreads.getTableName(),
                        Schema.getProjection(Schema.VerticalSpreads.values()),
                        selection, selectionArgs.toArray(new String[]{}),
                        null, null, orderBy);

        List<Spread> ret = new ArrayList<>();
        while (c != null && c.moveToNext()) {
            ret.add(Spread.newSpread(c));
        }
        return ret;

    }

    @Override
    public String toJson(Gson gson) {
        return null;
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return null;
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return 0;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public String getError() {
        return null;
    }
}
