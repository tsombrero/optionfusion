package com.optionfusion.jobqueue;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.ClientInterfaces;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

public abstract class BaseApiJob extends Job {

    @Inject
    EventBus bus;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    ClientInterfaces.AccountClient accountClient;


    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition completed = lock.newCondition();
    protected final Condition gotToken = lock.newCondition();
    public GoogleSignInResult signinResult;

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
