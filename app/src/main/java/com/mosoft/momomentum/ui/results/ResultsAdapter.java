package com.mosoft.momomentum.ui.results;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;

import java.util.ArrayList;
import java.util.List;

public class ResultsAdapter extends RecyclerView.Adapter<ListViewHolders.BaseViewHolder> {

    private final Activity activity;
    List<ListItem> items;
    private final ResultsListener resultsListener;

    public ResultsAdapter(FilterSet filterSet, List<Spread> spreads, Activity activity, ResultsListener resultsListener) {
        this.activity = activity;
        this.resultsListener = resultsListener;

        update(filterSet, spreads);
    }

    public void update(FilterSet filterSet, List<Spread> spreads) {
        if (spreads == null || spreads.isEmpty())
            return;

        List<ListItem> newList = new ArrayList<>();

        newList.add(new ListItem(filterSet, spreads.get(0).getUnderlyingSymbol()));

        if (spreads != null) {
            for (Spread spread : spreads) {
                newList.add(new ListItem(spread));
            }
        }

        items = newList;
        notifyDataSetChanged();
    }

    @Override
    public ListViewHolders.BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListViewHolders.ViewType type = ListViewHolders.ViewType.values()[viewType];

        View itemView = LayoutInflater.from(parent.getContext()).inflate(type.layout, parent, false);

        switch (type) {
            case LABEL:
                return new ListViewHolders.LabelViewHolder(itemView);
            case FILTER_SET:
                return new FilterViewHolder(itemView, activity, resultsListener);
            case SPREAD_DETAILS:
                return new ListViewHolders.SpreadViewHolder(itemView, activity, resultsListener);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType.ordinal();
    }

    @Override
    public void onBindViewHolder(ListViewHolders.BaseViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ListItem {
        String symbol;
        Spread spread;
        ListViewHolders.ViewType viewType;
        FilterSet filterSet;
        int layout;
        String labelText;

        public ListItem(Spread spread) {
            this.spread = spread;
            layout = R.layout.item_spread_details;
            viewType = ListViewHolders.ViewType.SPREAD_DETAILS;
        }

        ListItem(String labelText) {
            this.labelText = labelText;
            layout = R.layout.item_label;
            viewType = ListViewHolders.ViewType.LABEL;
        }

        public ListItem(FilterSet filterSet, String symbol) {
            this.symbol = symbol;
            this.filterSet = filterSet;
            layout = R.layout.item_filter_buttons;
            viewType = ListViewHolders.ViewType.FILTER_SET;
        }
    }

    public interface ResultsListener {
        void onChange(FilterSet filterSet);
        void onResultSelected(Spread spread, View headerLayout, View detailsLayout);
    }
}
