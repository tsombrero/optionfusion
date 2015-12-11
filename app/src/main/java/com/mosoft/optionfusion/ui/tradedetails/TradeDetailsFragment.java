package com.mosoft.optionfusion.ui.tradedetails;

import android.app.Fragment;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.SharedViewHolders;
import com.mosoft.optionfusion.ui.widgets.VerticalTextView;
import com.mosoft.optionfusion.util.Util;

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

public class TradeDetailsFragment extends Fragment {

    @Bind(R.id.stock_quote)
    protected ViewGroup stockQuote;

    @Bind(R.id.details_brief)
    protected ViewGroup briefDetailsLayout;

    @Bind(R.id.header)
    protected ViewGroup spreadHeaderLayout;

    @Bind(R.id.pl_chart)
    protected LineChartView plChart;

    @Bind(R.id.label_xaxis)
    protected TextView labelX;

    @Bind(R.id.label_yaxis)
    protected VerticalTextView labelY;

    @Bind(R.id.trade_container)
    protected LinearLayout tradeContainer;

    @BindColor(R.color.secondary_text)
    protected int axisColor;

    @BindColor(R.color.primary_text)
    protected int axisTextColor;

    @BindColor(R.color.primary)
    protected int lineColor;

    @BindColor(R.color.rangebar_red)
    protected int zeroLineColor;

    @BindColor(R.color.primary_text)
    protected int currentPriceColor;

    @Inject
    OptionChainProvider optionChainProvider;

    Spread spread;

    private static final String ARG_TRADE = "trade";
    private Interfaces.OptionChain oc;

    public static TradeDetailsFragment newInstance(Spread spread) {
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

        return ret;
    }

    @Override
    public void onResume() {
        super.onResume();
        spread = getArguments().getParcelable(ARG_TRADE);
        initView();
    }

    public void initView() {
        oc = optionChainProvider.get(spread.getUnderlyingSymbol());

        new SharedViewHolders.StockQuoteViewHolder(stockQuote).bind(oc.getUnderlyingStockQuote());
        new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout).bind(spread);
        new SharedViewHolders.TradeDetailsHeaderHolder(spreadHeaderLayout).bind(spread);

        spreadHeaderLayout.setElevation(stockQuote.getElevation());
        spreadHeaderLayout.findViewById(R.id.item_menu).setVisibility(View.GONE);

        View buyLayout = getActivity().getLayoutInflater().inflate(R.layout.incl_option_quote, null);
        new SharedViewHolders.OptionQuoteHolder(getActivity(), buyLayout).bind(1, spread.getBuy());
        tradeContainer.addView(buyLayout);

        View sellLayout = getActivity().getLayoutInflater().inflate(R.layout.incl_option_quote, null);
        new SharedViewHolders.OptionQuoteHolder(getActivity(), sellLayout).bind(-1, spread.getSell());
        tradeContainer.addView(sellLayout);

        initTradeProfitChart();
    }

    private void initTradeProfitChart() {
        List<PointValue> values = new ArrayList<>();
        values.add(new PointValue((float) spread.getPrice_MaxLoss(), -1f * (float) spread.getAsk()).setLabel(""));
        values.add(new PointValue((float) spread.getPrice_BreakEven(), 0f).setLabel(""));
        values.add(new PointValue((float) spread.getPrice_MaxReturn(), (float) spread.getMaxReturn()).setLabel(Util.formatDollars(spread.getMaxReturn()) + " / " + Util.formatPercentCompact(spread.getMaxPercentProfitAtExpiration())));

        float lastPrice = (float) oc.getUnderlyingStockQuote().getLast();

        if (spread.isBullSpread()) {
            values.add(new PointValue(lastPrice * 100f, (float) spread.getMaxReturn()).setLabel(""));
        } else {
            values.add(new PointValue(0f, (float) spread.getMaxReturn()).setLabel(""));
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
                .setHasPoints(true)
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
        currentPriceValues.add(new PointValue(lastPrice, (float) (spread.getMaxReturn() * 2f)));
        currentPriceValues.add(new PointValue(lastPrice, (float) (spread.getAsk() * -1f)));
        Line currentPriceLine = new Line(currentPriceValues)
                .setColor(currentPriceColor)
                .setFilled(false)
                .setHasLabels(false)
                .setHasLines(true)
                .setHasPoints(false)
                .setStrokeWidth(1);

        currentPriceLine.setPathEffect(new DashPathEffect(new float[]{14f, 12f}, 0f));

        Axis xAxis = new Axis()
                .setHasLines(true)
                .setLineColor(axisColor)
                .setTextColor(axisTextColor)
                .setHasSeparationLine(true)
                .setFormatter(new SimpleAxisValueFormatter().setPrependedText("$".toCharArray()))
                .setAutoGenerated(true);

        Axis yAxis = new Axis()
                .setHasLines(true)
                .setLineColor(axisColor)
                .setTextColor(axisTextColor)
                .setHasSeparationLine(true)
                .setAutoGenerated(true)
                .setFormatter(new SimpleAxisValueFormatter().setPrependedText("$".toCharArray()));

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

        float yViewRange = (float) Math.abs(spread.getMaxReturn() * 1.2f);

        final Viewport v = new Viewport(plChart.getMaximumViewport());
        v.top = yViewRange;
        v.bottom = yViewRange * -1f;
        v.left = (float) (spread.getPrice_BreakEven() - xViewRange);
        v.right = (float) (spread.getPrice_BreakEven() + xViewRange);

        v.left = (float) Math.max(0f, Math.min(oc.getUnderlyingStockQuote().getLast() * 0.98f, v.left));
        v.right = (float) Math.max(oc.getUnderlyingStockQuote().getLast() * 1.02f, v.right);

        v.bottom = (float) Math.max(v.bottom, Math.max(0f, spread.getAsk()) * -1f);

        plChart.setMaxZoom(Float.MAX_VALUE);
        plChart.setMaximumViewport(v);
        plChart.setCurrentViewport(v);

        labelX.setText(oc.getUnderlyingStockQuote().getSymbol() + " SHARE PRICE");
        labelY.setText("PROFIT AT EXP");
    }
}
