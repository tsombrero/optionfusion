package com.mosoft.momomentum.ui.results;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.util.Util;

import butterknife.Bind;
import butterknife.ButterKnife;

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
        protected final ResultsAdapter.FilterChangeListener filterChangeListener;

        public BaseViewHolder(View itemView, Context context, ResultsAdapter.FilterChangeListener filterChangeListener) {
            super(itemView);
            this.context = context;
            this.filterChangeListener = filterChangeListener;
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

        @Bind(R.id.askPrice)
        TextView askPrice;

        @Bind(R.id.breakEvenPrice)
        TextView breakEvenPrice;

        @Bind(R.id.daysToExp)
        TextView daysToExp;

        @Bind(R.id.description_left)
        TextView description;

        @Bind(R.id.maxReturn)
        TextView maxReturn;

        @Bind(R.id.summary)
        TextView summary;

        @Bind(R.id.header)
        View header;

        public SpreadViewHolder(View itemView, Context context) {
            super(itemView, context, null);
        }

        public void bind(ResultsAdapter.ListItem item) {
            Spread spread = item.spread;

            summary.setText(String.format("Returns %s if %s is %s %s from the current price",
                            Util.formatPercentCompact(spread.getMaxPercentProfitAtExpiration()),    // Returns %s
                            spread.getUnderlyingSymbol(),                                           // if %symbol
                            spread.isBullSpread()                                                   // "down less than" "up at least" "up less than" "down at least"
                                    ? (spread.isInTheMoney_MaxReturn() ? "down less than" : "up at least")
                                    : (spread.isInTheMoney_MaxReturn() ? "up less than" : "down at least"),
                            Util.formatPercentCompact(Math.abs(spread.getPercentChange_MaxProfit()))    // some percent
            ));

            askPrice.setText(Util.formatDollars(spread.getAsk()));
            breakEvenPrice.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            daysToExp.setText(Util.getFormattedOptionDate(spread.getExpiresDate()) + " / " + String.valueOf(spread.getDaysToExpiration()) + " days");
            description.setText(String.format("%s %.2f/%.2f", spread.getBuy().getOptionType().toString(), spread.getBuy().getStrike(), spread.getSell().getStrike()));
            maxReturn.setText(Util.formatDollars(spread.getMaxProfitAtExpiration()) + " / " + Util.formatPercentCompact(spread.getMaxReturnAnnualized()) + " yr" );

            Resources resources = context.getResources();

            int color = spread.isInTheMoney_BreakEven()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.material_red_900);

            breakEvenPrice.setTextColor(color);

            color = spread.isBullSpread()
                    ? resources.getColor(R.color.bull_spread_background)
                    : resources.getColor(R.color.bear_spread_background);

            header.setBackgroundColor(color);
        }
    }
}
