package com.mosoft.momomentum.model.provider;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.module.MomentumApplication;

import java.util.Date;
import java.util.List;

import static com.mosoft.momomentum.module.MomentumApplication.*;

public class Interfaces {

    public enum OptionType {
        PUT,
        CALL;
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
    }

    public interface OptionStrike {
        boolean isStandard();

        public OptionQuote getPut();

        public OptionQuote getCall();

        public double getStrikePrice();

        String getJson(Gson gson);
    }

    public interface OptionDate {
        int getDaysToExpiration();

        List<Spread> getAllSpreads(FilterSet filterSet);

        Date getExpirationDate();

        List<Double> getStrikePrices();

        String getJson(Gson gson);
    }

    public interface OptionChain extends ResponseBase {
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

        double getClose();
    }

    public interface Account {
        String getAccountId();

        String getDisplayName();

        String getDescription();

        String getAssociatedAccount();
    }

    public interface ResponseBase {
        boolean succeeded();

        String getError();
    }
}
