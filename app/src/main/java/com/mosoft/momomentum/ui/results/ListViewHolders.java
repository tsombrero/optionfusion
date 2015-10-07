package com.mosoft.momomentum.ui.results;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.SpreadFilter;
import com.mosoft.momomentum.util.Util;

import java.security.KeyException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

public class ListViewHolders {
    enum ViewType {
        LABEL(R.layout.item_label),
        ACTIVE_FILTER(R.layout.item_filter_active),
        INACTIVE_FILTER(R.layout.item_filter_inactive),
        SPREAD_DETAILS(R.layout.item_spread_details);

        int layout;

        ViewType(int layout) {

            this.layout = layout;
        }
    }

    public static abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        protected final Resources resources;
        protected final ResultsAdapter.FilterClickListener clickListener;

        public BaseViewHolder(View itemView, Resources resources, ResultsAdapter.FilterClickListener clickListener) {
            super(itemView);
            this.resources = resources;
            this.clickListener = clickListener;
            ButterKnife.bind(this, itemView);
        }

        abstract void bind(ResultsAdapter.ListItem item);
    }

    public static class LabelViewHolder extends BaseViewHolder {
        @Bind(R.id.text)
        TextView textView;

        public LabelViewHolder(View itemView, Resources resources, ResultsAdapter.FilterClickListener clickListener) {
            super(itemView, resources, clickListener);
        }

        @Override
        void bind(ResultsAdapter.ListItem item) {
            textView.setText(item.labelText);
        }
    }

    public static class FilterViewHolder extends BaseViewHolder {

        @Bind(R.id.filter_name)
        TextView filterName;

        @Bind(R.id.filter_add)
        ImageView addFilter;

        @Bind(R.id.percent_entry_layout)
        View percentageEntryLayout;

        @Bind(R.id.dollar_entry_layout)
        View dollarEntryLayout;

        @Bind(R.id.date_entry_layout)
        View dateEntryLayout;

        private SpreadFilter.Filter filter;

        public FilterViewHolder(View itemView, Resources resources, ResultsAdapter.FilterClickListener clickListener) {
            super(itemView, resources, clickListener);
        }

        public void bind(final ResultsAdapter.ListItem item) {
            filterName.setText(resources.getString(item.filter.getStringRes()));
            this.filter = item.filter;
        }

        @OnClick(R.id.filter_add)
        public void onAddFilter(View view) {
            percentageEntryLayout.setVisibility(View.GONE);
            dollarEntryLayout.setVisibility(View.GONE);
            dateEntryLayout.setVisibility(View.GONE);
            addFilter.setVisibility(View.GONE);

            if (filter.isPercentage()) {
                percentageEntryLayout.setVisibility(View.VISIBLE);
            } else if (filter.isCurrency()) {
                dollarEntryLayout.setVisibility(View.VISIBLE);
            } else {
                dateLayout.setVisibility(View.VISIBLE);
            }
        }

        @OnEditorAction({R.id.filter_dollar_value, R.id.filter_percent_value})
        public void onAction(View v, int actionId, KeyEvent keyEvent) {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            } else if (actionId == EditorInfo.IME_ACTION_DONE) {

            }
        }

    }

    public static class ActiveFilterViewHolder extends FilterViewHolder {

        @Bind(R.id.filterValue)
        TextView filterValue;

        public ActiveFilterViewHolder(View itemView, Resources resources, ResultsAdapter.FilterClickListener clickListener) {
            super(itemView, resources, clickListener);
        }

        public void bind(ResultsAdapter.ListItem item) {
            super.bind(item);
            filterValue.setText(item.filter.formatValue(item.filterValue));
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

        public SpreadViewHolder(View itemView, Resources resources) {
            super(itemView, resources, null);
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

            title_maxReturnPrice.setText(String.format(resources.getString(R.string.formatPriceAtMaxReturn), spread.isCall() ? "Above" : "Below"));
            title_breakEvenPrice.setText(String.format(resources.getString(R.string.formatPriceAtBreakEven), spread.isCall() ? "Below" : "Above"));

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
