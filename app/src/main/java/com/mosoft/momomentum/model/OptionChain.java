package com.mosoft.momomentum.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

public class OptionChain extends AmtdResponse {

    @Element(name = "option-chain-results", required = false)
    private Data data;

    public String getSymbol() {
        return data.symbol;
    }

    public String getSymbolOpen() {
        return data.open;
    }

    public String getSymbolClose() {
        return data.close;
    }

    public List<OptionDate> getOptionDates() {
        return data.optionDates;
    }

    @Override
    public String toString() {
        if (data == null)
            return "<empty>";

        return "Chain: " + data.description + " (" + data.optionDates.size() + " dates x " + (data.optionDates.isEmpty() ? "0" : data.optionDates.get(0).optionStrikes.size()) + " strikes)";
    }

    public static class Data {
        private Data(){};

        private String error;
        private String symbol;
        private String description;
        private String bid, ask;
        private String high, low;
        private String open, close;
        private String change;
        private String time;

        @ElementList(name = "option-date", inline = true)
        List<OptionDate> optionDates;
    }

    @Root(name = "option-date")
    public static class OptionDate {
        private String date;

        @Element(name="days-to-expiration")
        private String daysToExpiration;

        @ElementList(name = "option-strike", inline = true)
        List<OptionStrike> optionStrikes;

        public String getDate() {
            return date;
        }

        public String getDaysToExpiration() {
            return daysToExpiration;
        }

        public List<OptionStrike> getOptionStrikes() {
            return optionStrikes;
        }

        @Override
        public String toString() {
            if (optionStrikes == null)
                return "<empty>";

            return optionStrikes.size() + " strikes";
        }
    }

    @Root(name="option-strike")
    public static class OptionStrike {

        @Element(name="strike-price")
        private String strikePrice;

        private OptionQuote put, call;

        public String getStrikePrice() {
            return strikePrice;
        }

        public OptionQuote getPut() {
            return put;
        }

        public OptionQuote getCall() {
            return call;
        }
    }

    public static class OptionQuote {
        @Element(name = "option-symbol")
        String optionSymbol;

        String description;
        String bid, ask;

        @Element(name = "implied-volatility")
        String impliedVolatility;

        @Element(name = "theoratical-value")
        String theoreticalValue;

        public String getOptionSymbol() {
            return optionSymbol;
        }

        public String getDescription() {
            return description;
        }

        public String getBid() {
            return bid;
        }

        public String getAsk() {
            return ask;
        }

        public String getImpliedVolatility() {
            return impliedVolatility;
        }

        public String getTheoreticalValue() {
            return theoreticalValue;
        }
    }
}
