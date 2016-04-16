package com.optionfusion.jobqueue;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.StockQuotes;
import com.optionfusion.events.StockQuotesUpdatedEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.optionfusion.model.provider.Interfaces.StockQuote;

public class GetStockQuotesJob extends BaseApiJob {

    private static final String TAG = "GetStockQuotesJob";

    private List<StockQuote> result;
    private List<String> symbols = new ArrayList<>();

    @Override
    protected int getRetryLimit() {
        return 5;
    }

    private GetStockQuotesJob(Collection<String> symbols) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST)
                .singleInstanceBy(GetStockQuotesJob.class.getSimpleName() + TextUtils.join(",", symbols)));

        Log.i(TAG, "New StockQuoteJob : " + GetStockQuotesJob.class.getSimpleName() + TextUtils.join(",", symbols));
        this.symbols.addAll(symbols);
    }

    public static GetStockQuotesJob fromSymbols(Collection<String> symbols) {
        return new GetStockQuotesJob(symbols);
    }

    public static GetStockQuotesJob fromEquities(List<Equity> equities) {
        List<String> symbols = new ArrayList<>();
        for (Equity equity : equities) {
            symbols.add(equity.getSymbol());
        }
        return new GetStockQuotesJob(symbols);
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        result = stockQuoteClient.getStockQuotes(symbols, null);

        if (result == null)
            throw new UnknownError("GetStockQuoteJob Failed");

        Log.d(TAG, "Got " + result.size() + " quotes from server");

        bus.post(new StockQuotesUpdatedEvent(result));
        writeResultsToDb(result);
    }

    private void writeResultsToDb(List<StockQuote> result) {
//        org.sqlite.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
//        db.beginTransaction();
//        try {
//            for (StockQuote stockQuote : result) {
//                db.insertWithOnConflict(StockQuotes.getTableName(), "",
//                        getStockQuoteContentValues(stockQuote), SQLiteDatabase.CONFLICT_REPLACE);
//            }
//            db.setTransactionSuccessful();
//        } finally {
//            db.endTransaction();
//        }
    }

    private ContentValues getStockQuoteContentValues(StockQuote stockQuote) {
        ContentValues cv = new Schema.ContentValueBuilder()
                .put(StockQuotes.CHANGE, stockQuote.getChange())
                .put(StockQuotes.CHANGE_PERCENT, stockQuote.getChangePercent())
                .put(StockQuotes.LAST, stockQuote.getLast())
                .put(StockQuotes.DESCRIPTION, stockQuote.getDescription())
                .put(StockQuotes.TIMESTAMP_UPDATED, stockQuote.getLastUpdatedLocalTimestamp())
                .put(StockQuotes.TIMESTAMP, stockQuote.getQuoteTimestamp())
                .put(StockQuotes.SYMBOL, stockQuote.getSymbol())
                .build();

        return cv;
    }

}
