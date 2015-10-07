package com.mosoft.momomentum.ui.results;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.SpreadFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ResultsAdapter extends RecyclerView.Adapter<ListViewHolders.BaseViewHolder> {

    List<ListItem> items;
    private Resources resources;
    private final FilterClickListener filterClickListener;

    public ResultsAdapter(SpreadFilter spreadFilter, List<Spread> spreads, Resources resources, FilterClickListener filterClickListener) {
        this.resources = resources;
        this.filterClickListener = filterClickListener;

        update(spreadFilter, spreads);
    }

    private void update(SpreadFilter spreadFilter, List<Spread> spreads) {
        List<ListItem> newList = new ArrayList<>();

        Map<SpreadFilter.Filter, Double> activeFilters = spreadFilter.getActiveFilters();

        if (!activeFilters.isEmpty()) {
            newList.add(new ListItem("Active filters:"));
        }

        for (SpreadFilter.Filter filter : activeFilters.keySet()) {
            newList.add(new ListItem(filter, activeFilters.get(filter)));
        }

        if (activeFilters.isEmpty()) {
            newList.add(new ListItem("Add filter:"));
        } else if (!spreadFilter.getInactiveFilters().isEmpty()){
            newList.add(new ListItem("Refine results:"));
        }

        for (SpreadFilter.Filter filter : spreadFilter.getInactiveFilters()) {
            newList.add(new ListItem(filter));
        }

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
                return new ListViewHolders.LabelViewHolder(itemView, resources, filterClickListener);
            case ACTIVE_FILTER:
                return new ListViewHolders.ActiveFilterViewHolder(itemView, resources, filterClickListener);
            case INACTIVE_FILTER:
                return new ListViewHolders.FilterViewHolder(itemView, resources, filterClickListener);
            case SPREAD_DETAILS:
                return new ListViewHolders.SpreadViewHolder(itemView, resources);
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
        SpreadFilter.Filter filter;
        double filterValue;
        ListViewHolders.ViewType viewType;
        int layout;
        String labelText;

        ListItem(Spread spread) {
            this.spread = spread;
            layout = R.layout.item_spread_details;
            viewType = ListViewHolders.ViewType.SPREAD_DETAILS;
        }

        ListItem(SpreadFilter.Filter filter) {
            this.filter = filter;
            layout = R.layout.item_filter_inactive;
            viewType = ListViewHolders.ViewType.INACTIVE_FILTER;
        }

        ListItem(SpreadFilter.Filter filter, double filterValue) {
            this.filter = filter;
            this.filterValue = filterValue;
            layout = R.layout.item_filter_active;
            viewType = ListViewHolders.ViewType.ACTIVE_FILTER;
        }

        ListItem(String labelText) {
            this.labelText = labelText;
            layout = R.layout.item_label;
            viewType = ListViewHolders.ViewType.LABEL;
        }
    }

    public interface FilterClickListener {
        public void onAddFilter(SpreadFilter.Filter filter, String val);
        public void onClickRemoveFilter(SpreadFilter.Filter filter);
        public void onClickChangeFilter(SpreadFilter.Filter filter);
    }
}
