package com.optionfusion.backend.models;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.optionfusion.backend.utils.TextUtils;

@Entity
public class StockQuote {
    public static final String DATA_TIMESTAMP = "timestamp";
    @Id
    long timestamp;

    @Parent
    @Load
    Ref<Equity> equity;

    String ticker;

    long volume;
    double open, hi, lo, close;

    // not present in eod data
    double bid, ask;

    double previousClose;

    public StockQuote() {
    }

    public StockQuote(String ticker, long timestamp) {
        setEquity(ticker);
        this.timestamp = timestamp;
        this.ticker = ticker;
    }

    public long getDataTimestamp() {
        return timestamp;
    }

    public String getTicker() {
        if (TextUtils.isEmpty(ticker)) {
            ticker = getEquity().getTicker();
        }
        return ticker;
    }

    public Equity getEquity() {
        if (equity == null)
            return null;

        return equity.get();
    }

    public void setEquity(String ticker) {
        this.equity = Ref.create(Key.create(Equity.class, ticker));
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHi() {
        return hi;
    }

    public void setHi(double hi) {
        this.hi = hi;
    }

    public double getLo() {
        return lo;
    }

    public void setLo(double lo) {
        this.lo = lo;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getBid() {
        return bid;
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(double previousClose) {
        this.previousClose = previousClose;
    }
}
