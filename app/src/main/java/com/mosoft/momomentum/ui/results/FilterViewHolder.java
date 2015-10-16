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
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.filter.Filter;
import com.mosoft.momomentum.model.filter.RoiFilter;
import com.mosoft.momomentum.model.filter.StrikeFilter;
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

    @Bind(R.id.btn_sort)
    ImageButton btnSorting;

    @Bind(R.id.btn_roi)
    ImageButton btnRoi;

    @Bind(R.id.btn_strike)
    ImageButton btnStrike;

    @Bind(R.id.btn_time)
    ImageButton btnTime;

    @Bind(R.id.return_edit_layout)
    ViewGroup editRoiLayout;

    @Bind(R.id.expiration_edit_layout)
    ViewGroup editTimeLayout;

    @Bind(R.id.strike_edit_layout)
    ViewGroup editStrikeLayout;

    @Bind(R.id.active_filters_container)
    FlowLayout activeFiltersContainer;

    @Bind(R.id.rangebar_expiration)
    RangeBar rangeBarExpiration;

    @Bind(R.id.filter_hint)
    TextView filterHintText;

    @Bind(R.id.strike_edit_bullish)
    RangeBar rangeBarStrikeBullish;

    @Bind(R.id.strike_edit_bearish)
    RangeBar rangeBarStrikeBearish;

    @Bind(R.id.sorting_edit_layout)
    ViewGroup editSortingLayout;

    @Bind(R.id.strike_edit_bearish_text)
    TextView textStrikeBearish;

    @Bind(R.id.strike_edit_bullish_text)
    TextView textStrikeBullish;

    @Bind(R.id.rangebar_expiration_text)
    TextView textExpiration;

    private String symbol;

    private int
            indexBullStrikeLow,
            indexBearStrikeLow,
            indexDateRangeLo,
            indexBullStrikeHi = Integer.MAX_VALUE,
            indexBearStrikeHi = Integer.MAX_VALUE,
            indexDateRangeHi = Integer.MAX_VALUE;

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
                    removeFilterMatching((Filter) v.getTag());
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
            addFilter(new RoiFilter(Double.valueOf(view.getText().toString()) / 100d));
        } catch (Exception e) {
            Log.w("Can't add filter", e);
        }
        resetButtons(true);
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

        indexDateRangeHi = Math.min(indexDateRangeHi, dates.size() - 1);

        rangeBarExpiration.setxValues(dateStr);
        rangeBarExpiration.setRangePinsByIndices(indexDateRangeLo, indexDateRangeHi);

        ExpirationRangeBarListener listener = new ExpirationRangeBarListener(dates);
        rangeBarExpiration.setOnRangeBarChangeListener(listener);
        listener.onRangeChangeListener(rangeBarExpiration, null, indexDateRangeLo, indexDateRangeHi);

        showEditFilter(btnTime, editTimeLayout);
    }

    private class ExpirationRangeBarListener implements RangeBar.OnRangeBarChangeListener {
        private final List<Date> dates;

        ExpirationRangeBarListener(List<Date> dates) {
            this.dates = dates;
        }

        @Override
        public void onRangeChangeListener(RangeBar rangeBar, RangeBar.Action action, int leftPinIndex, int rightPinIndex) {
            if (action == RangeBar.Action.DOWN) {
                animateTextViewActive(textExpiration, true);
                return;
            }
            Date startDate = null;
            if (leftPinIndex > 0)
                startDate = dates.get(leftPinIndex);

            Date endDate = null;
            if (rightPinIndex < dates.size() - 1)
                endDate = dates.get(rightPinIndex);

            textExpiration.setText(Util.formatDateRange(startDate, endDate));

            indexDateRangeLo = leftPinIndex;
            indexDateRangeHi = rightPinIndex;

            if (action == RangeBar.Action.UP) {
                animateTextViewActive(textExpiration, false);
                if (startDate == null && endDate == null) {
                    removeFilterMatching(TimeFilter.EMPTY_FILTER);
                } else {
                    addFilter(new TimeFilter(startDate, endDate));
                }
            }
        }

    }

    @OnClick(R.id.btn_strike)
    public void onClickStrikeFilter() {
        if (editStrikeLayout.getVisibility() == View.VISIBLE) {
            resetButtons(true);
            return;
        }

        final List<Double> strikes = optionChainProvider.get(symbol).getStrikePrices();

        Collections.sort(strikes, new Comparator<Double>() {
            @Override
            public int compare(Double lhs, Double rhs) {
                return Double.compare(lhs, rhs);
            }
        });

        ArrayList<String> strikeStrings = new ArrayList<>();
        for (Double strike : strikes) {
            strikeStrings.add(Util.formatDollarsCompact(strike));
        }

        rangeBarStrikeBullish.setxValues(strikeStrings);
        rangeBarStrikeBearish.setxValues(strikeStrings);

        indexBearStrikeHi = Math.min(indexBearStrikeHi, strikes.size() - 1);
        indexBullStrikeHi = Math.min(indexBullStrikeHi, strikes.size() - 1);

        rangeBarStrikeBullish.setRangePinsByIndices(indexBullStrikeLow, indexBullStrikeHi);
        rangeBarStrikeBearish.setRangePinsByIndices(indexBearStrikeLow, indexBearStrikeHi);

        StrikeRangeChangeListener rangeBarListener = new StrikeRangeChangeListener(strikes);
        rangeBarStrikeBearish.setOnRangeBarChangeListener(rangeBarListener);
        rangeBarStrikeBullish.setOnRangeBarChangeListener(rangeBarListener);

        rangeBarListener.onRangeChangeListener(rangeBarStrikeBearish, null, indexBearStrikeLow, indexBearStrikeHi);
        rangeBarListener.onRangeChangeListener(rangeBarStrikeBullish, null, indexBullStrikeLow, indexBullStrikeHi);

        rangeBarStrikeBearish.setTag(StrikeFilter.Type.BEARISH);
        rangeBarStrikeBullish.setTag(StrikeFilter.Type.BULLISH);

        showEditFilter(btnStrike, editStrikeLayout);
    }


    @OnClick(R.id.btn_sort)
    public void onClickSortButton() {
        if (editSortingLayout.getVisibility() == View.VISIBLE) {
            resetButtons(true);
            return;
        }

        showEditFilter(btnSorting, editSortingLayout);
    }

    @OnClick(R.id.sort_low_risk)
    public void onClickSortByRisk() {
        filterSet.setComparator(new Spread.DescendingBreakEvenDepthComparator());
        filterChangeListener.onChange(filterSet);
        resetButtons(true);
    }

    @OnClick(R.id.sort_high_return)
    public void onClickSortHighReturn() {
        filterSet.setComparator(new Spread.DescendingMaxReturnComparator());
        filterChangeListener.onChange(filterSet);
        resetButtons(true);
    }

    private class StrikeRangeChangeListener implements RangeBar.OnRangeBarChangeListener {

        private final List<Double> strikes;

        public StrikeRangeChangeListener(List<Double> strikes) {
            this.strikes = strikes;
        }

        @Override
        public void onRangeChangeListener(RangeBar rangeBar, RangeBar.Action action, int leftPinIndex, int rightPinIndex) {
            StrikeFilter.Type type = (StrikeFilter.Type) rangeBar.getTag();
            TextView textView = type == StrikeFilter.Type.BULLISH ? textStrikeBullish : textStrikeBearish;

            if (action == RangeBar.Action.DOWN) {
                animateTextViewActive(textView, true);
                return;
            } else if (action == RangeBar.Action.UP) {
                animateTextViewActive(textView, false);
            }

            double limitLo;
            double limitHi;

            if (leftPinIndex == strikes.size() - 1)
                limitLo = Double.MAX_VALUE;
            else if (leftPinIndex == 0)
                limitLo = 0d;
            else
                limitLo = strikes.get(leftPinIndex);

            if (rightPinIndex == strikes.size() - 1)
                limitHi = Double.MAX_VALUE;
            else if (rightPinIndex == 0)
                limitHi = 0d;
            else
                limitHi = strikes.get(rightPinIndex);

            textView.setText(Util.formatDollarRange(limitLo, limitHi));

            if (action == RangeBar.Action.UP) {
                if (type == StrikeFilter.Type.BULLISH) {
                    indexBullStrikeLow = leftPinIndex;
                    indexBullStrikeHi = rightPinIndex;
                } else {
                    indexBearStrikeLow = leftPinIndex;
                    indexBearStrikeHi = rightPinIndex;
                }

                Filter filter = new StrikeFilter(limitLo, limitHi, type);

                if (limitLo == 0d && limitHi == Double.MAX_VALUE)
                    removeFilterMatching(filter);
                else
                    addFilter(filter);

            }
        }
    }

    private void addFilter(Filter filter) {
        filterSet.addFilter(filter);
        filterChangeListener.onChange(filterSet);
    }

    private void removeFilterMatching(Filter matchfilter) {
        if (filterSet.removeFilterMatching(matchfilter))
            filterChangeListener.onChange(filterSet);
    }

    private void resetButtons(boolean enabled) {
        for (ImageButton btn : new ImageButton[]{btnSorting, btnRoi, btnStrike, btnTime}) {
            btn.setEnabled(enabled);
            btn.setSelected(false);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout, editTimeLayout, editStrikeLayout, editSortingLayout}) {
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

    private void animateTextViewActive(TextView textView, boolean active) {
        float targetScale = active ? 1.2f : 1f;

        textView.animate()
                .scaleX(targetScale).scaleY(targetScale)
                .setDuration(100)
                .translationY((1f - targetScale) * (float)textView.getHeight())
                .start();
    }
}
