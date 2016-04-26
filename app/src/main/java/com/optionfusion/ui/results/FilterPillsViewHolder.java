package com.optionfusion.ui.results;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.filter.Filter;

import butterknife.Bind;
import butterknife.BindColor;

public class FilterPillsViewHolder extends FilterViewHolder {

    private final LayoutInflater inflater;

    @Bind(R.id.filter_hint)
    TextView filterHintText;

    @Bind(R.id.active_filters_container)
    ViewGroup activeFiltersContainer;

    @BindColor(R.color.rangebar_red)
    int redColor;

    public FilterPillsViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);

        inflater = activity.getLayoutInflater();
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);

        activeFiltersContainer.removeAllViews();

        for (Filter filter : getFilterSet().getFilters()) {
            inflater.inflate(R.layout.item_filter_pill, activeFiltersContainer, true);
            TextView newFilterView = (TextView) activeFiltersContainer.getChildAt(activeFiltersContainer.getChildCount() - 1);
            newFilterView.setText(filter.getPillText());
            newFilterView.setTag(filter);
            newFilterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeFilterMatching((Filter) v.getTag());
                }
            });
            if (filter.isError()) {
                newFilterView.setTextColor(redColor);
            }
        }
        filterHintText.setVisibility(getFilterSet().isEmpty() ? View.VISIBLE : View.GONE);
    }
}
