package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Entity
@Cache
public class Equity {

    public static final String TICKER = "ticker";
    public static final String DESCRIPTION = "description";

    @Id
    private String ticker;

    private String description;

    private Date timeLastAccessed;

    @Index
    private ArrayList<String> keywords = new ArrayList<>();

    @Load
    Ref<StockQuote> eodStockQuote;

    public Equity() {
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = new ArrayList<>(keywords);
    }

    public Equity(String symbol, String description, List<String> keywords) {
        this.ticker = symbol;
        this.description = description;
        if (keywords != null)
           this.keywords.addAll(keywords);
    }

    public String getTicker() {
        return ticker;
    }

    public String getDescription() {
        return description;
    }

    public static final Comparator<Equity> TICKER_COMPARATOR = new Comparator<Equity>() {
        @Override
        public int compare(Equity o1, Equity o2) {
            if (o1 == o2)
                return 0;

            if (o1 == null)
                return -1;

            if (o2 == null)
                return 1;

            return o1.getTicker().compareTo(o2.getTicker());
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Equity) {
            Equity e2 = (Equity)obj;
            return e2.ticker.equals(this.ticker);
        }
        return false;
    }

    public StockQuote getEodStockQuote() {
        if (eodStockQuote == null)
            return null;
        return eodStockQuote.get();
    }

    public void setEodStockQuote(StockQuote eodStockQuote) {
        this.eodStockQuote = Ref.create(eodStockQuote);
    }

//    public Key<Equity> getKey() {
//        return Key.create(Equity.class, ticker);
//    }
}
