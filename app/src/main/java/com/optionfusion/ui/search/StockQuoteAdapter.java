package com.optionfusion.ui.search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.optionfusion.R;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.events.WatchListUpdatedEvent;
import com.optionfusion.jobqueue.GetWatchlistJob;
import com.optionfusion.jobqueue.SetWatchlistJob;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StockQuoteAdapter extends RecyclerView.Adapter<SharedViewHolders.StockQuoteViewHolder> {

    private static final String TAG = "StockQuoteAdapter";

    private SearchFragment searchFragment;
    private final Context context;
    private ArrayList<Interfaces.StockQuote> stockQuoteList = new ArrayList<>();
    private boolean isUpdating;

    public StockQuoteAdapter(SearchFragment searchFragment) {
        this(searchFragment, Collections.EMPTY_LIST);
    }

    public StockQuoteAdapter(SearchFragment searchFragment, List<String> symbols) {
        this.searchFragment = searchFragment;
        context = searchFragment.getActivity();
        stockQuoteList = searchFragment.stockQuoteProvider.getFromSymbols(symbols);
        searchFragment.bus.register(this);
    }

    public void removeItem(int index) {
        notifyItemRemoved(index);
        stockQuoteList.remove(index);
        searchFragment.jobManager.addJobInBackground(SetWatchlistJob.fromStockQuoteList(stockQuoteList));
    }

    public synchronized void onManualRefresh() {
        if (isUpdating)
            return;

        if (searchFragment.accountClient.getAccountUser() == null)
            return;

        isUpdating = true;

        searchFragment.jobManager.addJobInBackground(new GetWatchlistJob());
        searchFragment.recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (isUpdating) {
                        Toast.makeText(context, "Failed refreshing stock quotes", Toast.LENGTH_SHORT);
                        searchFragment.swipeRefreshLayout.setRefreshing(false);
                        isUpdating = false;
                    }
                }
            }
        }, 10000);
        notifyDataSetChanged();
    }

    private boolean hasStaleStockQuotes(List<Interfaces.StockQuote> stockQuoteList) {
        for (Interfaces.StockQuote stockQuote : stockQuoteList) {
            if (stockQuote.getProvider() == OptionFusionApplication.Provider.DUMMY)
                return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StockQuotesUpdatedEvent event) {

        searchFragment.showProgress(false);

        if (event.getStockQuoteList() == null) {
            return;
        }

        synchronized (stockQuoteList) {
            for (Interfaces.StockQuote oldStockQuote : new ArrayList<>(stockQuoteList)) {
                stockQuoteList.remove(oldStockQuote);
                stockQuoteList.add(Util.bestOf(oldStockQuote, event.getStockQuote(oldStockQuote.getSymbol())));
            }
            Collections.sort(stockQuoteList, Interfaces.StockQuote.COMPARATOR);
        }

        notifyDataSetChanged();
        searchFragment.swipeRefreshLayout.setRefreshing(false);
        isUpdating = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(WatchListUpdatedEvent event) {
        searchFragment.showProgress(false);
        searchFragment.swipeRefreshLayout.setRefreshing(false);

        setStockQuoteList(event.getWatchList());
    }

    @Override
    public SharedViewHolders.StockQuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_quote, parent, false);
        return new SharedViewHolders.StockQuoteViewHolder(v, searchFragment.viewConfig, searchFragment, searchFragment.bus);
    }

    @Override
    public void onBindViewHolder(SharedViewHolders.StockQuoteViewHolder holder, int position) {
        searchFragment.stockQuoteProvider.getFromSymbol(stockQuoteList.get(position).getSymbol());
        holder.bind(stockQuoteList.get(position));
    }

    @Override
    public int getItemCount() {
        return stockQuoteList == null ? 0 : stockQuoteList.size();
    }

    public ArrayList<Interfaces.StockQuote> getStockQuoteList() {
        return stockQuoteList;
    }

    public void setStockQuoteList(List<Interfaces.StockQuote> newStockQuoteList) {
        if (newStockQuoteList == null)
            return;

        if (newStockQuoteList == null || newStockQuoteList.isEmpty()) {
            if (this.stockQuoteList != null)
                return;
        }

        Collections.sort(newStockQuoteList, Interfaces.StockQuote.COMPARATOR);

        boolean needsFullUpdate = false;

        if (newStockQuoteList.size() == this.stockQuoteList.size() + 1) {
            // Figure out which one we added
            for (int i = 0; i < this.stockQuoteList.size(); i++) {
                if (!this.stockQuoteList.get(i).getSymbol().equals(newStockQuoteList.get(i).getSymbol())) {
                    this.stockQuoteList = new ArrayList<>(newStockQuoteList);
                    notifyItemInserted(i);
                    break;
                }
            }
        } else if (newStockQuoteList.size() == this.stockQuoteList.size()) {
            // Update the ones that changed
            for (int i = 0; i < this.stockQuoteList.size(); i++) {
                if (!this.stockQuoteList.get(i).getSymbol().equals(newStockQuoteList.get(i).getSymbol())) {
                    needsFullUpdate = true;
                    break;
                } else {
                    if (stockQuoteList.get(i).getQuoteTimestamp() < newStockQuoteList.get(i).getQuoteTimestamp()) {
                        this.stockQuoteList.set(i, newStockQuoteList.get(i));
                        notifyItemChanged(i);
                    }
                }
            }
        } else {
            needsFullUpdate = true;
        }

        if (needsFullUpdate) {
            this.stockQuoteList = new ArrayList<>(newStockQuoteList);
            Log.i(TAG, "TACO notifyDataSetChanged");
            notifyDataSetChanged();
        }
    }
}
