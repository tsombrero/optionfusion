package com.mosoft.optionfusion.ui;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.ui.widgets.AutoFitTextView;
import com.mosoft.optionfusion.util.Util;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SharedViewHolders {

    public static class StockInfoHolder {

        private final View view;
        @Bind(R.id.symbol)
        TextView symbolView;

        @Bind(R.id.price)
        TextView priceView;

        @Bind(R.id.equityDescription)
        TextView equityDescriptionView;

        public StockInfoHolder(View view) {
            this.view = view;
            ButterKnife.bind(this, view);
        }

        public void bind(Interfaces.OptionChain oc) {
            if (oc == null)
                return;

            symbolView.setText(oc.getUnderlyingStockQuote().getSymbol());
            priceView.setText(Util.formatDollars(oc.getUnderlyingStockQuote().getLast()));
            equityDescriptionView.setText(oc.getUnderlyingStockQuote().getDescription());
            view.setTransitionName(getTransitionName(oc.getUnderlyingStockQuote().getSymbol()));
        }

        static public String getTransitionName(String symbol) {
            return "stockinfo_" + symbol;
        }
    }

    public static class StockQuoteViewHolder {
        private final View view;

        @Bind(R.id.symbol)
        AutoFitTextView symbolView;

        @Bind(R.id.price)
        AutoFitTextView priceView;

        @Bind(R.id.change)
        AutoFitTextView changeView;

        @Bind(R.id.arrow)
        ViewFlipper arrowViewFlipper;

        public StockQuoteViewHolder(View view) {
            this.view = view;
            ButterKnife.bind(this, view);
        }

        public void bind(Interfaces.StockQuote stockQuote) {
            Double change = stockQuote.getLast() - stockQuote.getOpen();
            symbolView.setText(stockQuote.getSymbol());
            priceView.setText(Util.formatDollars(stockQuote.getLast()));
            changeView.setText(Util.formatDollars(change));
            view.setTransitionName(getTransitionName(stockQuote.getSymbol()));
            arrowViewFlipper.setDisplayedChild(change > 0 ? 1 : 0);
        }

        static public String getTransitionName(String symbol) {
            return "stockquote_" + symbol;
        }
    }

    public static class TradeDetailsHeaderHolder {
        private final Context context;

        @Bind(R.id.header)
        View header;

        @Bind(R.id.trade_description)
        TextView description;

        public TradeDetailsHeaderHolder(View view) {
            this.context = view.getContext();
            ButterKnife.bind(this, view);
        }

        public void bind(Spread spread) {

            Resources resources = context.getResources();

            int color = spread.isBullSpread()
                    ? resources.getColor(R.color.bull_spread_background)
                    : resources.getColor(R.color.bear_spread_background);

            header.setBackgroundColor(color);

            description.setText(String.format("%s %.2f/%.2f", spread.getSpreadType().toString(), spread.getBuy().getStrike(), spread.getSell().getStrike()));

            header.setTransitionName(getTransitionName(spread));
        }

        static public String getTransitionName(Spread spread) {
            return "header_" + spread.toString();
        }
    }

    public static class OptionQuoteHolder {
        private final Context context;

        @Bind(R.id.quantity)
        TextView textQuantity;

        @Bind(R.id.option_description)
        TextView textDescription;

        @Bind(R.id.bid)
        TextView textBid;

        @Bind(R.id.ask)
        TextView textAsk;

        public OptionQuoteHolder(Context context, View view) {
            this.context = context;
            ButterKnife.bind(this, view);
        }

        public void bind(int qty, Interfaces.OptionQuote optionQuote) {
            textQuantity.setText(String.valueOf(qty));
            textDescription.setText(String.format("%s %s %s",
                    Util.getFormattedOptionDate(optionQuote.getExpiration()),
                    Util.formatDollars(optionQuote.getStrike()),
                    optionQuote.getOptionType().toString()
            ));
            textBid.setText(Util.formatDollars(optionQuote.getBid()));
            textAsk.setText(Util.formatDollars(optionQuote.getAsk()));
        }
    }

    public static class BriefTradeDetailsHolder {

        private final Context context;

        @Bind(R.id.askPrice)
        TextView askPrice;

        @Bind(R.id.breakEvenPrice)
        TextView breakEvenPrice;

        @Bind(R.id.daysToExp)
        TextView daysToExp;

        @Bind(R.id.maxReturn)
        TextView maxReturn;

        @Bind(R.id.summary)
        TextView summary;

        @Bind(R.id.details_brief)
        ViewGroup layout;

        public BriefTradeDetailsHolder(View view) {
            this.context = view.getContext();
            ButterKnife.bind(this, view);
        }

        public void bind(Spread spread) {
            summary.setText(String.format("Returns %s/yr if %s is %s %s from the current price",
                    Util.formatPercentCompact(spread.getMaxReturnAnnualized()),             // Returns %s / yr
                    spread.getUnderlyingSymbol(),                                           // if %symbol
                    spread.isBullSpread()                                                   // "down less than" "up at least" "up less than" "down at least"
                            ? (spread.isInTheMoney_MaxReturn() ? "down less than" : "up at least")
                            : (spread.isInTheMoney_MaxReturn() ? "up less than" : "down at least"),
                    Util.formatPercentCompact(Math.abs(spread.getPercentChange_MaxProfit()))    // some percent
            ));

            askPrice.setText(Util.formatDollars(spread.getAsk()));
            breakEvenPrice.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            daysToExp.setText(Util.getFormattedOptionDate(spread.getExpiresDate()) + " / " + String.valueOf(spread.getDaysToExpiration()) + " days");
            maxReturn.setText(Util.formatDollars(spread.getMaxReturn()) + " / " + Util.formatPercentCompact(spread.getMaxPercentProfitAtExpiration()));

            Resources resources = context.getResources();

            int color = spread.isInTheMoney_BreakEven()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.material_red_900);

            breakEvenPrice.setTextColor(color);

            layout.setTransitionName(getTransitionName(spread));
        }

        static public String getTransitionName(Spread spread) {
            return "details_" + spread;
        }
    }

}
