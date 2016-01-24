package com.optionfusion.ui.tradedetails;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.optionfusion.cache.StockQuoteProvider;

import javax.inject.Inject;

import lecho.lib.hellocharts.view.LineChartView;

public class LineChartViewHolder extends RecyclerView.ViewHolder {

    LineChartView lineChartView;

    @Inject
    StockQuoteProvider stockQuoteProvider;

    public LineChartViewHolder(View itemView) {
        super(itemView);
        lineChartView = ((LineChartView)itemView);
        ViewGroup.LayoutParams lp = lineChartView.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
    }


    public void bind(String symbol) {

    }
}
