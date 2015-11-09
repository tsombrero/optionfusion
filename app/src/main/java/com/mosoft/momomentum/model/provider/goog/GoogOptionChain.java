package com.mosoft.momomentum.model.provider.goog;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.FilterSet;
import com.mosoft.momomentum.model.Spread;
import com.mosoft.momomentum.model.provider.Interfaces;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.util.Util;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogOptionChain implements Interfaces.OptionChain {

    private ArrayList<GoogOptionDate> optionDates = new ArrayList<>();
    private transient boolean succeeded;

    private transient Interfaces.StockQuote stockQuote;

    public void addToChain(GoogOptionDate date) {
        optionDates.add(date);
    }

    @Override
    public Interfaces.StockQuote getUnderlyingStockQuote() {
        return stockQuote;
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
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }

    @Override
    public String getError() {
        return null;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public MomentumApplication.Provider getProvider() {
        return MomentumApplication.Provider.AMERITRADE;
    }

    public void setStockQuote(Interfaces.StockQuote stockQuote) {
        this.stockQuote = stockQuote;
    }

    public class GoogOptionQuote implements Interfaces.OptionQuote {
        private String s;
        private String e;
        private String p;
//        private String c;
        private Double b;
        private Double a;
//        private long oi;
//        private String vol;
        private Double strike;

        transient GoogOptionDate optionDate;

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
        public int getDaysUntilExpiration() {
            return optionDate.getDaysToExpiration();
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
            return optionDate.getExpirationDate();
        }

        @Override
        public boolean isStandard() {
            return false;
        }

        @Override
        public Interfaces.StockQuote getUnderlyingStockQuote() {
            return optionDate.optionChain.getUnderlyingStockQuote();
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }

        @Override
        public MomentumApplication.Provider getProvider() {
            return MomentumApplication.Provider.GOOGLE_FINANCE;
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

    public class GoogStockQuote implements Interfaces.StockQuote {
        String t;
        Double l_fix, s, c_fix, pcls_fix;

        @Override
        public String getSymbol() {
            return t;
        }

        @Override
        public String getDescription() {
            return "no description";
        }

        @Override
        public double getBid() {
            return l_fix;
        }

        @Override
        public double getAsk() {
            return l_fix;
        }

        @Override
        public double getLast() {
            return l_fix;
        }

        @Override
        public double getOpen() {
            return l_fix - c_fix;
        }

        @Override
        public double getClose() {
            return pcls_fix;
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }

        @Override
        public MomentumApplication.Provider getProvider() {
            return MomentumApplication.Provider.GOOGLE_FINANCE;
        }
    }

    public class GoogOptionDate implements Interfaces.OptionDate {
        private GoogExpiration expiry;
        private List<GoogOptionQuote> puts = new ArrayList<>();
        private List<GoogOptionQuote> calls = new ArrayList<>();

        private transient GoogOptionChain optionChain;

        public GoogOptionChain getOptionChain() {
            return optionChain;
        }

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
