package com.mosoft.momomentum.ui.results;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.filter.Filter;
import com.mosoft.momomentum.model.filter.RoiFilter;
import com.wefika.flowlayout.FlowLayout;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class FilterViewHolder extends ListViewHolders.BaseViewHolder {

    private FilterSet filterSet;

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

    @Bind(R.id.active_filters_container)
    FlowLayout activeFiltersContainer;

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
            View newFilterView = activeFiltersContainer.inflate(context, R.layout.item_filter_pill, activeFiltersContainer);
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
            resetButtons();
            return;
        }

        showEditFilter(btnRoi, editRoiLayout);
    }

    @OnEditorAction(R.id.return_edit_value)
    public boolean onEditRoi(TextView view, int action) {
        if (action != EditorInfo.IME_ACTION_DONE)
            return false;

        try {
            filterSet.addFilter(new RoiFilter(Double.valueOf(view.getText().toString()) / 100d));
        } catch (Exception e) {
            Log.w("Can't add filter", e);
        }
        resetButtons();
        changeListener.onChange(filterSet);
        return true;
    }

    private void resetButtons() {
        for (ImageButton btn : new ImageButton[]{btnDisposition, btnRoi, btnStrike, btnTime}) {
            btn.setEnabled(true);
            btn.setSelected(false);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout}) {
            viewGroup.setVisibility(View.GONE);
        }
    }

    private void showEditFilter(ImageButton filterButton, ViewGroup filterLayout) {
        for (ImageButton btn : new ImageButton[]{btnDisposition, btnRoi, btnStrike, btnTime}) {
            btn.setEnabled(false);
            btn.setSelected(false);
        }

        if (filterButton != null) {
            filterButton.setEnabled(true);
            filterButton.setSelected(true);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout}) {
            viewGroup.setVisibility(View.GONE);
        }

        if (filterLayout != null)
            filterLayout.setVisibility(View.VISIBLE);
    }
}
