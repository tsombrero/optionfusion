package com.optionfusion.ui.results;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.StrikeFilter;
import com.optionfusion.ui.widgets.rangebar.RangeBar;
import com.optionfusion.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;

public class StrikeFilterViewHolder extends FilterLayoutViewHolder {

    @Bind(R.id.strike_edit_bullish)
    RangeBar rangeBarStrikeBullish;

    @Bind(R.id.strike_edit_bearish)
    RangeBar rangeBarStrikeBearish;

    @Bind(R.id.strike_edit_bearish_text)
    TextView textStrikeBearish;

    @Bind(R.id.strike_edit_bullish_text)
    TextView textStrikeBullish;

    public StrikeFilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);

        rangeBarStrikeBearish.setTag(StrikeFilter.Type.BEARISH);
        rangeBarStrikeBullish.setTag(StrikeFilter.Type.BULLISH);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);

        String symbol = ((ResultsAdapter.FilterLayoutListItem)item).symbol;
        FilterSet filterSet = ((ResultsAdapter.FilterLayoutListItem)item).filterSet;

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
    }

    @Override
    Filter.FilterType getFilterType() {
        return Filter.FilterType.STRIKE;
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
}
