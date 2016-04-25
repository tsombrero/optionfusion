package com.optionfusion.cache;

import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.callback.JobManagerCallbackAdapter;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.jobqueue.GetOptionChainJob;
import com.optionfusion.model.provider.Interfaces;

import java.util.ArrayList;
import java.util.HashMap;

public class OptionChainProvider {

    private final String TAG = OptionChainProvider.class.getSimpleName();
    private final JobManager jobManager;
    private final StockQuoteProvider stockQuoteProvider;
    HashMap<String, Interfaces.OptionChain> chains = new HashMap<>();
    HashMap<String, ArrayList<ClientInterfaces.Callback<Interfaces.OptionChain>>> callbacks = new HashMap<>();

    public OptionChainProvider(JobManager jobManager, StockQuoteProvider stockQuoteProvider) {
        this.jobManager = jobManager;
        this.stockQuoteProvider = stockQuoteProvider;
        jobManager.addCallback(new JobManagerCallbackAdapter(){
            @Override
            public void onDone(Job job) {
                if (job instanceof GetOptionChainJob) {
                    handleResult(((GetOptionChainJob)job).getSymbol(), ((GetOptionChainJob)job).getResult());
                }
            }
        });
    }

    public synchronized Interfaces.OptionChain get(String symbol) {
        Interfaces.StockQuote quote = stockQuoteProvider.get(symbol);

        Interfaces.OptionChain ret = chains.get(symbol);

        if (quote != null && ret != null && quote.getQuoteTimestamp() > ret.getQuoteTimestamp()) {
            chains.remove(symbol);
            ret = null;
        }

        if (ret != null)
            return ret;

        jobManager.addJobInBackground(new GetOptionChainJob(symbol));
        return null;
    }

    public synchronized void get(String symbol, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {

        addCallback(symbol, callback);

        Interfaces.OptionChain ret = get(symbol);

        if (ret != null) {
            handleResult(symbol, ret);
        }
    }

    private synchronized void addCallback(String symbol, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        if (callbacks.get(symbol) == null) {
            callbacks.put(symbol, new ArrayList<ClientInterfaces.Callback<Interfaces.OptionChain>>());
        }
        callbacks.get(symbol).add(callback);
    }

    private synchronized void callCallback(String symbol, Interfaces.OptionChain chain, ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        if (symbol == null || callback == null)
            return;

        try {
            callback.call(chain);
        } catch (Throwable t) {
            Log.e(TAG, "Error notifying of new option chain");
        } finally {
            if (callbacks.get(symbol) != null) {
                callbacks.get(symbol).remove(callback);
            }
        }
    }

    public synchronized void put(Interfaces.OptionChain result) {
        chains.put(result.getSymbol(), result);
    }

    private void handleResult(String symbol, Interfaces.OptionChain chain) {
        chains.put(symbol, chain);

        if (!callbacks.containsKey(symbol))
            return;

        ArrayList<ClientInterfaces.Callback<Interfaces.OptionChain>> listeners = new ArrayList<>(callbacks.get(symbol));
        for (ClientInterfaces.Callback<Interfaces.OptionChain> listener : listeners) {
            callCallback(symbol, chain, listener);
        }
    }
}
