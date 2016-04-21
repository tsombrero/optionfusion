package com.optionfusion.ui.results;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.RoiFilter;
import com.optionfusion.model.filter.SpreadTypeFilter;
import com.optionfusion.model.filter.StrikeFilter;
import com.optionfusion.model.filter.TimeFilter;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.widgets.rangebar.RangeBar;
import com.optionfusion.util.Util;
import com.wefika.flowlayout.FlowLayout;

import org.joda.time.DateTime;

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
    private final Activity activity;
    private FilterSet filterSet;

    @Inject
    OptionChainProvider optionChainProvider;

    @Bind(R.id.btn_roi)
    ImageButton btnRoi;

    @Bind(R.id.btn_strike)
    ImageButton btnStrike;

    @Bind(R.id.btn_time)
    ImageButton btnTime;

    @Bind(R.id.btn_spread_types)
    ImageButton btnSpreadTypes;

    @Bind(R.id.return_edit_layout)
    ViewGroup editRoiLayout;

    @Bind(R.id.expiration_edit_layout)
    ViewGroup editTimeLayout;

    @Bind(R.id.strike_edit_layout)
    ViewGroup editStrikeLayout;

    @Bind(R.id.spread_types_edit_layout)
    ViewGroup editSpreadTypesLayout;

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

    @Bind(R.id.strike_edit_bearish_text)
    TextView textStrikeBearish;

    @Bind(R.id.strike_edit_bullish_text)
    TextView textStrikeBullish;

    @Bind(R.id.rangebar_expiration_text)
    TextView textExpiration;

    @Bind(R.id.roi_edit_value)
    EditText editRoiValue;

    @Bind(R.id.bullcall)
    TextView typesFilter_bullCallSelection;

    @Bind(R.id.bearcall)
    TextView typesFilter_bearCallSelection;

    @Bind(R.id.bullput)
    TextView typesFilter_bullPutSelection;

    @Bind(R.id.bearput)
    TextView typesFilter_bearPutSelection;

    private String symbol;

    public FilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
        this.activity = activity;
        OptionFusionApplication.from(context).getComponent().inject(this);
        inflater = activity.getLayoutInflater();
        ButterKnife.bind(itemView);

        rangeBarStrikeBearish.setTag(StrikeFilter.Type.BEARISH);
        rangeBarStrikeBullish.setTag(StrikeFilter.Type.BULLISH);
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
            case R.id.btn_spread_types:
                onClickSpreadTypeFilter();
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
        editRoiValue.requestFocus();
        Util.showSoftKeyboard(activity);
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

        final List<DateTime> dates = optionChainProvider.get(symbol).getExpirationDates();
        Collections.sort(dates, new Comparator<DateTime>() {
            @Override
            public int compare(DateTime lhs, DateTime rhs) {
                return lhs.compareTo(rhs);
            }
        });

        ArrayList<String> dateStr = new ArrayList<>();
        for (DateTime date : dates) {
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

    @OnClick(R.id.btn_spread_types)
    public void onClickSpreadTypeFilter() {
        if (editSpreadTypesLayout.getVisibility() == View.VISIBLE) {
            resetButtons(true);
            return;
        }

        refreshSpreadTypeFilterSelection();

        showEditFilter(btnSpreadTypes, editSpreadTypesLayout);
    }

    private void refreshSpreadTypeFilterSelection() {
        int grey = (btnSpreadTypes.getContext().getResources().getColor(R.color.foreground_light_grey));
        int bull = (btnSpreadTypes.getContext().getResources().getColor(R.color.bull_spread_background));
        int bear = (btnSpreadTypes.getContext().getResources().getColor(R.color.bear_spread_background));


        SpreadTypeFilter filter = (SpreadTypeFilter) filterSet.getFilterMatching(new SpreadTypeFilter());
        if (filter == null)
            filter = new SpreadTypeFilter();

        typesFilter_bullPutSelection.setBackgroundColor(filter.isIncluded(VerticalSpread.SpreadType.BULL_PUT) ? bull : grey);
        typesFilter_bearCallSelection.setBackgroundColor(filter.isIncluded(VerticalSpread.SpreadType.BEAR_CALL) ? bear : grey);
        typesFilter_bullCallSelection.setBackgroundColor(filter.isIncluded(VerticalSpread.SpreadType.BULL_CALL) ? bull : grey);
        typesFilter_bearPutSelection.setBackgroundColor(filter.isIncluded(VerticalSpread.SpreadType.BEAR_PUT) ? bear : grey);
    }

    @OnClick({R.id.bearput, R.id.bullcall, R.id.bullput, R.id.bearcall})
    public void onEditSpreadTypeFilter(View v) {

        //can this be a tag on the view?
        VerticalSpread.SpreadType spreadType = null;
        switch (v.getId()) {
            case R.id.bearcall:
                spreadType = VerticalSpread.SpreadType.BEAR_CALL;
                break;
            case R.id.bearput:
                spreadType = VerticalSpread.SpreadType.BEAR_PUT;
                break;
            case R.id.bullcall:
                spreadType = VerticalSpread.SpreadType.BULL_CALL;
                break;
            case R.id.bullput:
                spreadType = VerticalSpread.SpreadType.BULL_PUT;
                break;
            default:
                return;
        }
        SpreadTypeFilter filter = (SpreadTypeFilter) filterSet.getFilterMatching(new SpreadTypeFilter());
        if (filter == null)
            filter = new SpreadTypeFilter();

        filter.includeSpreadType(spreadType, !filter.isIncluded(spreadType));

        if (TextUtils.isEmpty(filter.getPillText()))
            removeFilterMatching(filter);
        else
            addFilter(filter);

        refreshSpreadTypeFilterSelection();
    }

    private class ExpirationRangeBarListener implements RangeBar.OnRangeBarChangeListener {
        private final List<DateTime> dates;

        ExpirationRangeBarListener(List<DateTime> dates) {
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

            DateTime startDate = null;
            if (leftPinIndex > 0)
                startDate = dates.get(leftPinIndex);

            DateTime endDate = null;
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

        showEditFilter(btnStrike, editStrikeLayout);
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
        if (filterSet.removeFilterMatching(matchfilter)) {
            resultsListener.onChange(filterSet);
            refreshSpreadTypeFilterSelection();
        }
    }

    private void resetButtons(boolean enabled) {
        for (ImageButton btn : new ImageButton[]{btnRoi, btnStrike, btnTime, btnSpreadTypes}) {
            btn.setEnabled(enabled);
            btn.setSelected(false);
        }

        for (ViewGroup viewGroup : new ViewGroup[]{editRoiLayout, editTimeLayout, editStrikeLayout, editSpreadTypesLayout}) {
            viewGroup.setVisibility(View.GONE);
        }

        filterSet.setActiveButton(0);

        filterHintText.setVisibility(filterSet.isEmpty() ? View.VISIBLE : View.GONE);

        if (enabled)
            Util.hideSoftKeyboard(activity);
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
