package com.optionfusion.ui.results;

import android.app.Activity;
import android.support.annotation.CallSuper;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.module.OptionFusionApplication;

import javax.inject.Inject;

import butterknife.ButterKnife;

public class FilterViewHolder extends ResultsListViewHolders.BaseViewHolder {

    private static final String TAG = FilterViewHolder.class.getSimpleName();
    protected final Activity activity;

    FilterSet filterSet;

    @Inject
    OptionChainProvider optionChainProvider;

    public FilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
        this.activity = activity;
        OptionFusionApplication.from(context).getComponent().inject(this);
        ButterKnife.bind(itemView);
    }

    @CallSuper
    public void bind(final ResultsAdapter.ListItem item) {
        if (item instanceof ResultsAdapter.FilterSetListItem)
            this.filterSet = ((ResultsAdapter.FilterSetListItem) item).filterSet;
        if (item instanceof ResultsAdapter.FilterLayoutListItem)
            this.filterSet = ((ResultsAdapter.FilterLayoutListItem) item).filterSet;
    }

    public FilterSet getFilterSet() {
        return filterSet;
    }

    protected void addFilter(Filter filter) {
        filterSet.addFilter(filter);
        resultsListener.onChange(filterSet);
    }

    protected void removeFilterMatching(Filter matchfilter) {
        if (filterSet.removeFilterMatching(matchfilter)) {
            resultsListener.onChange(filterSet);
        }
    }

    protected void animateTextViewActive(TextView textView, boolean active) {
        float targetScale = active ? 1.2f : 1f;

        textView.animate()
                .scaleX(targetScale).scaleY(targetScale)
                .setDuration(100)
                .translationY((1f - targetScale) * (float) textView.getHeight())
                .start();
    }

}
