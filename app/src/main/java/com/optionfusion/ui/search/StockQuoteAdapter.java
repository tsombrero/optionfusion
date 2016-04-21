package com.optionfusion.ui.search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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

class StockQuoteAdapter extends RecyclerView.Adapter<SharedViewHolders.StockQuoteListViewHolder> {

    private static final String TAG = "StockQuoteAdapter";

    private WatchlistFragment watchlistFragment;
    private final Context context;
    private ArrayList<Interfaces.StockQuote> stockQuoteList = new ArrayList<>();
    private boolean isUpdating;

    public StockQuoteAdapter(WatchlistFragment watchlistFragment) {
        this(watchlistFragment, Collections.EMPTY_LIST);
    }

        public StockQuoteAdapter(WatchlistFragment watchlistFragment, List<String> symbols) {
        this.watchlistFragment = watchlistFragment;
        context = watchlistFragment.getActivity();
        stockQuoteList = watchlistFragment.stockQuoteProvider.getFromSymbols(symbols);
        watchlistFragment.bus.register(this);
    }

    public void removeItem(int pos) {
        stockQuoteList.remove(pos2index(pos));
        notifyItemRemoved(pos);
        watchlistFragment.jobManager.addJobInBackground(SetWatchlistJob.fromStockQuoteList(stockQuoteList));
    }

    public synchronized void onManualRefresh() {
        if (isUpdating)
            return;

        if (watchlistFragment.accountClient.getAccountUser() == null)
            return;

        isUpdating = true;

        watchlistFragment.jobManager.addJobInBackground(new GetWatchlistJob());
        watchlistFragment.recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (isUpdating) {
                        Toast.makeText(context, "Failed refreshing stock quotes", Toast.LENGTH_SHORT);
                        watchlistFragment.swipeRefreshLayout.setRefreshing(false);
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

        watchlistFragment.showProgress(false);

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
        watchlistFragment.swipeRefreshLayout.setRefreshing(false);
        isUpdating = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(WatchListUpdatedEvent event) {
        watchlistFragment.showProgress(false);
        watchlistFragment.swipeRefreshLayout.setRefreshing(false);

        setStockQuoteList(event.getWatchList());
    }

    @Override
    public SharedViewHolders.StockQuoteListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (ViewTypes.values()[viewType]) {
            case STOCKQUOTE: {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_quote, parent, false);
                return new SharedViewHolders.StockQuoteViewHolder(v, watchlistFragment.viewConfig, watchlistFragment, watchlistFragment.bus);
            }
            default: {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timestamp_header, parent, false);
                return new SharedViewHolders.MarketDataTimestampHeaderViewHolder(v, watchlistFragment.getFragmentManager(), watchlistFragment.jobManager, watchlistFragment.accountClient);
            }
        }
    }

    enum ViewTypes {
        HEADER, STOCKQUOTE
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return ViewTypes.HEADER.ordinal();
        return ViewTypes.STOCKQUOTE.ordinal();
    }

    @Override
    public void onBindViewHolder(SharedViewHolders.StockQuoteListViewHolder holder, int position) {
        if (stockQuoteList.isEmpty())
            return;

        holder.bind(stockQuoteList.get(pos2index(position)));
    }

    @Override
    public int getItemCount() {
        return stockQuoteList == null || stockQuoteList.isEmpty() ? 0 : stockQuoteList.size() + 1;
    }

    public ArrayList<Interfaces.StockQuote> getStockQuoteList() {
        return stockQuoteList;
    }

    public synchronized void setStockQuoteList(List<Interfaces.StockQuote> newStockQuoteList) {
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
            int i;
            for (i = 0; i < newStockQuoteList.size(); i++) {
                if (i > this.stockQuoteList.size() - 1)
                    break;
                String prevSym = this.stockQuoteList.get(i).getSymbol();
                String newSym = newStockQuoteList.get(i).getSymbol();
                if (!TextUtils.equals(newSym, prevSym)) {
                    break;
                }
            }
            this.stockQuoteList = new ArrayList<>(newStockQuoteList);
            notifyItemInserted(index2pos(i));
        } else if (newStockQuoteList.size() == this.stockQuoteList.size()) {
            // Update the ones that changed
            for (int i = 0; i < this.stockQuoteList.size(); i++) {
                if (!this.stockQuoteList.get(i).getSymbol().equals(newStockQuoteList.get(i).getSymbol())) {
                    needsFullUpdate = true;
                    break;
                } else {
                    if (stockQuoteList.get(i).getQuoteTimestamp() < newStockQuoteList.get(i).getQuoteTimestamp()) {
                        this.stockQuoteList.set(i, newStockQuoteList.get(i));
                        notifyItemChanged(index2pos(i));
                    }
                }
            }
        } else {
            needsFullUpdate = true;
        }

        if (needsFullUpdate) {
            this.stockQuoteList = new ArrayList<>(newStockQuoteList);
            Log.i(TAG, "notifyDataSetChanged");
            notifyDataSetChanged();
        }
    }

    private int pos2index(int pos) {
        return Math.max(0, pos - 1);
    }

    private int index2pos(int index) {
        return index + 1;
    }
}
