package com.optionfusion.ui.results;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.SpreadTypeFilter;
import com.optionfusion.model.provider.VerticalSpread;

import butterknife.Bind;
import butterknife.OnClick;

public class SpreadKindViewHolder extends FilterLayoutViewHolder {

    @Bind(R.id.bullcall)
    TextView typesFilter_bullCallSelection;

    @Bind(R.id.bearcall)
    TextView typesFilter_bearCallSelection;

    @Bind(R.id.bullput)
    TextView typesFilter_bullPutSelection;

    @Bind(R.id.bearput)
    TextView typesFilter_bearPutSelection;

    private FilterSet filterSet;

    public SpreadKindViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);
        this.filterSet = ((ResultsAdapter.FilterLayoutListItem)item).filterSet;
        refreshSpreadTypeFilterSelection();
    }

    @Override
    Filter.FilterType getFilterType() {
        return Filter.FilterType.SPREAD_TYPE;
    }

    private void refreshSpreadTypeFilterSelection() {
        int grey = (itemView.getContext().getResources().getColor(R.color.foreground_light_grey));
        int bull = (itemView.getContext().getResources().getColor(R.color.bull_spread_background));
        int bear = (itemView.getContext().getResources().getColor(R.color.bear_spread_background));

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

}
