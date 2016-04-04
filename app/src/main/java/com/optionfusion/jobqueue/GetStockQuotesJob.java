package com.optionfusion.jobqueue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.client.ClientInterfaces.Callback;
import com.optionfusion.client.ClientInterfaces.StockQuoteClient;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.StockQuotes;
import com.optionfusion.events.StockQuotesUpdatedEvent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.optionfusion.model.provider.Interfaces.StockQuote;

public class GetStockQuotesJob extends BaseApiJob {

    @Inject
    StockQuoteClient stockQuoteClient;

    @Inject
    Context context;

    private List<StockQuote> result;
    private final List<String> symbols;

    public GetStockQuotesJob(List<String> symbols) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .singleInstanceBy(GetStockQuotesJob.class.getSimpleName() + TextUtils.join(",", symbols)));

        this.symbols = symbols;
    }

    public GetStockQuotesJob(Context context, String symbol) {
        this(Collections.singletonList(symbol));
    }

    @Override
    public void onRun() throws Throwable {

        lock.lock();
        try {
            stockQuoteClient.getStockQuotes(symbols, new Callback<List<StockQuote>>() {
                @Override
                public void call(List<StockQuote> type) {
                    GetStockQuotesJob.this.result = result;
                    writeResultsToDb(result);
                    completed.signalAll();
                }

                @Override
                public void onError(int status, String message) {
                    Log.w("tag", "Failed: " + status + " " + message);
                    Toast.makeText(context, "Failed getting stock quote", Toast.LENGTH_SHORT);
                    completed.signalAll();
                }
            });
        } finally {
            lock.unlock();
        }

        completed.await(15, TimeUnit.SECONDS);
        if (result == null)
            throw new UnknownError("GetStockQuoteJob Failed");
        else
            bus.post(new StockQuotesUpdatedEvent(result));
    }

    private void writeResultsToDb(List<StockQuote> result) {
        db.beginTransaction();
        try {
            for (StockQuote stockQuote : result) {
                db.insertWithOnConflict(StockQuotes.getTableName(), "",
                        getStockQuoteContentValues(stockQuote), SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
