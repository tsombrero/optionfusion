package com.optionfusion.model.provider;

import com.google.gson.Gson;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.HistoricalQuote;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.optionfusion.module.OptionFusionApplication.Provider;

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

        DateTime getExpiration();

        boolean isStandard();

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

        Double getChange();

        Double getChangePercent();

        long getLastUpdatedTimestamp();

        long getQuoteTimestamp();
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

        List<VerticalSpread> getAllSpreads(FilterSet filterSet);

        DateTime getExpirationDate();

        String toJson(Gson gson);
    }

    public interface OptionChain extends ResponseBase {
        double getUnderlyingPrice();

        String getSymbol();

        List<DateTime> getExpirationDates();

        List<Double> getStrikePrices();

        List<VerticalSpread> getAllSpreads(FilterSet filterSet);

        String toJson(Gson gson);

        Provider getProvider();

        long getLastUpdatedTimestamp();
    }

    public interface Account {
        String getAccountId();

        String getDisplayName();

        String getDescription();

        String getAssociatedAccount();
    }

    public interface StockPriceHistory {
        String getSymbol();

        Interval getInterval();

        HistoricalQuote[] getPrices();

        long getAgeOfLastEntryMs();

        void addQuote(HistoricalQuote quote);

        enum Interval {
            MINUTE(TimeUnit.MINUTES.toSeconds(1)),
            HOUR(TimeUnit.HOURS.toSeconds(1)),
            DAY(TimeUnit.DAYS.toSeconds(1)),
            WEEK(TimeUnit.DAYS.toSeconds(7));

            long intervalInSeconds;

            Interval(long intervalInSeconds) {
                this.intervalInSeconds = intervalInSeconds;
            }

            public long getIntervalInSeconds() {
                return intervalInSeconds;
            }

            public static Interval fromSeconds(final long intervalInSeconds) {
                for (Interval interval : values()) {
                    if (intervalInSeconds == interval.intervalInSeconds)
                        return interval;
                }
                return null;
            }

            public static Interval forStartDate(final Date date) {
                long dateDiff = System.currentTimeMillis() - date.getTime();
                if (dateDiff < TimeUnit.DAYS.toMillis(3))
                    return MINUTE;
                if (dateDiff < TimeUnit.DAYS.toMillis(14))
                    return HOUR;
                if (dateDiff < TimeUnit.DAYS.toMillis(365))
                    return DAY;
                return WEEK;
            }

            public long maxAgeBeforeStaleMs() {
                switch (this) {

                    case MINUTE:
                        return TimeUnit.SECONDS.toMillis(30);
                    case HOUR:
                        return TimeUnit.MINUTES.toMillis(5);
                    case DAY:
                        return TimeUnit.HOURS.toMillis(1);
                    case WEEK:
                    default:
                        return TimeUnit.DAYS.toMillis(1);
                }
            }
        }
    }

    public interface ResponseBase {
        boolean succeeded();

        String getError();
    }
}
