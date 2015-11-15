package com.mosoft.momomentum.model.provider.amtd;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import org.joda.time.LocalDate;
import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AmeritradeOptionChain extends AmtdResponseBase implements Interfaces.OptionChain {

    @Element(name = "option-chain-results", required = false)
    private Data data;

    private List<LocalDate> expirationDates;
    private List<Double> strikePrices;

    private AmeritradeStockQuote stockQuote;

    public AmeritradeStockQuote getStockQuote() {
        return stockQuote;
    }

    public void setStockQuote(AmeritradeStockQuote stockQuote) {
        this.stockQuote = stockQuote;
    }

    @Override
    public Interfaces.StockQuote getUnderlyingStockQuote() {
        return stockQuote;
    }

    @Override
    public List<AmtdOptionDate> getChainsByDate() {
        if (data == null)
            return Collections.EMPTY_LIST;

        return Collections.unmodifiableList(data.optionDates);
    }

    @Override
    public synchronized List<LocalDate> getExpirationDates() {
        if (expirationDates == null) {
            HashSet<LocalDate> ret = new HashSet<>();
            for (Interfaces.OptionDate optionDate : getChainsByDate()) {
                LocalDate d = optionDate.getExpirationDate();
                if (d != null)
                    ret.add(d);
            }
            if (!ret.isEmpty())
                expirationDates = new ArrayList(ret);
        }

        return expirationDates;
    }

    @Override
    public synchronized List<Double> getStrikePrices() {
        if (strikePrices == null) {
            // grab the list of prices from the first four and the last one
            Set<Double> priceSet = new HashSet<>();
            for (int i = 0; i < 4 && i < data.optionDates.size(); i++) {
                for (double val : data.optionDates.get(i).getStrikePrices()) {
                    priceSet.add(val);
                }
            }

            for (double val : data.optionDates.get(data.optionDates.size() - 1).getStrikePrices()) {
                priceSet.add(val);
            }

            if (!priceSet.isEmpty())
                strikePrices = new ArrayList<>(priceSet);
        }
        return strikePrices;
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionCalls() {
        return callQuotes;
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionPuts() {
        return putQuotes;
    }

    @Override
    public List<Spread> getAllSpreads(FilterSet filterSet) {
        List<Spread> ret = new ArrayList<>();
        for (AmtdOptionDate optionDate : data.optionDates) {
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

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    transient List<Interfaces.OptionQuote> callQuotes = new ArrayList<>();
    transient List<Interfaces.OptionQuote> putQuotes = new ArrayList<>();

    @Commit
    public void build() {
        for (AmtdOptionDate optionDate : data.optionDates) {
            optionDate.optionChain = this;
            for (OptionStrike strike : optionDate.optionStrikes) {
                strike.optionDate = optionDate;
                if (strike.put != null) {
                    strike.put.optionDate = optionDate;
                    strike.put.optionStrike = strike;
                    strike.put.optionType = Interfaces.OptionType.PUT;
                    strike.put.standard = strike.isStandard();
                    putQuotes.add(strike.put);
                }
                if (strike.call != null) {
                    strike.call.optionDate = optionDate;
                    strike.call.optionStrike = strike;
                    strike.call.optionType = Interfaces.OptionType.CALL;
                    strike.call.standard = strike.isStandard();
                    callQuotes.add(strike.call);
                }
            }
        }
    }

    // Deserialized:

    @Root
    @Default(value = DefaultType.FIELD, required = false)
    private static class Data {
        private String error;
        private String symbol;
        private String description;
        private Double bid, ask, last;
        private Double high, low;
        private Double open, close;
        private Double change;
        private String time;

        @ElementList(name = "option-date", inline = true)
        List<AmtdOptionDate> optionDates;
    }

    @Root(name = "option-date")
    @Default(value = DefaultType.FIELD, required = false)
    public static class AmtdOptionDate implements Interfaces.OptionDate {
        String date;

        @Override
        public int getDaysToExpiration() {
            return daysToExpiration;
        }

        @Element(name = "days-to-expiration")
        int daysToExpiration;

        @ElementList(name = "option-strike", inline = true)
        List<OptionStrike> optionStrikes;

        @Transient
        transient AmeritradeOptionChain optionChain;

        @Override
        public List<Spread> getAllSpreads(FilterSet filterSet) {
            List<Spread> ret = new ArrayList<>();

            if (!filterSet.pass(this))
                return ret;

            int i = 0;

            OptionStrike lo = null;
            OptionStrike hi = null;

            Interfaces.StockQuote underlying = optionChain.getUnderlyingStockQuote();

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

        @Override
        public LocalDate getExpirationDate() {
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

        @Override
        public double[] getStrikePrices() {
            List<Double> ret = new ArrayList<>();
            for (OptionStrike strike : optionStrikes) {
                ret.add(strike.strikePrice);
            }
            return Util.toArray(ret);
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }
    }

    @Root(name = "option-strike")
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionStrike implements Interfaces.OptionStrike {

        @Element(name = "strike-price")
        private double strikePrice = Double.MAX_VALUE;

        @Element(name = "standard-option")
        private Boolean isStandard;

        @Transient
        public transient AmtdOptionDate optionDate;

        public OptionQuote getPut() {
            return put;
        }

        public OptionQuote getCall() {
            return call;
        }

        public double getStrikePrice() {
            return strikePrice;
        }

        private OptionQuote put, call;

        @Override
        public boolean isStandard() {
            return isStandard;
        }

        @Override
        public String toString() {
            return String.format("strike: $%.2f", strikePrice);
        }

        @Override
        public String getJson(Gson gson) {
            return gson.toJson(this);
        }
    }

    @Root
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionQuote implements Interfaces.OptionQuote {

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

        @Element(name = "bid-ask-size", required = false)
        String bidAskSize;


        // Transients

        @Transient
        transient private OptionStrike optionStrike;
        @Transient
        transient private AmtdOptionDate optionDate;
        @Transient
        transient private Interfaces.OptionType optionType;
        @Transient
        transient private boolean standard;
        @Transient
        transient private LocalDate exp;

        // Getters

        @Override
        public String getOptionSymbol() {
            return optionSymbol;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public double getImpliedVolatility() {
            return impliedVolatility;
        }

        @Override
        public double getTheoreticalValue() {
            return theoreticalValue;
        }

        @Override
        public double getStrike() {
            if (optionStrike.strikePrice == Double.MAX_VALUE)
                return 0d;

            return optionStrike.strikePrice;
        }

        @Override
        public int getDaysUntilExpiration() {
            return optionDate.daysToExpiration;
        }

        @Override
        public double getMultiplier() {
            if (multiplier == Double.MAX_VALUE)
                return 0d;
            return multiplier;
        }

        @Override
        public double getAsk() {
            if (ask == null)
                return Double.MAX_VALUE;

            return ask;
        }

        @Override
        public double getBid() {
            if (bid == null)
                return 0d;

            return bid;
        }

        public String toString() {
            return description + " bid/ask: " + bid + "/" + ask;
        }

        @Override
        public int getBidSize() {
            if (TextUtils.isEmpty(bidAskSize) || !bidAskSize.contains("X"))
                return 0;

            try {
                return Integer.valueOf(bidAskSize.split("X")[0]);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public int getAskSize() {
            if (TextUtils.isEmpty(bidAskSize) || !bidAskSize.contains("X"))
                return 0;

            try {
                return Integer.valueOf(bidAskSize.split("X")[1]);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public boolean hasBid() {
            return bid != null && bid > 0d && getBidSize() > 0;
        }

        @Override
        public boolean hasAsk() {
            return ask != null && ask > 0d && getAskSize() > 0 && ask < Double.MAX_VALUE;
        }

        @Override
        public Interfaces.OptionType getOptionType() {
            return optionType;
        }

        @Override
        public LocalDate getExpiration() {
            if (exp == null) {
                int year = Integer.valueOf(optionDate.date.substring(0, 4));
                int month = Integer.valueOf(optionDate.date.substring(4, 6));
                int day = Integer.valueOf(optionDate.date.substring(6));
                exp = new LocalDate(year, month, day);
            }
            return exp;
        }

        @Override
        public boolean isStandard() {
            return standard;
        }

        @Override
        public Interfaces.StockQuote getUnderlyingStockQuote() {
            return optionDate.optionChain.stockQuote;
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }

        @Override
        public MomentumApplication.Provider getProvider() {
            return MomentumApplication.Provider.AMERITRADE;
        }
    }
}
