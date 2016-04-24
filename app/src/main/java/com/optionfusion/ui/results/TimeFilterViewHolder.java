package com.optionfusion.ui.results;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.TimeFilter;
import com.optionfusion.ui.widgets.rangebar.RangeBar;
import com.optionfusion.util.Util;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.Bind;

public class TimeFilterViewHolder extends FilterLayoutViewHolder {

    @Bind(R.id.rangebar_expiration)
    RangeBar rangeBarExpiration;

    @Bind(R.id.rangebar_expiration_text)
    TextView textExpiration;

    public TimeFilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);

        String symbol = ((ResultsAdapter.FilterLayoutListItem) item).symbol;
        FilterSet filterSet = ((ResultsAdapter.FilterLayoutListItem) item).filterSet;

        //TODO optimize out?
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
    }

    @Override
    Filter.FilterType getFilterType() {
        return Filter.FilterType.TIME;
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
}
