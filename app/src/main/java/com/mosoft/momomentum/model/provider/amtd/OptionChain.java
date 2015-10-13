package com.mosoft.momomentum.model.provider.amtd;

import android.text.TextUtils;

import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OptionChain extends AmtdResponse {

    @Element(name = "option-chain-results", required = false)
    private Data data;

    private List<Date> expirationDates;
    private List<Double> strikePrices;

    public String getSymbol() {
        return data.symbol;
    }

    public double getLast() {
        return data.last;
    }

    public double getChange() {
        return data.change;
    }

    public List<OptionDate> getChainsByDate() {
        if (data == null)
            return Collections.EMPTY_LIST;
        return data.optionDates;
    }

    public synchronized List<Date> getExpirationDates() {
        if (expirationDates == null) {
            HashSet<Date> ret = new HashSet<>();
            for (OptionDate optionDate : getChainsByDate()) {
                Date d = optionDate.getExpirationDate();
                if (d != null)
                    ret.add(d);
            }
            if (!ret.isEmpty())
                expirationDates = new ArrayList(ret);
        }

        return expirationDates;
    }

    public synchronized List<Double> getStrikePrices() {
        if (strikePrices == null) {
            // grab the list of prices from the first four and the last one
            Set<Double> priceSet = new HashSet<>();
            for (int i=0; i<4 && i < data.optionDates.size(); i++) {
                priceSet.addAll(data.optionDates.get(i).getStrikePrices());
            }

            priceSet.addAll(data.optionDates.get(data.optionDates.size()-1).getStrikePrices());

            if (!priceSet.isEmpty())
                strikePrices = new ArrayList<>(priceSet);
        }
        return strikePrices;
    }

    public List<OptionQuote> getOptionCalls() {
        return data.callQuotes;
    }

    public List<OptionQuote> getOptionPuts() {
        return data.putQuotes;
    }

    public List<Spread> getAllSpreads(FilterSet filterSet) {
        List<Spread> ret = new ArrayList<>();
        for (OptionDate optionDate : data.optionDates) {
            ret.addAll(optionDate.getAllSpreads(filterSet));
        }
        return ret;
    }

    @Override
    public String toString() {
        if (data == null)
            return "<empty>";

        return "Chain: " + data.description + " (" + data.optionDates.size() + " dates x " + (data.optionDates.isEmpty() ? "0" : data.optionDates.get(0).optionStrikes.size()) + " strikes)";
    }

    public String getEquityDescription() {
        return data.description;
    }

    // Deserialized:

    @Root
    @Default(value = DefaultType.FIELD, required = false)
    public static class Data {
        private Data() {
        }

        private String error;
        private String symbol;
        private String description;
        private Double bid, ask, last;
        private Double high, low;
        private Double open, close;
        private Double change;
        private String time;

        @ElementList(name = "option-date", inline = true)
        List<OptionDate> optionDates;

        transient List<OptionQuote> callQuotes = new ArrayList<>();
        transient List<OptionQuote> putQuotes = new ArrayList<>();

        @Commit
        public void build() {
            for (OptionDate optionDate : optionDates) {
                optionDate.underlying = this;
                for (OptionStrike strike : optionDate.optionStrikes) {
                    if (strike.put != null) {
                        strike.put.underlyingSymbol = this;
                        strike.put.optionDate = optionDate;
                        strike.put.optionStrike = strike;
                        strike.put.optionType = OptionType.BEAR_PUT;
                        strike.put.standard = strike.isStandard();
                        putQuotes.add(strike.put);
                    }
                    if (strike.call != null) {
                        strike.call.underlyingSymbol = this;
                        strike.call.optionDate = optionDate;
                        strike.call.optionStrike = strike;
                        strike.call.optionType = OptionType.BULL_CALL;
                        strike.call.standard = strike.isStandard();
                        callQuotes.add(strike.call);
                    }
                }
            }
        }

        public double getAsk() {
            if (ask == null)
                return Double.MAX_VALUE;

            return ask;
        }

        public double getBid() {
            if (bid == null)
                return 0d;

            return bid;
        }

        public double getLast() {
            if (last == null)
                return 0d;

            return last;
        }

        public String getSymbol() {
            return symbol;
        }

        public Double getClose() {
            return close;
        }
    }

    @Root(name = "option-date")
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionDate {
        String date;

        public int getDaysToExpiration() {
            return daysToExpiration;
        }

        @Element(name = "days-to-expiration")
        int daysToExpiration;

        @ElementList(name = "option-strike", inline = true)
        List<OptionStrike> optionStrikes;

        @Transient
        transient Data underlying;

        public List<Spread> getAllSpreads(FilterSet filterSet) {
            List<Spread> ret = new ArrayList<>();

            if (!filterSet.pass(this))
                return ret;

            int i = 0;

            OptionStrike lo = null;
            OptionStrike hi = null;

            // bull call spread
            while (i < optionStrikes.size() - 1) {
                hi = optionStrikes.get(i);
                int j = i + 1;

                while (j < optionStrikes.size()) {
                    lo = optionStrikes.get(j);
                    addIfPassFilters(ret, filterSet, Spread.newSpread(hi.call, lo.call, underlying));
                    addIfPassFilters(ret, filterSet, Spread.newSpread(lo.call, hi.call, underlying));
                    addIfPassFilters(ret, filterSet, Spread.newSpread(hi.put, lo.put, underlying));
                    addIfPassFilters(ret, filterSet, Spread.newSpread(lo.put, hi.put, underlying));
                    j++;
                }
                i++;
            }

            return ret;
        }

        private void addIfPassFilters(List<Spread> ret, FilterSet filters, Spread spread) {
            if (spread == null
                    || spread.getMaxPercentProfitAtExpiration() < 0.001d
                    || !spread.getBuy().isStandard()
                    || !spread.getSell().isStandard())
                return;

            if (filters.pass(spread))
                ret.add(spread);
        }

        public Date getExpirationDate() {
            for (OptionStrike strike : optionStrikes) {
                if (strike.call != null)
                    return strike.call.getExpiration();
                if (strike.put != null)
                    return strike.put.getExpiration();
            }
            return null;
        }

        @Override
        public String toString() {
            return String.format("expiration: %d days (%d strikes)", daysToExpiration, optionStrikes.size());
        }

        public List<Double> getStrikePrices() {
            List<Double> ret = new ArrayList<>();
            for (OptionStrike strike : optionStrikes) {
                ret.add(strike.strikePrice);
            }
            return ret;
        }
    }

    @Root(name = "option-strike")
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionStrike {

        @Element(name = "strike-price")
        private double strikePrice = Double.MAX_VALUE;

        @Element(name = "standard-option")
        private Boolean isStandard;

        private OptionQuote put, call;

        public boolean isStandard() {
            return isStandard;
        }

        @Override
        public String toString() {
            return String.format("strike: $%.2f", strikePrice);
        }
    }

    public enum OptionType {
        BEAR_PUT("BEAR PUT"),
        BULL_CALL("BULL CALL"),
        BEAR_CALL("BEAR CALL"),
        BULL_PUT("BULL PUT");

        private final String string;

        OptionType(String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }
    }

    @Root
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionQuote {

        // Serialized

        String description;
        Double bid, ask;

        @Element(name = "option-symbol")
        String optionSymbol;

        @Element(name = "implied-volatility", required = false)
        double impliedVolatility = Double.MAX_VALUE;

        @Element(name = "theoratical-value", required = false)
        double theoreticalValue = Double.MAX_VALUE;

        double multiplier = Double.MAX_VALUE;

        @Element(name="bid-ask-size", required = false)
        String bidAskSize;


        // Transients

        @Transient
        transient private OptionStrike optionStrike;
        @Transient
        transient private Data underlyingSymbol;
        @Transient
        transient private OptionDate optionDate;
        @Transient
        transient private OptionType optionType;
        private boolean standard;

        // Getters

        public String getOptionSymbol() {
            return optionSymbol;
        }

        public String getDescription() {
            return description;
        }

        public double getImpliedVolatility() {
            return impliedVolatility;
        }

        public double getTheoreticalValue() {
            return theoreticalValue;
        }

        public double getStrike() {
            if (optionStrike.strikePrice == Double.MAX_VALUE)
                return 0d;

            return optionStrike.strikePrice;
        }

        public Double getUnderlyingSymbolAsk() {
            return underlyingSymbol.ask;
        }

        public int getDaysUntilExpiration() {
            return optionDate.daysToExpiration;
        }

        public double getMultiplier() {
            if (multiplier == Double.MAX_VALUE)
                return 0d;
            return multiplier;
        }

        public double getAsk() {
            if (ask == null)
                return Double.MAX_VALUE;

            return ask;
        }

        public double getBid() {
            if (bid == null)
                return 0d;

            return bid;
        }

        public String toString() {
            return description + " (" + bid + "/" + ask + ") V" + impliedVolatility;
        }

        public int getBidSize() {
            if (TextUtils.isEmpty(bidAskSize) || !bidAskSize.contains("X"))
                return 0;

            try {
                return Integer.valueOf(bidAskSize.split("X")[0]);
            } catch (Exception e) {
                return 0;
            }
        }

        public int getAskSize() {
            if (TextUtils.isEmpty(bidAskSize) || !bidAskSize.contains("X"))
                return 0;

            try {
                return Integer.valueOf(bidAskSize.split("X")[1]);
            } catch (Exception e) {
                return 0;
            }
        }

        public boolean hasBid() {
            return bid != null && bid > 0d && getBidSize() > 0;
        }

        public boolean hasAsk() {
            return ask != null && ask > 0d && getAskSize() > 0 && ask < Double.MAX_VALUE;
        }

        public OptionType getOptionType() {
            return optionType;
        }

        public Date getExpiration() {
            int year = Integer.valueOf(optionDate.date.substring(0, 4));
            int month = Integer.valueOf(optionDate.date.substring(4, 6));
            int day = Integer.valueOf(optionDate.date.substring(6));
            return new GregorianCalendar(year, month, day).getTime();
        }

        public boolean isStandard() {
            return standard;
        }
    }



}
