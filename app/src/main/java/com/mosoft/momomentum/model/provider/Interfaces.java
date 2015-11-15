package com.mosoft.momomentum.model.provider;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;

import org.joda.time.LocalDate;

import java.util.List;

import static com.mosoft.momomentum.module.MomentumApplication.Provider;

public class Interfaces {

    public enum OptionType {
        PUT,
        CALL;
    }

    public interface OptionQuote {
        String getOptionSymbol();

        String getDescription();

        double getImpliedVolatility();

        double getTheoreticalValue();

        double getStrike();

        int getDaysUntilExpiration();

        double getMultiplier();

        double getAsk();

        double getBid();

        int getBidSize();

        int getAskSize();

        boolean hasBid();

        boolean hasAsk();

        OptionType getOptionType();

        LocalDate getExpiration();

        boolean isStandard();

        StockQuote getUnderlyingStockQuote();

        String toJson(Gson gson);

        Provider getProvider();
    }

    public interface StockQuote {
        String getSymbol();
        String getDescription();
        double getBid();
        double getAsk();
        double getLast();
        double getOpen();
        double getClose();
        String toJson(Gson gson);
        Provider getProvider();
    }

    public interface OptionStrike {
        boolean isStandard();

        OptionQuote getPut();

        OptionQuote getCall();

        double getStrikePrice();

        String getJson(Gson gson);
    }

    public interface OptionDate {
        int getDaysToExpiration();

        List<Spread> getAllSpreads(FilterSet filterSet);

        LocalDate getExpirationDate();

        double[] getStrikePrices();

        String toJson(Gson gson);
    }

    public interface OptionChain extends ResponseBase {
        StockQuote getUnderlyingStockQuote();

        List<? extends OptionDate> getChainsByDate();

        List<LocalDate> getExpirationDates();

        List<Double> getStrikePrices();

        List<? extends OptionQuote> getOptionCalls();

        List<? extends OptionQuote> getOptionPuts();

        List<Spread> getAllSpreads(FilterSet filterSet);

        String toJson(Gson gson);

        Provider getProvider();
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
