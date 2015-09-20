package com.mosoft.momomentum.model;

import com.mosoft.momomentum.util.Util;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

import java.util.ArrayList;
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

    public List<OptionQuote> getOptionQuotes() {

    }

    @Override
    public String toString() {
        if (data == null)
            return "<empty>";

        return "Chain: " + data.description + " (" + data.optionDates.size() + " dates x " + (data.optionDates.isEmpty() ? "0" : data.optionDates.get(0).optionStrikes.size()) + " strikes)";
    }

    public static class Data {
        private Data() {
        }

        private String error;
        private String symbol;
        private String description;
        private Double bid, ask;
        private Double high, low;
        private Double open, close;
        private Double change;
        private String time;

        @ElementList(name = "option-date", inline = true)
        List<OptionDate> optionDates;

        transient List<OptionQuote> callQuotes = new ArrayList<OptionQuote>();
        transient List<OptionQuote> putQuotes = new ArrayList<OptionQuote>();

        @Commit
        public void build() {
            for (OptionDate optionDate : optionDates) {
                for (OptionStrike strike : optionDate.optionStrikes) {
                    if (strike.put != null) {
                        strike.put.underlyingSymbol = this;
                        strike.put.optionDate = optionDate;
                        strike.put.optionStrike = strike;
                        putQuotes.add(strike.put);
                    }
                    if (strike.call != null) {
                        strike.call.underlyingSymbol = this;
                        strike.call.optionDate = optionDate;
                        strike.call.optionStrike = strike;
                        callQuotes.add(strike.call);
                    }
                }
            }
        }

        public Double getAsk() {
            return ask;
        }

        public Double getBid() {
            return bid;
        }
    }

    @Root(name = "option-date")
    public static class OptionDate {
        private String date;

        @Element(name = "days-to-expiration")
        private int daysToExpiration;

        @ElementList(name = "option-strike", inline = true)
        List<OptionStrike> optionStrikes;
    }

    @Root(name = "option-strike")
    public static class OptionStrike {

        @Element(name = "strike-price")
        private Double strikePrice;
        private OptionQuote put, call;
    }

    public static class OptionQuote {

        // Serialized

        String description;
        Double bid, ask;

        @Element(name = "option-symbol")
        String optionSymbol;

        @Element(name = "implied-volatility")
        Double impliedVolatility;

        @Element(name = "theoratical-value")
        Double theoreticalValue;


        // Transient

        transient private OptionStrike optionStrike;
        transient private Data underlyingSymbol;
        transient private OptionDate optionDate;

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

        public Double getStrike() {
            return optionStrike.strikePrice;
        }

        public Double getUnderlyingSymbolAsk() {
            return underlyingSymbol.ask;
        }

        public long getDaysUntilExpiration() {
            return optionDate.daysToExpiration;
        }

        public Double getAsk() {
            return ask;
        }

        public Double getBid() {
            return bid;
        }
    }

}
