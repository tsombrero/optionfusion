package com.optionfusion.ui.widgets;

import android.content.Context;
import android.support.annotation.MainThread;
import android.util.AttributeSet;

import com.optionfusion.R;
import com.optionfusion.events.StockQuotesUpdatedEvent;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class PriceChangeView extends AutoFitTextView {
    EventBus bus;

    Interfaces.StockQuote stockQuote;

    private boolean showAsPercentage;

    public PriceChangeView(Context context) {
        super(context);
    }

    public PriceChangeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PriceChangeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public synchronized void observe(EventBus bus) {
        if (this.bus == null) {
            this.bus = bus;
            bus.register(this);
        }
    }

    @Override
    protected synchronized void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bus != null)
            bus.unregister(this);
        bus = null;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StockQuotesUpdatedEvent quotesUpdatedEvent) {
        if (stockQuote == null)
            return;

        Interfaces.StockQuote sq = quotesUpdatedEvent.getStockQuote(stockQuote.getSymbol());
        update();
    }

    @MainThread
    private void update() {
        if (showAsPercentage) {
            setText(Util.formatPercent(stockQuote.getChangePercent()));
        } else {
            setText(Util.formatDollarChange(stockQuote.getChange()));
        }
    }

    public void setShowAsPercentage(boolean showAsPercentage) {
        this.showAsPercentage = showAsPercentage;
        update();
    }

    public void setStockQuote(Interfaces.StockQuote stockQuote) {
        this.stockQuote = stockQuote;
        update();
    }

    private static final int[] STATE_VALUE_DECREASED = new int[]{R.attr.state_value_decreased};

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (getText() != null
                && getText().toString().indexOf('-') >= 0) {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
            mergeDrawableStates(drawableState, STATE_VALUE_DECREASED);
            return drawableState;
        }
        return super.onCreateDrawableState(extraSpace);
    }


}
