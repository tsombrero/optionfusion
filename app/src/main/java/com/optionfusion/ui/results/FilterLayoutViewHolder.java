package com.optionfusion.ui.results;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;

import javax.inject.Inject;

import butterknife.ButterKnife;

public abstract class FilterLayoutViewHolder extends FilterViewHolder {

    public FilterLayoutViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    public void bind(final ResultsAdapter.ListItem item) {
        this.filterSet = ((ResultsAdapter.FilterLayoutListItem) item).filterSet;
    }

    abstract Filter.FilterType getFilterType();
}
