package com.optionfusion.ui.search;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.optionfusion.R;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.events.WatchListUpdatedEvent;
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
        stockQuoteList = searchFragment.stockQuoteProvider.get(symbols);
        searchFragment.bus.register(this);
        update();
    }

    public void removeItem(int index) {
        notifyItemRemoved(index);
        stockQuoteList.remove(index);
    }

    public synchronized void update() {
        if (isUpdating)
            return;

        if (searchFragment.accountClient.getAccountUser() == null)
            return;

        isUpdating = true;
        stockQuoteList = searchFragment.stockQuoteProvider.getFromEquityList(searchFragment.accountClient.getAccountUser().getWatchList());

        if (hasDummyStockQuotes(stockQuoteList)) {
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
        } else {
            isUpdating = false;
            searchFragment.swipeRefreshLayout.setRefreshing(false);
            searchFragment.showProgress(false);
        }
        notifyDataSetChanged();
    }

    private boolean hasDummyStockQuotes(List<Interfaces.StockQuote> stockQuoteList) {
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
        if (event.getWatchList().isEmpty())
            searchFragment.showProgress(false);
        update();
    }

    @Override
    public SharedViewHolders.StockQuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stock_quote, parent, false);
        return new SharedViewHolders.StockQuoteViewHolder(v, searchFragment.viewConfig, searchFragment, searchFragment.bus);
    }

    @Override
    public void onBindViewHolder(SharedViewHolders.StockQuoteViewHolder holder, int position) {
        holder.bind(stockQuoteList.get(position));
    }

    @Override
    public int getItemCount() {
        return stockQuoteList == null ? 0 : stockQuoteList.size();
    }

    public ArrayList<Interfaces.StockQuote> getStockQuoteList() {
        return stockQuoteList;
    }

    public void setStockQuoteList(ArrayList<Interfaces.StockQuote> stockQuoteList) {
        this.stockQuoteList = stockQuoteList;
        notifyDataSetChanged();
    }
}
