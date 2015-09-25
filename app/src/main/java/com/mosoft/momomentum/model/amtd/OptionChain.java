package com.mosoft.momomentum.model.amtd;

import android.text.TextUtils;

import com.mosoft.momomentum.model.BullCallSpread;
import com.mosoft.momomentum.model.Spread;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;
import org.simpleframework.xml.core.Commit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionChain extends AmtdResponse {

    @Element(name = "option-chain-results", required = false)
    private Data data;

    public String getSymbol() {
        return data.symbol;
    }

    public List<OptionDate> getOptionDates() {
        return data.optionDates;
    }

    public List<OptionQuote> getOptionCalls() {
        return data.callQuotes;
    }

    public List<OptionQuote> getOptionPuts() {
        return data.putQuotes;
    }

    public List<Spread> getAllSpreads() {
        List<Spread> ret = new ArrayList<>();
        for (OptionDate optionDate : data.optionDates) {
            ret.addAll(optionDate.getAllSpreads());
        }

        //TODO fix double sorting
        Collections.sort(ret, new Spread.AscendingAnnualizedProfitComparator());
        return ret;
    }

    @Override
    public String toString() {
        if (data == null)
            return "<empty>";

        return "Chain: " + data.description + " (" + data.optionDates.size() + " dates x " + (data.optionDates.isEmpty() ? "0" : data.optionDates.get(0).optionStrikes.size()) + " strikes)";
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
                        strike.put.optionType = OptionType.PUT;
                        putQuotes.add(strike.put);
                    }
                    if (strike.call != null) {
                        strike.call.underlyingSymbol = this;
                        strike.call.optionDate = optionDate;
                        strike.call.optionStrike = strike;
                        strike.call.optionType = OptionType.CALL;
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

        public List<Spread> getBullCallSpreads(double spread) {
            List<Spread> ret = new ArrayList<>();

            int i = 0;
            while (i < optionStrikes.size() - 1) {
                OptionStrike buy = optionStrikes.get(i);

                // Find the sell leg.
                double sellStrike = (buy.strikePrice + spread);
                OptionStrike sell = null;
                int j = i + 1;
                while (j < optionStrikes.size()) {

                    if (optionStrikes.get(j).strikePrice == sellStrike) {
                        sell = optionStrikes.get(j);
                        break;
                    }

                    if (optionStrikes.get(j).strikePrice > sellStrike) {
                        break;
                    }

                    j++;
                }

                if (sell != null) {
                    ret.add(Spread.newSpread(buy.call, sell.call, underlying));
                }

                if (j == optionStrikes.size())
                    break;

                i++;
            }

            return ret;
        }

        public List<Spread> getAllSpreads() {
            List<Spread> ret = new ArrayList<>();

            int i = 0;

            OptionStrike lo = null;
            OptionStrike hi = null;

            // bull call spread
            while (i < optionStrikes.size() - 1) {
                hi = optionStrikes.get(i);
                int j = i + 1;

                while (j < optionStrikes.size()) {
                    lo = optionStrikes.get(j);
                    addIfNotNull(ret, Spread.newSpread(hi.call, lo.call, underlying));
                    addIfNotNull(ret, Spread.newSpread(lo.call, hi.call, underlying));
                    addIfNotNull(ret, Spread.newSpread(hi.put, lo.put, underlying));
                    addIfNotNull(ret, Spread.newSpread(lo.put, hi.put, underlying));
                    j++;
                }
                i++;
            }

            return ret;
        }

        private void addIfNotNull(List<Spread> ret, Spread spread) {
            if (spread != null)
                ret.add(spread);
        }

        @Override
        public String toString() {
            return String.format("expiration: %d days (%d strikes)", daysToExpiration, optionStrikes.size());
        }
    }

    @Root(name = "option-strike")
    @Default(value = DefaultType.FIELD, required = false)
    public static class OptionStrike {

        @Element(name = "strike-price")
        private Double strikePrice;

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
        PUT, CALL;
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
        Double impliedVolatility;

        @Element(name = "theoratical-value", required = false)
        Double theoreticalValue;

        Double multiplier;

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

        // Getters

        public String getOptionSymbol() {
            return optionSymbol;
        }

        public String getDescription() {
            return description;
        }

        public Double getImpliedVolatility() {
            return impliedVolatility;
        }

        public Double getTheoreticalValue() {
            return theoreticalValue;
        }

        public double getStrike() {
            if (optionStrike.strikePrice == null)
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
            if (multiplier == null)
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
            return bid > 0d && getBidSize() > 0;
        }

        public boolean hasAsk() {
            return ask > 0d && getAskSize() > 0 && ask < Double.MAX_VALUE;
        }

        public OptionType getOptionType() {
            return optionType;
        }
    }



}
