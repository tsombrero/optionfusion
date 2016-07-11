package com.optionfusion.backend.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import com.optionfusion.common.OptionKey;
import com.optionfusion.common.TextUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.jdo.annotations.Embedded;

@Entity
public class Position {
    @Id
    private String hashString;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    Key<FusionUser> fusionUserKey;

    String underlyingSymbol;

    double bid, ask;
    long quoteTimestamp;

    long deletedTimestamp;

    double cost;
    long acquiredTimestamp;

    HashMap<String, Long> legs = new HashMap<>();

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public HashMap<String, Long> getLegs() {
        return legs;
    }

    public void setLegs(HashMap<String, Long> legs) {
        this.legs = legs;
    }

    @Override
    public int hashCode() {
        return getPositionKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.hashCode() == hashCode() && obj instanceof Position)
            return TextUtils.equals(getPositionKey(), ((Position) obj).getPositionKey());
        return false;
    }

    @OnSave
    public String getPositionKey() {
        if (TextUtils.isEmpty(hashString)) {
            List<String> keys = new ArrayList<>(legs.keySet());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder();
            for (String key : keys) {
                sb.append(legs.get(key))
                        .append(":")
                        .append(key)
                        .append(";");
            }
            hashString = sb.toString();
        }
        return hashString;
    }

    @OnSave
    public String getUnderlyingSymbol() {
        if (underlyingSymbol == null) {
            for (String leg : getLegs().keySet()) {
                try {
                    OptionKey optionKey = OptionKey.parse(leg);
                    setUnderlyingSymbol(optionKey.getUnderlyingSymbol());
                } catch (ParseException e) {
                }
            }
        }
        return underlyingSymbol;
    }

    public long getQuoteTimestamp() {
        return quoteTimestamp;
    }

    public Long getQty(String leg) {
        return legs.get(leg);
    }

    public void setBid(double bid) {
        this.bid = bid;
    }

    public void setAsk(double ask) {
        this.ask = ask;
    }

    public void setQuoteTimestamp(long quoteTimestamp) {
        this.quoteTimestamp = quoteTimestamp;
    }

    public void setFusionUserKey(Key<FusionUser> key) {
        this.fusionUserKey = key;
    }

    public String getId() {
        return getPositionKey();
    }

    public long getDeletedTimestamp() {
        return deletedTimestamp;
    }

    public void setDeletedTimestamp(long deletedTimestamp) {
        this.deletedTimestamp = deletedTimestamp;
    }

    public double getCost() {
        return cost;
    }

    public long getAcquiredTimestamp() {
        return acquiredTimestamp;
    }

    public void setAcquiredTimestamp(long acquiredTimestamp) {
        this.acquiredTimestamp = acquiredTimestamp;
    }


}
