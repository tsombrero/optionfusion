package com.mosoft.momomentum.ui.results;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.FilterSet;

import java.util.ArrayList;
import java.util.List;

class ResultsAdapter extends RecyclerView.Adapter<ListViewHolders.BaseViewHolder> {

    private final Context context;
    List<ListItem> items;
    private final FilterChangeListener filterChangeListener;

    public ResultsAdapter(FilterSet filterSet, List<Spread> spreads, Context context, FilterChangeListener filterChangeListener) {
        this.context = context;
        this.filterChangeListener = filterChangeListener;

        update(filterSet, spreads);
    }

    private void update(FilterSet filterSet, List<Spread> spreads) {
        List<ListItem> newList = new ArrayList<>();

        newList.add(new ListItem(filterSet));

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
                return new FilterViewHolder(itemView, context, filterChangeListener);
            case SPREAD_DETAILS:
                return new ListViewHolders.SpreadViewHolder(itemView, context);
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

    static class ListItem {
        Spread spread;
        ListViewHolders.ViewType viewType;
        FilterSet filterSet;
        int layout;
        String labelText;

        ListItem(Spread spread) {
            this.spread = spread;
            layout = R.layout.item_spread_details;
            viewType = ListViewHolders.ViewType.SPREAD_DETAILS;
        }

        ListItem(String labelText) {
            this.labelText = labelText;
            layout = R.layout.item_label;
            viewType = ListViewHolders.ViewType.LABEL;
        }

        public ListItem(FilterSet filterSet) {
            this.filterSet = filterSet;
            layout = R.layout.item_filter_buttons;
            viewType = ListViewHolders.ViewType.FILTER_SET;
        }
    }

    public interface FilterChangeListener {
        public void onChange(FilterSet filterSet);
    }
}
