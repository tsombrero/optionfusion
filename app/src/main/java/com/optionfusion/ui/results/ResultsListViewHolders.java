package com.optionfusion.ui.results;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.optionfusion.R;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.ui.SharedViewHolders;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ResultsListViewHolders {
    enum ViewType {
        NONE(R.layout.item_filter_layout_none),
        FILTER_BUTTONS(R.layout.item_filter_buttons),
        FILTER_TIME(R.layout.item_filter_layout_time),
        FILTER_STRIKE(R.layout.item_filter_layout_strike),
        FILTER_ROI(R.layout.item_filter_layout_roi),
        FILTER_SPREAD_KIND(R.layout.item_filter_layout_spread_kind),
        FILTER_PILLS(R.layout.item_filter_pills),
        SPREAD_DETAILS(R.layout.item_spread_details);

        int layout;

        ViewType(int layout) {
            this.layout = layout;
        }

        public static ViewType filterTypeFromButtonId(int activeButton) {
            if (activeButton == R.id.btn_roi)
                return FILTER_ROI;
            if (activeButton == R.id.btn_spread_types)
                return FILTER_SPREAD_KIND;
            if (activeButton == R.id.btn_strike)
                return FILTER_STRIKE;
            if (activeButton == R.id.btn_time)
                return FILTER_TIME;
            return NONE;
        }
    }

    public static abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        protected final Context context;
        protected final ResultsAdapter.ResultsListener resultsListener;

        public BaseViewHolder(View itemView, Context context, ResultsAdapter.ResultsListener resultsListener) {
            super(itemView);
            this.context = context;
            this.resultsListener = resultsListener;
            ButterKnife.bind(this, itemView);
        }

        abstract void bind(ResultsAdapter.ListItem item);
    }

    public static class SpreadViewHolder extends BaseViewHolder {

        @Bind(R.id.details_brief)
        View briefDetailsLayout;

        @Bind(R.id.header)
        View spreadHeaderLayout;

        private final SharedViewHolders.TradeDetailsHeaderHolder tradeHeaderHolder;
        private VerticalSpread spread;
        private final SharedViewHolders.BriefTradeDetailsHolder briefTradeDetailsHolder;

        public SpreadViewHolder(View itemView, Context context, ResultsAdapter.ResultsListener listener) {
            super(itemView, context, listener);
            briefTradeDetailsHolder = new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout);
            tradeHeaderHolder = new SharedViewHolders.TradeDetailsHeaderHolder(spreadHeaderLayout);
        }

        public void bind(ResultsAdapter.ListItem item) {
            this.spread = ((ResultsAdapter.ListItemSpread) item).spread;
            briefTradeDetailsHolder.bind(spread);
            tradeHeaderHolder.bind(spread);
        }

        @OnClick(R.id.btn_more)
        @Nullable
        public void onClickMore() {
            if (resultsListener != null)
                resultsListener.onResultSelected(spread, spreadHeaderLayout, briefDetailsLayout);
        }
    }
}
