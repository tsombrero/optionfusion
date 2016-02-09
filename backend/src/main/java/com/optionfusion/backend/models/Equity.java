package com.optionfusion.backend.models;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Entity
public class Equity {

    @Id
    private Long id;

    @Index
    private String ticker;

    private String description;

    private Date timeLastAccessed;

    @Index
    private ArrayList<String> keywords = new ArrayList<>();

    public Equity() {
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

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static final Comparator<Equity> TICKER_COMPARATOR = new Comparator<Equity>() {
        @Override
        public int compare(Equity o1, Equity o2) {
            return o1.getTicker().compareTo(o2.getTicker());
        }
    };

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Equity) {
            Equity e2 = (Equity)obj;
            return e2.ticker.equals(this.ticker)
                    && e2.id.equals(this.id);
        }
        return false;
    }
}
