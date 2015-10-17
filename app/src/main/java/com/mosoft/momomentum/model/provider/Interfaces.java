package com.mosoft.momomentum.model.provider;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;

import java.util.Date;
import java.util.List;

public class Interfaces {

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

    public enum Provider {
        AMERITRADE,
        GOOGGLE_FINANCE
    }

    public static interface OptionQuote {
        String getOptionSymbol();

        String getDescription();

        double getImpliedVolatility();

        double getTheoreticalValue();

        double getStrike();

        Double getUnderlyingSymbolAsk();

        int getDaysUntilExpiration();

        double getMultiplier();

        double getAsk();

        double getBid();

        int getBidSize();

        int getAskSize();

        boolean hasBid();

        boolean hasAsk();

        OptionType getOptionType();

        Date getExpiration();

        boolean isStandard();

        String toJson(Gson gson);

        Provider getProvider();
    }

    public interface OptionStrike {
        boolean isStandard();

        public OptionQuote getPut();

        public OptionQuote getCall();

        public double getStrikePrice();

        String getJson(Gson gson);

        Provider getProvider();
    }

    public interface OptionDate {
        int getDaysToExpiration();

        List<Spread> getAllSpreads(FilterSet filterSet);

        Date getExpirationDate();

        List<Double> getStrikePrices();

        String getJson(Gson gson);

        Provider getProvider();
    }

    public interface OptionChain {
        String getSymbol();

        double getLast();

        double getAsk();

        double getChange();

        List<OptionDate> getChainsByDate();

        List<Date> getExpirationDates();

        List<Double> getStrikePrices();

        List<OptionQuote> getOptionCalls();

        List<OptionQuote> getOptionPuts();

        List<Spread> getAllSpreads(FilterSet filterSet);

        String getEquityDescription();

        String toJson(Gson gson);

        Provider getProvider();

        double getClose();
    }
}
