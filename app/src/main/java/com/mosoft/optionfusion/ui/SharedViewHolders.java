package com.mosoft.optionfusion.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.util.Util;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SharedViewHolders {

    public static class StockInfoHolder {

        @Bind(R.id.symbol)
        TextView symbolView;

        @Bind(R.id.price)
        TextView priceView;

        @Bind(R.id.equityDescription)
        TextView equityDescriptionView;

        public StockInfoHolder(View view) {
            ButterKnife.bind(this, view);
        }

        public void bind(Interfaces.OptionChain oc) {
            if (oc == null)
                return;

            symbolView.setText(oc.getUnderlyingStockQuote().getSymbol());
            priceView.setText(Util.formatDollars(oc.getUnderlyingStockQuote().getLast()));
            equityDescriptionView.setText(oc.getUnderlyingStockQuote().getDescription());
        }

        static public String getTransitionName(String symbol) {
            return "stockinfo_" + symbol;
        }
    }

    public static class StockQuoteViewHolder extends RecyclerView.ViewHolder {
        private final View view;

        @Bind(R.id.symbol)
        TextView symbolView;

        @Bind(R.id.price)
        TextView priceView;

        @Bind(R.id.change)
        TextView changeView;

        @Bind(R.id.arrow)
        @Nullable
        ViewFlipper arrowViewFlipper;

        @Bind(R.id.description)
        @Nullable
        TextView description;

        StockQuoteViewHolderListener stockQuoteViewHolderListener;
        Interfaces.StockQuote stockQuote;

        static boolean showChangeAsPercent;

        public StockQuoteViewHolder(final View itemView, final StockQuoteViewHolderListener stockQuoteViewHolderListener) {
            super(itemView);
            this.view = itemView;
            this.stockQuoteViewHolderListener = stockQuoteViewHolderListener;
            ButterKnife.bind(this, view);

            itemView.post(new Runnable() {
                @Override
                public void run() {
                    Rect changeViewHitRect = new Rect();
                    changeView.getHitRect(changeViewHitRect);
                    changeViewHitRect.top = 0;
                    changeViewHitRect.bottom = itemView.getHeight();
                    changeViewHitRect.left = changeView.getLeft() - 50;
                    changeViewHitRect.right = itemView.getWidth();

                    TouchDelegate touchDelegate = new TouchDelegate(changeViewHitRect, changeView);
                    itemView.setTouchDelegate(touchDelegate);
                }
            });

            if (stockQuoteViewHolderListener != null) {
                changeView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showChangeAsPercent = !showChangeAsPercent;
                        stockQuoteViewHolderListener.onTogglePriceChangeFormat();
                    }
                });

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stockQuoteViewHolderListener.onSymbolSelected(stockQuote.getSymbol());
                    }
                });
            }
        }

        public void bind(Interfaces.StockQuote stockQuote) {
            this.stockQuote = stockQuote;
            Double change = stockQuote.getChange();
            symbolView.setText(stockQuote.getSymbol());
            priceView.setText(Util.formatDollars(stockQuote.getLast(), 100000));

            if (showChangeAsPercent) {
                changeView.setText(Util.formatPercent(stockQuote.getChangePercent()));
            } else {
                changeView.setText(Util.formatDollarChange(stockQuote.getChange()));
            }
//            view.setTransitionName(getTransitionName());
            if (arrowViewFlipper != null)
                arrowViewFlipper.setDisplayedChild(change > 0 ? 1 : 0);
            if (description != null)
                description.setText(stockQuote.getDescription());
        }

        private String getTransitionName() {
            return "stockquote_" + stockQuote.getSymbol();
        }

        public Interfaces.StockQuote getStockQuote() {
            return stockQuote;
        }

        public String getSymbol() {
            if (stockQuote != null)
                return stockQuote.getSymbol();

            return null;
        }
    }

    public interface StockQuoteViewHolderListener {
        void onTogglePriceChangeFormat();

        void onSymbolSelected(String symbol);
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

            description.setText(spread.getDescriptionNoExp());

            header.setTransitionName(getTransitionName(spread));
        }

        static public String getTransitionName(Spread spread) {
            return "header_" + spread.toString();
        }
    }

    public static class OptionLegHolder {
        @Bind(R.id.transaction_type)
        TextView textTransactionType;

        @Bind(R.id.quantity)
        TextView textQuantity;

        @Bind(R.id.option_type)
        TextView textOptionType;

        @Bind(R.id.option_strike)
        TextView textOptionStrike;

        @Bind(R.id.option_exp)
        TextView textOptionExp;

        public OptionLegHolder(View view) {
            ButterKnife.bind(this, view);
        }

        public void bind(int qty, Interfaces.OptionQuote optionQuote) {
            textTransactionType.setText(qty > 0 ? "Buy" : "Sell");
            textQuantity.setText(String.valueOf(Math.abs(qty)));
            textOptionType.setText(optionQuote.getOptionType().toString());
            textOptionStrike.setText(Util.formatDollars(optionQuote.getStrike()));
            textOptionExp.setText(Util.getFormattedOptionDate(optionQuote.getExpiration()));
        }
    }

    public static class OptionTradeBidAskHolder {
        @Bind(R.id.trade_cost)
        TextView bidAsk;

        public OptionTradeBidAskHolder(View view) {
            ButterKnife.bind(this, view);
        }

        public void bind(Spread spread) {
            bidAsk.setText(String.format("%s / %s", Util.formatDollars(spread.getBid()), Util.formatDollars(spread.getAsk())));
        }
    }

    public static class BriefTradeDetailsHolder {

        private final Context context;

        @Bind(R.id.trade_cost)
        TextView textTradeCost;

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

            textTradeCost.setText(Util.formatDollars(spread.getAsk()));
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
