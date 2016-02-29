package com.optionfusion.ui.results;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.ui.SharedViewHolders;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ListViewHolders {
    enum ViewType {
        LABEL(R.layout.item_label),
        FILTER_SET(R.layout.item_filter_buttons),
        SPREAD_DETAILS(R.layout.item_spread_details);

        int layout;

        ViewType(int layout) {

            this.layout = layout;
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

    public static class LabelViewHolder extends BaseViewHolder {
        @Bind(R.id.text)
        TextView textView;

        public LabelViewHolder(View itemView) {
            super(itemView, null, null);
        }

        @Override
        void bind(ResultsAdapter.ListItem item) {
            textView.setText(item.labelText);
        }
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
            this.spread = item.spread;
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
