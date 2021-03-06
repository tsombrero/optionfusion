package com.optionfusion.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.R;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.client.FusionClient;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.jobqueue.SetUserDataJob;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.widgets.PriceChangeView;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SharedViewHolders {

    private static final String TAG = "SharedViewHolders";

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

    public abstract static class StockQuoteListViewHolder extends RecyclerView.ViewHolder {

        protected final View view;

        public StockQuoteListViewHolder(View view) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);
        }
        public abstract void bind(Interfaces.StockQuote stockQuote);
    }

    public static class StockQuoteViewHolder extends StockQuoteListViewHolder {

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
            this.stockQuoteViewConfig = stockQuoteViewConfig;

            if (this.stockQuoteViewConfig == null)
                this.stockQuoteViewConfig = new StockQuoteViewConfig();

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
        private final SpreadFavoriteListener spreadFavoriteListener;

        @Bind(R.id.header)
        View header;

        @Bind(R.id.trade_description)
        TextView description;

        @Bind(R.id.trade_expiration)
        TextView expiration;

        @Bind(R.id.trade_strikes)
        TextView strikes;

        @Bind(R.id.star)
        ImageView star;
        private VerticalSpread spread;

        public TradeDetailsHeaderHolder(View view, SpreadFavoriteListener spreadFavoriteListener) {
            this.spreadFavoriteListener = spreadFavoriteListener;
            this.context = view.getContext();
            ButterKnife.bind(this, view);
        }

        public void bind(VerticalSpread spread) {
            this.spread = spread;

            Resources resources = context.getResources();

            int color = spread.isBullSpread()
                    ? resources.getColor(R.color.bull_spread_background)
                    : resources.getColor(R.color.bear_spread_background);

            header.setBackgroundColor(color);

            description.setText(spread.getSpreadType().toString());
            expiration.setText(Util.getFormattedOptionDate(spread.getExpiresDate()));

            strikes.setText(String.format("%.2f/%.2f", spread.getBuyStrike(), spread.getSellStrike()).replaceAll("\\.00", ""));

            if (Build.VERSION.SDK_INT >= 21)
                header.setTransitionName(getTransitionName(spread));

            bindStar();
        }

        private void bindStar() {
            star.setImageDrawable(AppCompatResources.getDrawable(context,
                    spread.isFavorite()
                            ? R.drawable.ic_star_gold_v_24dp
                            : R.drawable.ic_star_border_gray_24dp));
        }

        @OnClick(R.id.star)
        public void onClickStar() {
            boolean isFavorite = !spread.isFavorite();
            Log.d(TAG, "newIsFavorite : " + isFavorite);
            spread.setIsFavorite(isFavorite);
            spreadFavoriteListener.setFavorite(spread, isFavorite);
            bindStar();
        }


        static public String getTransitionName(VerticalSpread spread) {
            return "header_" + spread.getDescription();
        }
    }

    public interface SpreadFavoriteListener {
        void setFavorite(VerticalSpread spread, boolean isFavorite);
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
            summary.setText(String.format("Returns %s if %s is %s %s from the current price",
                    getPeriodicRoi(spread),                                                 // Returns n/yr or n/mo
                    spread.getUnderlyingSymbol(),                                           // if %symbol
                    spread.isBullSpread()                                                   // "down less than" "up at least" "up less than" "down at least"
                            ? (spread.isInTheMoney_MaxReturn() ? "down less than" : "up at least")
                            : (spread.isInTheMoney_MaxReturn() ? "up less than" : "down at least"),
                    Util.formatPercentCompact(Math.abs(spread.getPercentChange_MaxProfit()))    // some percent
            ));

            textTradeCost.setText(Util.formatDollars(spread.getCapitalAtRisk()));
            breakEvenPrice.setText(Util.formatDollars(spread.getPrice_BreakEven()));
            daysToExp.setText(String.format(Locale.US, context.getString(R.string.days_to_exp_format), Util.getFormattedOptionDate(spread.getExpiresDate()), spread.getDaysToExpiration()));
            maxReturn.setText(String.format(Locale.US, "%s / %s", Util.formatDollars(spread.getMaxReturn()), Util.formatPercentCompact(spread.getMaxPercentProfitAtExpiration())));

            Resources resources = context.getResources();

            int color = spread.isInTheMoney_BreakEven()
                    ? resources.getColor(R.color.primary_text)
                    : resources.getColor(R.color.material_red_900);

            breakEvenPrice.setTextColor(color);

            if (Build.VERSION.SDK_INT >= 21)
                layout.setTransitionName(getTransitionName(spread));
        }

        static public String getTransitionName(VerticalSpread spread) {
            return "details_" + spread.getDescription();
        }
    }

    private static String getPeriodicRoi(VerticalSpread spread) {
        if (spread.getMaxReturnAnnualized() > Util.MAX_PERCENT_NORMAL_FORMAT)
            return Util.formatPercentCompact(spread.getMaxReturnMonthly()) + "/mo";

        return Util.formatPercentCompact(spread.getMaxReturnAnnualized()) + "/yr";
    }

    public static class MarketDataTimestampHeaderViewHolder extends StockQuoteListViewHolder {

        private final FragmentManager fragmentManager;
        private final JobManager jobManager;
        private final ClientInterfaces.AccountClient accountClient;
        @Bind(R.id.info)
        ImageView info;

        @Bind(R.id.market_data_timestamp)
        TextView textView;

        public MarketDataTimestampHeaderViewHolder(View v, FragmentManager fragmentManager, JobManager jobManager, ClientInterfaces.AccountClient accountClient) {
            super(v);
            this.fragmentManager = fragmentManager;
            this.jobManager = jobManager;
            this.accountClient = accountClient;
            ButterKnife.bind(this, v);
        }

        @Override
        public void bind(Interfaces.StockQuote stockQuote) {
            DateTime dateTime = new DateTime(stockQuote.getQuoteTimestamp());
            String dOw = dateTime.dayOfWeek().getAsText();
            String format = info.getContext().getResources().getString(R.string.prices_as_of_market_close_s);
            textView.setText(String.format(format, dOw));
        }

        @OnClick(R.id.info)
        public void onShowInfo() {
            ShowInfoDialog dialog = new ShowInfoDialog();
            dialog.jobManager = this.jobManager;
            dialog.isNotifySet = TextUtils.equals(FusionClient.USERDATA_TRUE, accountClient.getUserData(FusionClient.USERDATA_NOTIFY_UPGRADE));
            dialog.show(fragmentManager, null);
        }

        public static class ShowInfoDialog extends DialogFragment {
            @Bind(R.id.checkbox)
            CheckBox checkbox;
            private boolean checkboxDirty;
            public JobManager jobManager;
            public boolean isNotifySet;

            @Override
            public void onCreate(@Nullable Bundle savedInstanceState) {
                setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogTheme);
                super.onCreate(savedInstanceState);
            }

            @Nullable
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View v = inflater.inflate(R.layout.dialog_why_eod, container, false);
                ButterKnife.bind(this, v);
                checkbox.setChecked(isNotifySet);
                return v;
            }

            @OnClick(R.id.checkbox)
            public void onClickCheckbox() {
                checkboxDirty = true;
            }

            @OnClick(R.id.submit)
            public void onOkClicked() {
                dismiss();
            }

            @Override
            public void onDismiss(DialogInterface dialog) {
                super.onDismiss(dialog);
                if (checkboxDirty && jobManager != null) {
                    jobManager.addJobInBackground(new SetUserDataJob(FusionClient.USERDATA_NOTIFY_UPGRADE, checkbox.isChecked() ? FusionClient.USERDATA_TRUE : null));
                }
            }
        }
    }
}
