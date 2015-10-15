package com.mosoft.momomentum.ui.results;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.mosoft.momomentum.model.filter.TimeFilter;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;
import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class FilterViewHolder extends ListViewHolders.BaseViewHolder {

    private final LayoutInflater inflater;
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

    @Bind(R.id.filter_hint)
    TextView filterHintText;

    private String symbol;

    public FilterViewHolder(View itemView, Activity activity, ResultsAdapter.FilterChangeListener changeListener) {
        super(itemView, activity, changeListener);
        MomentumApplication.from(context).getComponent().inject(this);
        inflater = activity.getLayoutInflater();
        ButterKnife.bind(itemView);
    }

    public void bind(final ResultsAdapter.ListItem item) {
        this.symbol = item.symbol;
        this.filterSet = item.filterSet;
        bindFilters();
    }

    private void bindFilters() {
        activeFiltersContainer.removeAllViews();

        for (Filter filter : filterSet.getFilters()) {
            inflater.inflate(R.layout.item_filter_pill, activeFiltersContainer, true);
            TextView newFilterView = (TextView) activeFiltersContainer.getChildAt(activeFiltersContainer.getChildCount() - 1);
            newFilterView.setText(filter.getPillText());
            newFilterView.setTag(filter);
            newFilterView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    filterSet.removeFilter((Filter) v.getTag());
                    changeListener.onChange(filterSet);
                }
            });
        }

        filterHintText.setVisibility(filterSet.isEmpty() ? View.VISIBLE : View.GONE);
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

        final List<Date> dates = optionChainProvider.get(symbol).getExpirationDates();
        Collections.sort(dates, new Comparator<Date>() {
            @Override
            public int compare(Date lhs, Date rhs) {
                return Long.compare(lhs.getTime(), rhs.getTime());
            }
        });

        ArrayList<String> dateStr = new ArrayList<>();
        for (Date date : dates) {
            dateStr.add(Util.getFormattedOptionDate(date));
        }

        rangeBarExpiration.setxValues(dateStr);

        rangeBarExpiration.setRangePinsByIndices(0, dateStr.size() - 1);

        rangeBarExpiration.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex, int rightPinIndex, String leftPinValue, String rightPinValue) {
                Date startDate = null;
                if (leftPinIndex > 0)
                    startDate = dates.get(leftPinIndex);

                Date endDate = null;
                if (rightPinIndex < dates.size() - 1)
                    endDate = dates.get(rightPinIndex);

                if (startDate == null && endDate == null) {
                    for (Filter filter : filterSet.getFilters()) {
                        if (filter instanceof TimeFilter)
                            filterSet.removeFilter(filter);
                    }
                } else {
                    filterSet.addFilter(new TimeFilter(startDate, endDate));
                }
                changeListener.onChange(filterSet);
            }
        });

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
        filterHintText.setVisibility(filterSet.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showEditFilter(ImageButton filterButton, ViewGroup filterLayout) {
        resetButtons(false);

        if (filterButton != null) {
            filterButton.setEnabled(true);
            filterButton.setSelected(true);
        }

        if (filterLayout != null) {
            filterLayout.setVisibility(View.VISIBLE);
            filterHintText.setVisibility(View.GONE);
        }
    }
}
