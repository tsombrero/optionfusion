package com.mosoft.momomentum.ui.results;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;

import com.appyvet.rangebar.RangeBar;
import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.filter.Filter;
import com.mosoft.momomentum.model.filter.RoiFilter;
import com.wefika.flowlayout.FlowLayout;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class FilterViewHolder extends ListViewHolders.BaseViewHolder {

    private FilterSet filterSet;

    @Inject
    OptionChainProvider optionChainProvider;

    @Bind(R.id.btn_disposition)
    ImageButton btnDisposition;

    @Bind(R.id.btn_roi)
    ImageButton btnRoi;

    @Bind(R.id.btn_strike)
    ImageButton btnStrike;

    @Bind(R.id.btn_time)
    ImageButton btnTime;

    @Bind(R.id.return_edit_layout)
    ViewGroup editRoiLayout;

    @Bind(R.id.time_edit_layout)
    ViewGroup editTimeLayout;

    @Bind(R.id.active_filters_container)
    FlowLayout activeFiltersContainer;

    @Bind(R.id.rangebar_expiration)
    RangeBar rangeBarExpiration;

    public FilterViewHolder(View itemView, Context context, ResultsAdapter.FilterChangeListener changeListener) {
        super(itemView, context, changeListener);
        ButterKnife.bind(itemView);
    }

    public void bind(final ResultsAdapter.ListItem item) {
        this.filterSet = item.filterSet;
        bindFilters();
    }

    private void bindFilters() {
        activeFiltersContainer.removeAllViews();

        for (Filter filter : filterSet.getFilters()) {
            View newFilterView = View.inflate(context, R.layout.item_filter_pill, activeFiltersContainer);
            TextView newFilterTextView = (TextView) newFilterView.findViewById(R.id.filter_pill_text);
            newFilterTextView.setText(filter.getPillText());
            newFilterTextView.setTag(filter);
            newFilterTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filterSet.removeFilter((Filter) v.getTag());
                    changeListener.onChange(filterSet);
                }
            });
        }
    }

    @OnClick(R.id.btn_roi)
    public void onClickRoiFilter() {
        if (editRoiLayout.getVisibility() == View.VISIBLE) {
            resetButtons(true);
            return;
        }

        showEditFilter(btnRoi, editRoiLayout);
    }

    @OnEditorAction(R.id.roi_edit_value)
    public boolean onEditRoi(TextView view, int action) {
        if (action != EditorInfo.IME_ACTION_DONE)
            return false;

        try {
            filterSet.addFilter(new RoiFilter(Double.valueOf(view.getText().toString()) / 100d));
        } catch (Exception e) {
            Log.w("Can't add filter", e);
        }
        resetButtons(true);
        changeListener.onChange(filterSet);
        return true;
    }

    @OnClick(R.id.btn_time)
    public void onClickTimeFilter() {
        if (editTimeLayout.getVisibility() == View.VISIBLE) {
            resetButtons(true);
            return;
        }

        rangeBarExpiration.setXValues(optionChainProvider.get(););

        showEditFilter(btnTime, editTimeLayout);
    }

    private void resetButtons(boolean enabled) {
        for (ImageButton btn : new ImageButton[]{btnDisposition, btnRoi, btnStrike, btnTime}) {
            btn.setEnabled(enabled);
            btn.setSelected(false);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout, editTimeLayout}) {
            viewGroup.setVisibility(View.GONE);
        }
    }

    private void showEditFilter(ImageButton filterButton, ViewGroup filterLayout) {
        resetButtons(false);

        if (filterButton != null) {
            filterButton.setEnabled(true);
            filterButton.setSelected(true);
        }

        if (filterLayout != null)
            filterLayout.setVisibility(View.VISIBLE);
    }
}
