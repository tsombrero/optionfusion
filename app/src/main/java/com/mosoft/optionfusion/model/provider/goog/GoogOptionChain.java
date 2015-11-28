package com.mosoft.optionfusion.model.provider.goog;

import android.util.Log;

import com.google.gson.Gson;
import com.mosoft.optionfusion.model.FilterSet;
import com.mosoft.optionfusion.model.Spread;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.util.Util;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*

{
  "underlying_price": "32.32",
  "underlying_id": "33312",
  "calls": [
    {
      "expiry": "Jan 19, 2018",
      "strike": "18.00",
      "vol": "-",
      "oi": "13",
      "a": "16.80",
      "b": "12.20",
      "cid": "718366916236700",
      "name": "",
      "s": "T180119C00018000",
      "e": "OPRA",
      "p": "15.00",
      "cs": "chb",
      "c": "0.00",
      "cp": "0.00"
    },
  ],
  "puts": [
    {
      "expiry": "Jan 19, 2018",
      "strike": "45.00",
      "vol": "-",
      "oi": "23",
      "a": "17.60",
      "b": "13.45",
      "cid": "635924903483033",
      "name": "",
      "s": "T180119P00045000",
      "e": "OPRA",
      "p": "14.15",
      "cs": "chb",
      "c": "0.00",
      "cp": "0.00"
    }
  ],
  "expirations": [
    {
      "d": "20",
      "m": "11",
      "y": "2015"
    },
    {
      "d": "19",
      "m": "1",
      "y": "2018"
    }
  ],
  "expiry": {
    "d": "19",
    "m": "1",
    "y": "2018"
  }
}


 */
public class GoogOptionChain implements Interfaces.OptionChain {

    private ArrayList<GoogOptionDate> optionDates = new ArrayList<>();
    private transient boolean succeeded;

    private transient Interfaces.StockQuote stockQuote;

    public void addToChain(GoogOptionDate date) {
        for (GoogOptionQuote quote : date.calls) {
            quote.optionType = Interfaces.OptionType.CALL;
            quote.optionDate = date;
        }
        for (GoogOptionQuote quote : date.puts) {
            quote.optionType = Interfaces.OptionType.PUT;
            quote.optionDate = date;
        }
        date.optionChain = this;
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
        List<LocalDate> ret = new ArrayList<>();
        for (Interfaces.OptionDate optionDate : optionDates) {
            ret.add(optionDate.getExpirationDate());
        }
        return ret;
    }

    @Override
    public List<Double> getStrikePrices() {
        Set<Double> ret = new HashSet<>();
        for (int i = 0; i < optionDates.size(); i += 4) {
            for (double val : optionDates.get(i).getStrikePrices()) {
                ret.add(val);
            }
        }
        return new ArrayList<>(ret);
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionCalls() {
        ArrayList<Interfaces.OptionQuote> ret = new ArrayList<>();
        for (GoogOptionDate optionDate : optionDates) {
            ret.addAll(optionDate.calls);
        }
        return ret;
    }

    @Override
    public List<Interfaces.OptionQuote> getOptionPuts() {
        ArrayList<Interfaces.OptionQuote> ret = new ArrayList<>();
        for (GoogOptionDate optionDate : optionDates) {
            ret.addAll(optionDate.puts);
        }
        return ret;
    }

    @Override
    public List<Spread> getAllSpreads(FilterSet filterSet) {
        ArrayList<Spread> ret = new ArrayList<>();
        for (GoogOptionDate optionDate : optionDates) {
            ret.addAll(optionDate.getAllSpreads(filterSet));
        }
        return ret;
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

    public OptionFusionApplication.Provider getProvider() {
        return OptionFusionApplication.Provider.GOOGLE_FINANCE;
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

        // filled in post-parsing
        public Interfaces.OptionType optionType;

        @Override
        public String getOptionSymbol() {
            return s;
        }

        @Override
        public String getDescription() {
            return String.format("%s %s %s", optionType.name(), Util.formatDollars(strike), Util.getFormattedOptionDate(optionDate.getExpirationDate()));
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
            return 100;
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
            return optionType;
        }

        @Override
        public LocalDate getExpiration() {
            return optionDate.getExpirationDate();
        }

        @Override
        public boolean isStandard() {
            return true;
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
        public OptionFusionApplication.Provider getProvider() {
            return OptionFusionApplication.Provider.GOOGLE_FINANCE;
        }
    }

    public class GoogExpirations {
        public List<GoogExpiration> getExpirations() {
            return expirations;
        }

        private List<GoogExpiration> expirations = new ArrayList<>();
    }

    public class GoogOptionDate implements Interfaces.OptionDate {
        private GoogExpiration expiry;
        private List<GoogOptionQuote> puts = new ArrayList<>();
        private List<GoogOptionQuote> calls = new ArrayList<>();

        private transient GoogOptionChain optionChain;
        private transient double[] strikeArray;

        @Override
        public int getDaysToExpiration() {
            return expiry.getDaysToExpiration();
        }

        @Override
        public List<Spread> getAllSpreads(FilterSet filterSet) {
            if (!filterSet.pass(this))
                return new ArrayList<>();

            List<Spread> ret = getSpreads(filterSet, calls);
            ret.addAll(getSpreads(filterSet, puts));
            return ret;
        }

        private List<Spread> getSpreads(FilterSet filterSet, List<GoogOptionQuote> putsOrCalls) {
            List<Spread> ret = new ArrayList<>();
            Interfaces.OptionType type = putsOrCalls.get(0).getOptionType();
            int i = 0;
            while (i < putsOrCalls.size()) {
                GoogOptionQuote a = putsOrCalls.get(i);
                int j = i + 1;
                while (j < putsOrCalls.size()) {
                    GoogOptionQuote b = putsOrCalls.get(j);
                    addIfPassFilters(ret, filterSet, Spread.newSpread(a, b, optionChain.getUnderlyingStockQuote()));
                    addIfPassFilters(ret, filterSet, Spread.newSpread(b, a, optionChain.getUnderlyingStockQuote()));
                    j++;
                }
                i++;
            }
            return ret;
        }

        @Override
        public LocalDate getExpirationDate() {
            return expiry.getDate();
        }

        @Override
        public double[] getStrikePrices() {
            if (strikeArray == null) {
                Set<Double> strikes = new HashSet<>();
                for (GoogOptionQuote optionQuote : calls) {
                    strikes.add(optionQuote.getStrike());
                }
                for (GoogOptionQuote optionQuote : puts) {
                    strikes.add(optionQuote.getStrike());
                }
                strikeArray = Util.toArray(strikes);
            }
            return strikeArray;
        }

        @Override
        public String toJson(Gson gson) {
            return gson.toJson(this);
        }

        private void addIfPassFilters(List<Spread> ret, FilterSet filters, Spread spread) {
            if (spread == null
                    || spread.getMaxPercentProfitAtExpiration() < 0.001d)
                return;

            if (filters.pass(spread))
                ret.add(spread);
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
            if (daysToExpiration <= 0) {
                daysToExpiration = Days.daysBetween(new LocalDate(), getDate()).getDays();
                if (daysToExpiration == 0) {
                    Log.i("TACO", "TACO");
                }
            }
            return daysToExpiration;
        }

        private LocalDate getDate() {
            Log.i("TACO", "TACO " + new LocalDate(y, m, d) + " rounded = " + Util.roundToNearestFriday(new LocalDate(y, m, d)));
            return Util.roundToNearestFriday(new LocalDate(y, m, d));
        }
    }
}
