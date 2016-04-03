package com.optionfusion.jobqueue;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.common.eventbus.EventBus;
import com.optionfusion.client.ClientInterfaces.Callback;
import com.optionfusion.client.ClientInterfaces.StockQuoteClient;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.StockQuotes;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.module.OptionFusionApplication;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import static com.optionfusion.model.provider.Interfaces.StockQuote;

public abstract class BaseApiJob extends Job {

    @Inject
    SQLiteDatabase db;

    @Inject
    EventBus bus;

    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition completed = lock.newCondition();

    @Override
    public void onAdded() {
    }

    @Override
    protected void onCancel(int cancelReason) {

    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }
}
