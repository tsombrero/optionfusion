package com.mosoft.optionfusion.ui.results;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mosoft.optionfusion.ui.widgets.rangebar.RangeBar;
import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.model.FilterSet;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.filter.Filter;
import com.mosoft.optionfusion.model.filter.RoiFilter;
import com.mosoft.optionfusion.model.filter.StrikeFilter;
import com.mosoft.optionfusion.model.filter.TimeFilter;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.util.Util;
import com.wefika.flowlayout.FlowLayout;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class FilterViewHolder extends ListViewHolders.BaseViewHolder {

    private static final String TAG = FilterViewHolder.class.getSimpleName();
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

    public FilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
        OptionFusionApplication.from(context).getComponent().inject(this);
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

        int activeButton = filterSet.getActiveButton();

        resetButtons(true);

        switch (activeButton) {
            case R.id.btn_time:
                onClickTimeFilter();
                break;
            case R.id.btn_strike:
                onClickStrikeFilter();
                break;
            case R.id.btn_roi:
                onClickRoiFilter();
                break;
            case R.id.btn_sort:
                onClickSortButton();
                break;
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

        final List<LocalDate> dates = optionChainProvider.get(symbol).getExpirationDates();
        Collections.sort(dates, new Comparator<LocalDate>() {
            @Override
            public int compare(LocalDate lhs, LocalDate rhs) {
                return lhs.compareTo(rhs);
            }
        });

        ArrayList<String> dateStr = new ArrayList<>();
        for (LocalDate date : dates) {
            dateStr.add(Util.getFormattedOptionDate(date));
        }

        TimeFilter dateFilter = (TimeFilter) filterSet.getFilterMatching(TimeFilter.EMPTY_FILTER);

        rangeBarExpiration.setxValues(dateStr);
        rangeBarExpiration.setRangePins(dates, dateFilter);

        ExpirationRangeBarListener listener = new ExpirationRangeBarListener(dates);
        rangeBarExpiration.setOnRangeBarChangeListener(listener);
        listener.onRangeChangeListener(rangeBarExpiration, null);

        showEditFilter(btnTime, editTimeLayout);
    }

    private class ExpirationRangeBarListener implements RangeBar.OnRangeBarChangeListener {
        private final List<LocalDate> dates;

        ExpirationRangeBarListener(List<LocalDate> dates) {
            this.dates = dates;
        }

        @Override
        public void onRangeChangeListener(RangeBar rangeBar, RangeBar.Action action) {
            if (action == RangeBar.Action.DOWN) {
                animateTextViewActive(textExpiration, true);
                return;
            }

            int leftPinIndex = rangeBar.getLeftIndex();
            int rightPinIndex = rangeBar.getRightIndex();

            LocalDate startDate = null;
            if (leftPinIndex > 0)
                startDate = dates.get(leftPinIndex);

            LocalDate endDate = null;
            if (rightPinIndex < dates.size() - 1)
                endDate = dates.get(rightPinIndex);

            textExpiration.setText(Util.formatDateRange(startDate, endDate));

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
        Log.i(TAG, "TACO onClickStrikeFilter");

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

        rangeBarStrikeBullish.setRangePins(strikes, (RangeBar.RangeBarDataProvider) filterSet.getFilterMatching(StrikeFilter.EMPTY_BULLISH));
        rangeBarStrikeBearish.setRangePins(strikes, (RangeBar.RangeBarDataProvider) filterSet.getFilterMatching(StrikeFilter.EMPTY_BEARISH));

        StrikeRangeChangeListener rangeBarListener = new StrikeRangeChangeListener(strikes);
        rangeBarStrikeBearish.setOnRangeBarChangeListener(rangeBarListener);
        rangeBarStrikeBullish.setOnRangeBarChangeListener(rangeBarListener);

        rangeBarListener.onRangeChangeListener(rangeBarStrikeBearish, null);
        rangeBarListener.onRangeChangeListener(rangeBarStrikeBullish, null);

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
        resultsListener.onChange(filterSet);
        resetButtons(true);
    }

    @OnClick(R.id.sort_high_return)
    public void onClickSortHighReturn() {
        filterSet.setComparator(new Spread.DescendingMaxReturnComparator());
        resultsListener.onChange(filterSet);
        resetButtons(true);
    }

    private class StrikeRangeChangeListener implements RangeBar.OnRangeBarChangeListener {

        private final List<Double> strikes;

        public StrikeRangeChangeListener(List<Double> strikes) {
            this.strikes = strikes;
        }

        @Override
        public void onRangeChangeListener(RangeBar rangeBar, RangeBar.Action action) {
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

            int leftPinIndex = rangeBar.getLeftIndex();
            int rightPinIndex = rangeBar.getRightIndex();

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
        resultsListener.onChange(filterSet);
    }

    private void removeFilterMatching(Filter matchfilter) {
        if (filterSet.removeFilterMatching(matchfilter))
            resultsListener.onChange(filterSet);
    }

    private void resetButtons(boolean enabled) {
        for (ImageButton btn : new ImageButton[]{btnSorting, btnRoi, btnStrike, btnTime}) {
            btn.setEnabled(enabled);
            btn.setSelected(false);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout, editTimeLayout, editStrikeLayout, editSortingLayout}) {
            viewGroup.setVisibility(View.GONE);
        }

        filterSet.setActiveButton(0);

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
            filterSet.setActiveButton(filterButton.getId());
        }
    }

    private void animateTextViewActive(TextView textView, boolean active) {
        float targetScale = active ? 1.2f : 1f;

        textView.animate()
                .scaleX(targetScale).scaleY(targetScale)
                .setDuration(100)
                .translationY((1f - targetScale) * (float) textView.getHeight())
                .start();
    }
}
