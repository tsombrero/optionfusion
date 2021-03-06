package com.optionfusion.ui.tradedetails;

import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.R;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.jobqueue.SetFavoriteJob;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.util.PercentAxisValueFormatter;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.BindColor;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class TradeDetailsFragment extends Fragment implements SharedViewHolders.SpreadFavoriteListener {

    @Bind(R.id.stock_quote)
    protected ViewGroup stockQuoteLayout;

    @Bind(R.id.details_brief)
    protected ViewGroup briefDetailsLayout;

    @Bind(R.id.pl_chart)
    protected LineChartView plChart;

    @Bind(R.id.trade_container)
    protected LinearLayout tradeContainer;

    @Bind(R.id.toolbar)
    protected Toolbar toolbar;

    @Bind(R.id.header)
    protected ViewGroup heaader;

    @Bind(R.id.max_loss_price)
    protected TextView textMaxLoss;

    @BindColor(R.color.secondary_text)
    protected int axisColor;

    @BindColor(R.color.primary_text)
    protected int axisTextColor;

    @BindColor(R.color.primary)
    protected int lineColor;

    @BindColor(R.color.rangebar_red)
    protected int zeroLineColor;

    @BindColor(R.color.accent)
    protected int currentPriceColor;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    @Inject
    EventBus bus;

    @Inject
    JobManager jobManager;

    VerticalSpread spread;

    private static final String ARG_TRADE = "trade";
    private Interfaces.StockQuote stockQuote;

    public static TradeDetailsFragment newInstance(VerticalSpread spread) {
        TradeDetailsFragment ret = new TradeDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_TRADE, spread);
        ret.setArguments(args);
        return ret;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_full_trade_details, container, false);
        ButterKnife.bind(this, ret);

        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        spread = getArguments().getParcelable(ARG_TRADE);
        stockQuote = stockQuoteProvider.get(spread.getUnderlyingSymbol());
        initView();
    }

    public void initView() {
        new SharedViewHolders.StockQuoteViewHolder(stockQuoteLayout, null, null, bus).bind(stockQuote);
        new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout).bind(spread);

        if (tradeContainer.getChildCount() <= 1) {
                View buyLayout = getActivity().getLayoutInflater().inflate(R.layout.incl_option_quote, null);
            new SharedViewHolders.OptionLegHolder(buyLayout).bind(1, spread.getBuy());
            tradeContainer.addView(buyLayout);

            View sellLayout = getActivity().getLayoutInflater().inflate(R.layout.incl_option_quote, null);
            new SharedViewHolders.OptionLegHolder(sellLayout).bind(-1, spread.getSell());
            tradeContainer.addView(sellLayout);

            View totalLayout = getActivity().getLayoutInflater().inflate(R.layout.incl_trade_quote_total, null);
            new SharedViewHolders.OptionTradeBidAskHolder(totalLayout).bind(spread);
            tradeContainer.addView(totalLayout);
        }
        textMaxLoss.setText((spread.isBullSpread() ? "Below " : "Above ") + Util.formatDollars(spread.getBuy().getStrike()));

        initTradeProfitChart();

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Trade Details");
        new SharedViewHolders.TradeDetailsHeaderHolder(heaader, this).bind(spread);
    }

    private void initTradeProfitChart() {
        List<PointValue> values = new ArrayList<>();
        values.add(new PointValue((float) spread.getPrice_MaxLoss(), -1f).setLabel(""));
        values.add(new PointValue((float) spread.getPrice_BreakEven(), 0f).setLabel(""));
        values.add(new PointValue((float) spread.getPrice_MaxReturn(), (float) spread.getMaxPercentProfitAtExpiration()).setLabel(""));

        float lastPrice = (float) stockQuote.getLast();

        if (spread.isBullSpread()) {
            values.add(new PointValue(lastPrice * 100f, (float) spread.getMaxPercentProfitAtExpiration()).setLabel(""));
            values.add(new PointValue(0f, -1f).setLabel(""));
        } else {
            values.add(new PointValue(0f, (float) spread.getMaxPercentProfitAtExpiration()).setLabel(""));
            values.add(new PointValue(lastPrice * 100f, -1f).setLabel(""));
        }

        Collections.sort(values, new Comparator<PointValue>() {
            @Override
            public int compare(PointValue lhs, PointValue rhs) {
                return Float.compare(lhs.getX(), rhs.getX());
            }
        });

        Line line = new Line(values)
                .setColor(lineColor)
                .setFilled(false)
                .setCubic(false)
                .setHasLabels(true)
                .setHasPoints(false)
                .setStrokeWidth(3)
                .setHasLines(true);

        List<PointValue> zeroLineValues = new ArrayList<>();
        zeroLineValues.add(new PointValue(0, 0));
        zeroLineValues.add(new PointValue(values.get(values.size() - 1).getX() * 2f, 0));

        Line zeroLine = new Line(zeroLineValues)
                .setColor(zeroLineColor)
                .setHasLabels(false)
                .setHasPoints(false)
                .setHasLines(true)
                .setStrokeWidth(0)
                .setFilled(true)
                .setAreaTransparency(0x30);

        List<PointValue> currentPriceValues = new ArrayList<>();
        currentPriceValues.add(new PointValue(lastPrice, (float) (spread.getMaxPercentProfitAtExpiration() * 2f)));
        currentPriceValues.add(new PointValue(lastPrice, -1f));
        Line currentPriceLine = new Line(currentPriceValues)
                .setColor(currentPriceColor)
                .setFilled(false)
                .setHasLabels(false)
                .setHasLines(true)
                .setHasPoints(false)
                .setStrokeWidth(4);

        currentPriceLine.setPathEffect(new DashPathEffect(new float[]{20f, 20f}, 0f));

        Axis xAxis = new Axis()
                .setHasLines(true)
                .setLineColor(axisColor)
                .setTextColor(axisTextColor)
                .setHasSeparationLine(true)
                .setFormatter(new SimpleAxisValueFormatter().setPrependedText("$".toCharArray()))
                .setName(spread.getUnderlyingSymbol() + " price on " + Util.getFormattedOptionDateCompact(spread.getExpiresDate()))
                .setAutoGenerated(true);

        Axis yAxis = new Axis()
                .setHasLines(true)
                .setLineColor(axisColor)
                .setTextColor(axisTextColor)
                .setHasSeparationLine(true)
                .setAutoGenerated(true)
                .setFormatter(new PercentAxisValueFormatter());

        List<Line> lines = new ArrayList<>();
        lines.add(zeroLine);
        lines.add(currentPriceLine);
        lines.add(line);

        LineChartData data = new LineChartData();
        data.setLines(lines);
        data.setAxisXBottom(xAxis);
        data.setAxisYLeft(yAxis);
        data.setBaseValue(Float.NEGATIVE_INFINITY);

        plChart.setLineChartData(data);
        plChart.setInteractive(false);

        float xViewRange = (float) Math.abs((spread.getPrice_MaxReturn() - spread.getPrice_BreakEven()) * 2f);
        xViewRange = (float) Math.max(xViewRange, Math.abs(lastPrice - (spread.getPrice_BreakEven())) * 1.01f);

        float yViewRange = (float) Math.abs(spread.getMaxPercentProfitAtExpiration() * 1.2f);

        final Viewport v = new Viewport(plChart.getMaximumViewport());
        v.top = yViewRange;
        v.bottom = yViewRange * -1f;
        v.left = (float) (spread.getPrice_BreakEven() - xViewRange);
        v.right = (float) (spread.getPrice_BreakEven() + xViewRange);

        v.left = (float) Math.max(0f, Math.min(stockQuote.getLast() * 0.98f, v.left));
        v.right = (float) Math.max(stockQuote.getLast() * 1.02f, v.right);

        v.bottom = v.bottom / 2;

//        v.bottom = (float) Math.max(v.bottom, Math.max(0f, spread.getAsk()) * -1f);

        plChart.setMaxZoom(Float.MAX_VALUE);
        plChart.setMaximumViewport(v);
        plChart.setCurrentViewport(v);
    }

    @Override
    public void setFavorite(VerticalSpread spread, boolean isFavorite) {
        spread.setIsFavorite(isFavorite);
        if (spread instanceof DbSpread)
            jobManager.addJobInBackground(new SetFavoriteJob(getActivity(), (DbSpread) spread, isFavorite));
    }
}
