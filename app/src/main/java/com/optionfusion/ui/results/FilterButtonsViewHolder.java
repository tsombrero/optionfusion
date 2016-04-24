package com.optionfusion.ui.results;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.optionfusion.R;

import butterknife.Bind;
import butterknife.OnClick;

public class FilterButtonsViewHolder extends FilterViewHolder {
    @Bind(R.id.btn_roi)
    ImageButton btnRoi;

    @Bind(R.id.btn_strike)
    ImageButton btnStrike;

    @Bind(R.id.btn_time)
    ImageButton btnTime;

    @Bind(R.id.btn_spread_types)
    ImageButton btnSpreadTypes;

    @Bind(R.id.btn_roi_label)
    TextView labelRoi;

    @Bind(R.id.btn_spread_types_label)
    TextView labelTypes;

    @Bind(R.id.btn_strike_label)
    TextView labeStrike;

    @Bind(R.id.btn_time_label)
    TextView labelTime;


    public FilterButtonsViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        setSelectedButton(((ResultsAdapter.FilterSetListItem) item).filterSet.getActiveButton());
    }

    private void setSelectedButton(int viewResId) {
        ImageButton[] buttons = new ImageButton[]{btnRoi, btnStrike, btnTime, btnSpreadTypes};
        TextView[] textViews = new TextView[]{labelRoi, labeStrike, labelTime, labelTypes};

        for (int i = 0; i < buttons.length; i++) {
            ImageButton btn = buttons[i];
            btn.setActivated(btn.getId() == viewResId || viewResId == 0);
            btn.setSelected(btn.getId() == viewResId);

            TextView label = textViews[i];
            label.setActivated(btn.isActivated());
            label.setSelected(btn.isSelected());
        }
    }

    @OnClick({R.id.btn_spread_types, R.id.btn_roi, R.id.btn_strike, R.id.btn_time})
    public void onClickFilterButton(View view) {
        int selected = view.getId();

        if (view.isSelected())
            selected = 0;

        setSelectedButton(selected);

        resultsListener.onFilterSelected(selected);
    }
}
