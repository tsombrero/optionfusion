package com.optionfusion.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.widgets.PriceChangeView;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SharedViewHolders {

    public static class StockInfoHolder {

        @Bind(R.id.ticker)
        TextView tickerView;

        @Bind(R.id.price)
        TextView priceView;

        @Bind(R.id.equityDescription)
        TextView equityDescriptionView;

        public StockInfoHolder(View view) {
            ButterKnife.bind(this, view);
        }

        public void bind(Interfaces.StockQuote stockQuote) {
            if (stockQuote == null)
                return;

            tickerView.setText(stockQuote.getSymbol());
            priceView.setText(Util.formatDollars(stockQuote.getLast()));
            equityDescriptionView.setText(stockQuote.getDescription());
        }

        static public String getTransitionName(String symbol) {
            return "stockinfo_" + symbol;
        }
    }

    public static class StockQuoteViewHolder extends RecyclerView.ViewHolder {
        private final View view;

        @Bind(R.id.ticker)
        TextView tickerView;

        @Bind(R.id.price)
        TextView priceView;

        @Bind(R.id.change)
        PriceChangeView changeView;

        @Bind(R.id.description)
        @Nullable
        TextView descriptionView;

        StockQuoteViewConfig stockQuoteViewConfig;
        Interfaces.StockQuote stockQuote;

        public StockQuoteViewHolder(final View itemView, final StockQuoteViewConfig stockQuoteViewConfig, final SymbolSelectedListener symbolSelectedListener, EventBus bus) {
            super(itemView);
            this.view = itemView;
            this.stockQuoteViewConfig = stockQuoteViewConfig;

            if (this.stockQuoteViewConfig == null)
                this.stockQuoteViewConfig = new StockQuoteViewConfig();

            ButterKnife.bind(this, view);
            changeView.observe(bus);

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

            if (stockQuoteViewConfig != null) {
                changeView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (stockQuoteViewConfig != null)
                            stockQuoteViewConfig.setShowAsPercentage(!stockQuoteViewConfig.isShowAsPercentage());
                    }
                });
            }

            if (symbolSelectedListener != null) {
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (stockQuote != null)
                            symbolSelectedListener.onSymbolSelected(stockQuote.getSymbol());
                    }
                });
            }
        }

        public void bind(Interfaces.StockQuote stockQuote) {
            this.stockQuote = stockQuote;

            if (stockQuote != null)
                tickerView.setText(stockQuote.getSymbol());
            else
                tickerView.setText("");

            if (stockQuote != null && stockQuote.getProvider() != OptionFusionApplication.Provider.DUMMY) {
                priceView.setText(Util.formatDollars(stockQuote.getLast(), 100000));
                if (descriptionView != null)
                    descriptionView.setText(stockQuote.getDescription());
            } else {
                priceView.setText("");
                descriptionView.setText("");
            }
            changeView.setStockQuote(stockQuote);
            changeView.setShowAsPercentage(stockQuoteViewConfig.isShowAsPercentage());
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

    public static class StockQuoteViewConfig {
        boolean showAsPercentage;

        public boolean isShowAsPercentage() {
            return showAsPercentage;
        }

        public void setShowAsPercentage(boolean showAsPercentage) {
            if (this.showAsPercentage == showAsPercentage)
                return;

            this.showAsPercentage = showAsPercentage;
            onConfigChanged();
        }

        public void onConfigChanged() {
        }

    }

    public interface SymbolSelectedListener {
        void onSymbolSelected(String symbol);
    }

    public static class TradeDetailsHeaderHolder {
        private final Context context;

        @Bind(R.id.header)
        View header;

        @Bind(R.id.trade_description)
        TextView description;

        @Bind(R.id.trade_expiration)
        TextView expiration;

        @Bind(R.id.trade_strikes)
        TextView strikes;

        public TradeDetailsHeaderHolder(View view) {
            this.context = view.getContext();
            ButterKnife.bind(this, view);
        }

        public void bind(VerticalSpread spread) {

            Resources resources = context.getResources();

            int color = spread.isBullSpread()
                    ? resources.getColor(R.color.bull_spread_background)
                    : resources.getColor(R.color.bear_spread_background);

            header.setBackgroundColor(color);

            description.setText(spread.getSpreadType().toString());
            expiration.setText(Util.getFormattedOptionDate(spread.getExpiresDate()));
            strikes.setText(String.format("%.2f/%.2f", spread.getBuyStrike(), spread.getSellStrike()));

            header.setTransitionName(getTransitionName(spread));

        }

        static public String getTransitionName(VerticalSpread spread) {
            return "header_" + spread.getDescription();
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

        public void bind(VerticalSpread spread) {
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

        public void bind(VerticalSpread spread) {
            summary.setText(String.format("Returns %s/yr if %s is %s %s from the current price",
                    Util.formatPercentCompact(spread.getMaxReturnAnnualized()),             // Returns %s / yr
                    spread.getUnderlyingSymbol(),                                           // if %symbol
                    spread.isBullSpread()                                                   // "down less than" "up at least" "up less than" "down at least"
                            ? (spread.isInTheMoney_MaxReturn() ? "down less than" : "up at least")
                            : (spread.isInTheMoney_MaxReturn() ? "up less than" : "down at least"),
                    Util.formatPercentCompact(Math.abs(spread.getPercentChange_MaxProfit()))    // some percent
            ));

            textTradeCost.setText(Util.formatDollars(spread.getCapitalAtRisk()));
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

        static public String getTransitionName(VerticalSpread spread) {
            return "details_" + spread.getDescription();
        }
    }

}
