package com.optionfusion.ui.results;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.common.TextUtils;
import com.optionfusion.model.filter.AbsoluteReturnFilter;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.RoiFilter;
import com.optionfusion.util.Util;

import butterknife.Bind;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;

public class RoiFilterViewHolder extends FilterLayoutViewHolder {

    @Bind(R.id.roi_edit_value)
    EditText editRoi;

    @Bind(R.id.absolute_return_edit_value)
    EditText editAbsoluteReturn;
    private boolean isInit;

    public RoiFilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);
        
        RoiFilter roifilter = (RoiFilter) getFilterSet().getFilterMatching(new RoiFilter(0d));
        if (!isInit) {
            if (roifilter != null && roifilter.getValue() != null && roifilter.getValue() > 0d) {
                String text = String.valueOf(Math.round(roifilter.getValue() * 100));
                editRoi.setText(text);
                editRoi.setSelection(editRoi.getText().length());
            }

            AbsoluteReturnFilter absFilter = (AbsoluteReturnFilter) getFilterSet().getFilterMatching(new AbsoluteReturnFilter(0d));
            Double absFilterVal = (absFilter == null) ? 0d : absFilter.getValue();
            absFilterVal = Math.round(absFilterVal * 100d) / 100d;
            if (absFilterVal > 0.05d) {
                editAbsoluteReturn.setText(String.valueOf(Util.formatDollars(absFilterVal, 10).replace("$", "")));
                editAbsoluteReturn.setSelection(editAbsoluteReturn.getText().length());
            }
            isInit = true;
        }
    }

    @Override
    Filter.FilterType getFilterType() {
        return Filter.FilterType.ROI;
    }

    @OnTextChanged(R.id.roi_edit_value)
    public void onEditRoiText(CharSequence str) {
        onEditRoi(editRoi, 0);
    }

    @OnTextChanged(R.id.absolute_return_edit_value)
    public void onEditAbsoluteReturnText(CharSequence str) {
        onEditAbsoluteReturn(editAbsoluteReturn, 0);
    }

    @OnEditorAction(R.id.roi_edit_value)
    public boolean onEditRoi(TextView view, int action) {
        try {
            Double filterVal = 0d;
            String s = view.getText().toString();
            if (!TextUtils.isEmpty(s))
                filterVal = Double.valueOf(s);

            if (filterVal > 0d) {
                addFilter(new RoiFilter(filterVal / 100d));
            } else {
                removeFilterMatching(new RoiFilter(0d));
            }
        } catch (Exception e) {
            Log.w("Can't add roi filter", e);
        }

        if (action == EditorInfo.IME_ACTION_DONE) {
            resultsListener.onFilterSelected(0);
            Util.hideSoftKeyboard(activity);
        }
        return true;
    }

    @OnEditorAction(R.id.absolute_return_edit_value)
    public boolean onEditAbsoluteReturn(TextView view, int action) {
        try {
            double val = Float.valueOf(view.getText().toString());
            val = Math.round(val * 100f) / 100f;
            val = Math.max(0.05f, val);

            if (val > 0.05f) {
                addFilter(new AbsoluteReturnFilter(val));
            } else {
                removeFilterMatching(new AbsoluteReturnFilter(0d));
            }
        } catch (Exception e) {
            Log.w("Can't add abs filter", e);
        }

        if (action == EditorInfo.IME_ACTION_DONE) {
            resultsListener.onFilterSelected(0);
            Util.hideSoftKeyboard(activity);
        }
        return true;
    }
}
