package com.optionfusion.ui.results;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.filter.Filter;
import com.optionfusion.model.filter.RoiFilter;
import com.optionfusion.util.Util;

import butterknife.Bind;
import butterknife.OnEditorAction;

public class RoiFilterViewHolder extends FilterLayoutViewHolder {

    @Bind(R.id.roi_edit_value)
    EditText editRoiValue;

    public RoiFilterViewHolder(View itemView, Activity activity, ResultsAdapter.ResultsListener changeListener) {
        super(itemView, activity, changeListener);
    }

    @Override
    public void bind(ResultsAdapter.ListItem item) {
        super.bind(item);
        editRoiValue.requestFocus();
        Util.showSoftKeyboard(activity);
    }

    @Override
    Filter.FilterType getFilterType() {
        return Filter.FilterType.ROI;
    }

    @OnEditorAction(R.id.roi_edit_value)
    public boolean onEditRoi(TextView view, int action) {
        if (action != EditorInfo.IME_ACTION_DONE)
            return false;

        try {
            addFilter(new RoiFilter(Double.valueOf(view.getText().toString()) / 100d));
            resultsListener.onFilterSelected(0);
            Util.hideSoftKeyboard(activity);
        } catch (Exception e) {
            Log.w("Can't add filter", e);
        }
        return true;
    }
}
