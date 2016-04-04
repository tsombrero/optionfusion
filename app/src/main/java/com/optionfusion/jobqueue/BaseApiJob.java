package com.optionfusion.jobqueue;

import android.database.sqlite.SQLiteDatabase;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

public abstract class BaseApiJob extends Job {

    @Inject
    SQLiteDatabase db;

    @Inject
    EventBus bus;

    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition completed = lock.newCondition();

    public BaseApiJob(Params params) {
        super(params);
    }

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
