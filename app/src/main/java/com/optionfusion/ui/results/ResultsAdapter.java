package com.optionfusion.ui.results;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.R;
import com.optionfusion.jobqueue.SetFavoriteJob;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.ui.SharedViewHolders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.optionfusion.ui.results.ResultsListViewHolders.BaseViewHolder;
import static com.optionfusion.ui.results.ResultsListViewHolders.SpreadViewHolder;
import static com.optionfusion.ui.results.ResultsListViewHolders.ViewType;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsListViewHolders.BaseViewHolder> implements SharedViewHolders.SpreadFavoriteListener {

    private final String symbol;
    private final Activity activity;
    private final FilterSet filterSet;
    private final JobManager jobManager;
    List<ListItem> items;
    private final ResultsListener resultsListener;
    private int indexOfFilterLayout;
    private int indexOfFirstSpread;
    private int indexOfPillsLayout;
    private int indexOfButtonsLayout;

    private static final String TAG = "ResultsAdapter";

    public ResultsAdapter(FilterSet filterSet, String symbol, List<VerticalSpread> spreads, Activity activity, ResultsListener resultsListener, JobManager jobManager) {
        this.symbol = symbol;
        this.activity = activity;
        this.resultsListener = resultsListener;
        this.filterSet = filterSet;
        this.jobManager = jobManager;

        items = new ArrayList<>();
        items.add(new FilterSetListItem(ViewType.FILTER_BUTTONS, filterSet));
        indexOfButtonsLayout = items.size() - 1;
        items.add(new FilterLayoutListItem(ViewType.filterTypeFromButtonId(filterSet.getActiveButton()), filterSet, symbol));
        indexOfFilterLayout = items.size() - 1;
        items.add(new FilterSetListItem(ViewType.FILTER_PILLS, filterSet));
        indexOfPillsLayout = items.size() - 1;
        indexOfFirstSpread = items.size();

        updateSpreads(spreads);
    }

    public void updateSpreads(List<VerticalSpread> spreads) {

        notifyItemChanged(indexOfPillsLayout);
        notifyItemChanged(indexOfFilterLayout);

        int itemsRemoved = items.size() - indexOfFirstSpread;

        while (items.size() > indexOfFirstSpread)
            items.remove(indexOfFirstSpread);

        if (spreads != null) {
            for (VerticalSpread spread : spreads) {
                items.add(new ListItemSpread(spread));
            }
        }

        notifyItemRangeChanged(indexOfFirstSpread, Math.min(itemsRemoved, spreads.size()));

        if (itemsRemoved > spreads.size()) {
            int previousSize = itemsRemoved + indexOfFirstSpread;
            notifyItemRangeRemoved(items.size(), previousSize - items.size());
        }

        if (itemsRemoved < spreads.size()) {
            int previousSize = itemsRemoved + indexOfFirstSpread;
            notifyItemRangeInserted(previousSize, items.size() - previousSize);
        }

    }

    private Map<ViewType, BaseViewHolder> stableViewHolders = new HashMap<>();

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewType type = ViewType.values()[viewType];

        if (stableViewHolders.get(type) != null)
            return stableViewHolders.get(type);

        View itemView = LayoutInflater.from(parent.getContext()).inflate(type.layout, parent, false);

        BaseViewHolder ret = null;

        switch (type) {
            case NONE:
                ret = new EmptyViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_BUTTONS:
                ret = new FilterButtonsViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_TIME:
                ret = new TimeFilterViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_STRIKE:
                ret = new StrikeFilterViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_ROI:
                ret = new RoiFilterViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_SPREAD_KIND:
                ret = new SpreadKindViewHolder(itemView, activity, resultsListener);
                break;
            case FILTER_PILLS:
                ret = new FilterPillsViewHolder(itemView, activity, resultsListener);
                break;
            case SPREAD_DETAILS:
                ret = new SpreadViewHolder(itemView, activity, resultsListener, this);
                break;
        }

        switch (type) {
            case NONE:
            case FILTER_BUTTONS:
            case FILTER_TIME:
            case FILTER_STRIKE:
            case FILTER_ROI:
            case FILTER_SPREAD_KIND:
                stableViewHolders.put(type, ret);
        }
        return ret;
    }

    @Override
    public void setFavorite(VerticalSpread spread, boolean isFavorite) {
        if (spread instanceof DbSpread) {
            spread.setIsFavorite(isFavorite);
            jobManager.addJobInBackground(new SetFavoriteJob(activity, (DbSpread) spread, isFavorite));
        }
    }

    static class EmptyViewHolder extends BaseViewHolder {
        public EmptyViewHolder(View itemView, Context context, ResultsListener resultsListener) {
            super(itemView, context, resultsListener);
        }

        @Override
        void bind(ListItem item) {

        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType.ordinal();
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    public void onFilterSelected(ViewType type) {
        if (items.get(indexOfFilterLayout).viewType != type) {
            items.remove(indexOfFilterLayout);
            notifyItemRemoved(indexOfFilterLayout);
        }

        items.add(indexOfFilterLayout, new FilterLayoutListItem(type, filterSet, symbol));
        notifyItemInserted(indexOfFilterLayout);

        notifyItemChanged(indexOfButtonsLayout);
    }

    public static class ListItem {
        ViewType viewType;

        public ListItem(ViewType viewType) {
            this.viewType = viewType;
        }
    }

    public static class FilterSetListItem extends ListItem {
        FilterSet filterSet;

        public FilterSetListItem(ViewType type, FilterSet filterSet) {
            super(type);
            this.filterSet = filterSet;
        }
    }

    public static class FilterLayoutListItem extends ListItem {
        final FilterSet filterSet;
        String symbol;

        public FilterLayoutListItem(ViewType type, FilterSet filterSet, String symbol) {
            super(type);
            this.filterSet = filterSet;
            this.symbol = symbol;
        }
    }

    public static class ListItemSpread extends ListItem {
        VerticalSpread spread;

        public ListItemSpread(VerticalSpread spread) {
            super(ViewType.SPREAD_DETAILS);
            this.spread = spread;
        }
    }

    public interface SpreadSelectedListener {
        void onResultSelected(VerticalSpread spread, View headerLayout, View detailsLayout);
    }

    public interface ResultsListener extends SpreadSelectedListener {
        void onChange(FilterSet filterSet);
        void onFilterSelected(int buttonResId);
    }
}
