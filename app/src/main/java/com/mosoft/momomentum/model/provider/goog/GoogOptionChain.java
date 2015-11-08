package com.mosoft.momomentum.model.provider.goog;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.util.Util;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogOptionChain implements Interfaces.OptionChain {

    private ArrayList<GoogOptionDate> optionDates = new ArrayList<>();
    private transient boolean succeeded;

    public void addToChain(GoogOptionDate date) {
        optionDates.add(date);
    }

    @Override
    public String getSymbol() {
        return null;
    }

    @Override
    public double getLast() {
        return 0;
    }

    @Override
    public double getAsk() {
        return 0;
    }

    @Override
    public double getChange() {
        return 0;
    }

    @Override
    public List<? extends Interfaces.OptionDate> getChainsByDate() {
        return Collections.unmodifiableList(optionDates);
    }

    @Override
    public List<LocalDate> getExpirationDates() {
        return null;
    }

    @Override
    public List<Double> getStrikePrices() {
        return null;
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionCalls() {
        return null;
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionPuts() {
        return null;
    }

    @Override
    public List<Spread> getAllSpreads(FilterSet filterSet) {
        return null;
    }

    @Override
    public String getEquityDescription() {
        return null;
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public double getClose() {
        return 0;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public String getError() {
        return null;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public class GoogOptionQuote implements Interfaces.OptionQuote {
        private String s;
        private String e;
        private String p;
//        private String c;
        private double b;
        private double a;
//        private long oi;
//        private String vol;
        private double strike;

        transient GoogExpiration expiration;

        @Override
        public String getOptionSymbol() {
            return s;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public double getImpliedVolatility() {
            return 0;
        }

        @Override
        public double getTheoreticalValue() {
            return 0;
        }

        @Override
        public double getStrike() {
            return strike;
        }

        @Override
        public Double getUnderlyingSymbolAsk() {
            return null;
        }

        @Override
        public int getDaysUntilExpiration() {
            return expiration.getDaysToExpiration();
        }

        @Override
        public double getMultiplier() {
            return 0;
        }

        @Override
        public double getAsk() {
            return a;
        }

        @Override
        public double getBid() {
            return b;
        }

        @Override
        public int getBidSize() {
            return 0;
        }

        @Override
        public int getAskSize() {
            return 0;
        }

        @Override
        public boolean hasBid() {
            return b > 0;
        }

        @Override
        public boolean hasAsk() {
            return a > 0;
        }

        @Override
        public Interfaces.OptionType getOptionType() {
            return null;
        }

        @Override
        public LocalDate getExpiration() {
            return expiration.getDate();
        }

        @Override
        public boolean isStandard() {
            return false;
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }
    }

    public class GoogExpirations {
        public List<GoogExpiration> getExpirations() {
            return expirations;
        }

        public String getUnderlyingPrice() {
            return underlyingPrice;
        }

        private List<GoogExpiration> expirations = new ArrayList<>();
        private String underlyingPrice;
    }

    public class GoogOptionDate implements Interfaces.OptionDate {
        private GoogExpiration expiry;
        private List<GoogOptionQuote> puts = new ArrayList<>();
        private List<GoogOptionQuote> calls = new ArrayList<>();
        private String underlyingPrice;

        @Override
        public int getDaysToExpiration() {
            return expiry.getDaysToExpiration();
        }

        @Override
        public List<Spread> getAllSpreads(FilterSet filterSet) {
            return null;
        }

        @Override
        public LocalDate getExpirationDate() {
            return expiry.getDate();
        }

        @Override
        public List<Double> getStrikePrices() {
            return null;
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }
    }

    public class GoogExpiration {
        private Integer y;
        private Integer m;
        private Integer d;

        public Integer getY() {
            return y;
        }

        public Integer getM() {
            return m;
        }

        public Integer getD() {
            return d;
        }

        private transient int daysToExpiration = -1;

        private int getDaysToExpiration() {
            if (daysToExpiration < 0)
                daysToExpiration = Days.daysBetween(new LocalDate(), getDate()).getDays();
            return daysToExpiration;
        }

        private LocalDate getDate() {
            return Util.roundToNearestFriday(new LocalDate(y, m, d));
        }
    }
}
