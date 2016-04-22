package com.optionfusion.jobqueue;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public abstract class BaseApiJob extends Job {

    @Inject
    EventBus bus;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    OptionChainProvider optionChainProvider;

    @Inject
    ClientInterfaces.AccountClient accountClient;

    @Inject
    ClientInterfaces.StockQuoteClient stockQuoteClient;

    @Inject
    ClientInterfaces.OptionChainClient optionChainClient;

    @Inject
    Context context;

    private static final String TAG = "JobQueue";

    protected static String GROUP_ID_WATCHLIST = "watchlist_group";

    public BaseApiJob(Params params) {
        super(params);
    }

    @Override
    public void onAdded() {
    }

    @Override
    protected void onCancel(int cancelReason) {
    }

    @CallSuper
    @Override
    public void onRun() throws Throwable {
        Log.i(TAG, "Running job " + this);
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
        Log.w(TAG, "Job Failed: " + this, throwable);
        return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }
}
