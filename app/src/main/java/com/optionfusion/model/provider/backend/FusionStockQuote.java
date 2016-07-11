package com.optionfusion.model.provider.backend;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.StockQuote;
import com.optionfusion.common.protobuf.OptionChainProto;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

public class FusionStockQuote implements Interfaces.StockQuote {

    private String symbol, equityDescription;
    private long createTimestamp = System.currentTimeMillis();
    private long dataTimestamp;

    private double open, close, previousClose;


    public FusionStockQuote(Equity equity) {
        StockQuote stockQuote = equity.getEodStockQuote();
        equityDescription = equity.getDescription();
        symbol = equity.getSymbol();

        if (stockQuote != null) {
            dataTimestamp = stockQuote.getDataTimestamp();
            open = stockQuote.getOpen();
            close = stockQuote.getClose();
            previousClose = stockQuote.getPreviousClose();
        }
    }

    public FusionStockQuote(OptionChainProto.OptionChain protoChain) {
        symbol = protoChain.getSymbol();
        equityDescription = protoChain.getSymbol();
        dataTimestamp = protoChain.getTimestamp();
        open = protoChain.getUnderlyingPrice();
        close = protoChain.getUnderlyingPrice();

    }

    public FusionStockQuote(Parcel in) {
        symbol = in.readString();
        equityDescription = in.readString();
        createTimestamp = in.readLong();
        dataTimestamp = in.readLong();
        open = in.readDouble();
        close = in.readDouble();
        previousClose = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(symbol);
        dest.writeString(equityDescription);
        dest.writeLong(createTimestamp);
        dest.writeLong(dataTimestamp);
        dest.writeDouble(open);
        dest.writeDouble(close);
        dest.writeDouble(previousClose);
    }


    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getDescription() {
        return equityDescription;
    }

    @Override
    public double getBid() {
        return close;
    }

    @Override
    public double getAsk() {
        return close;
    }

    @Override
    public double getLast() {
        return close;
    }

    @Override
    public double getOpen() {
        return open;
    }

    @Override
    public double getClose() {
        return close;
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.OPTION_FUSION_BACKEND;
    }

    @Override
    public Double getChange() {
        return getClose() - previousClose;
    }

    @Override
    public Double getChangePercent() {
        return getChange() / previousClose;
    }

    @Override
    public long getLastUpdatedLocalTimestamp() {
        return createTimestamp;
    }

    @Override
    public long getQuoteTimestamp() {
        return dataTimestamp;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Creator
    public static final Parcelable.Creator CREATOR
            = new Parcelable.Creator() {
        public FusionStockQuote createFromParcel(Parcel in) {
            return new FusionStockQuote(in);
        }

        public FusionStockQuote[] newArray(int size) {
            return new FusionStockQuote[size];
        }
    };

}
