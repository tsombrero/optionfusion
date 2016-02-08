package com.optionfusion.backend.models;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Equity {

    @Id
    private String symbol;

    private String description;

    private Date timeLastAccessed;

    private ArrayList<String> keywords = new ArrayList<>();

    public Equity(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
