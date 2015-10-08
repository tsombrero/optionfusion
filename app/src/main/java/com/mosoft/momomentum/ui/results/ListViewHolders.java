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
        protected final ResultsAdapter.FilterChangeListener changeListener;

        public BaseViewHolder(View itemView, Context context, ResultsAdapter.FilterChangeListener changeListener) {
            super(itemView);
            this.context = context;
            this.changeListener = changeListener;
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

        @Bind(R.id.annualizedMaxReturn)
        TextView annualizedReturn;

        @Bind(R.id.askPrice)
        TextView askPrice;

        @Bind(R.id.breakEvenPrice)
        TextView breakEvenPrice;

        @Bind(R.id.daysToExp)
        TextView daysToExp;

        @Bind(R.id.descriptionLeft)
        TextView description;

        @Bind(R.id.descriptionRight)
        TextView expirationDate;

        @Bind(R.id.maxReturn)
        TextView maxReturn;

        @Bind(R.id.percentChangeToBreakEven)
        TextView percentChangeToBreakEven;

        @Bind(R.id.percentChangeToMaxReturn)
        TextView percentChangeToMaxReturn;

        @Bind(R.id.maxReturnPrice)
        TextView maxReturnPrice;

        @Bind(R.id.title_maxReturnPrice)
        TextView title_maxReturnPrice;

        @Bind(R.id.title_breakEvenPrice)
        TextView title_breakEvenPrice;

        public SpreadViewHolder(View itemView, Context context) {
            super(itemView, context, null);
        }

        public void bind(ResultsAdapter.ListItem item) {
            Spread spread = item.spread;
            annualizedReturn.setText(Util.formatPercent(spread.getMaxReturnAnnualized()));
            askPrice.setText(Util.formatDollars(spread.getAsk()));
            breakEvenPrice.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            maxReturnPrice.setText(Util.formatDollars(spread.getPrice_MaxReturn()));
            daysToExp.setText(String.valueOf(spread.getDaysToExpiration()) + " days");
            description.setText(String.format("%s %.2f/%.2f", spread.getBuy().getOptionType().toString(), spread.getBuy().getStrike(), spread.getSell().getStrike()));
            expirationDate.setText(Util.getFormattedOptionDate(spread.getExpiresDate()));
            maxReturn.setText(Util.formatDollars(spread.getMaxProfitAtExpiration()));
            percentChangeToBreakEven.setText(Util.formatPercent(spread.getPercentChange_BreakEven()) + (spread.isInTheMoney_BreakEven() ? "" : "  OTM"));
            percentChangeToMaxReturn.setText(Util.formatPercent(spread.getPercentChange_MaxProfit()) + (spread.isInTheMoney_MaxReturn() ? "" : "  OTM"));

            title_maxReturnPrice.setText(String.format(context.getResources().getString(R.string.formatPriceAtMaxReturn), spread.isCall() ? "Above" : "Below"));
            title_breakEvenPrice.setText(String.format(context.getResources().getString(R.string.formatPriceAtBreakEven), spread.isCall() ? "Below" : "Above"));

            Resources resources = context.getResources();

            int color = spread.isInTheMoney_BreakEven()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.red_900);

            percentChangeToBreakEven.setTextColor(color);
            breakEvenPrice.setTextColor(color);

            color = spread.isInTheMoney_MaxReturn()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.red_900);

            percentChangeToMaxReturn.setTextColor(color);
            maxReturnPrice.setTextColor(color);
        }
    }
}
