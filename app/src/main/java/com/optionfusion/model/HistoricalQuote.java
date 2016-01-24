package com.optionfusion.model;

public class HistoricalQuote {
    private double hi, lo, open, close;
    private long volume, date;

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

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HistoricalQuote) {
            HistoricalQuote that = (HistoricalQuote)o;
            return this.date == that.date
                    && this.hi == that.hi
                    && this.lo == that.lo
                    && this.open == that.open
                    && this.close == that.close
                    && this.volume == that.volume;
        }
        return false;
    }
}
