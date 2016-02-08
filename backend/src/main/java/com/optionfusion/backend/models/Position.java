package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import java.util.HashMap;

@Entity
public class Position {
    @Id
    Long id;

    @Parent
    Ref<Equity> underlying;

    boolean isDebit;
    double bid, ask;
    double maxGain, maxGainPercent, getMaxGainPercentAnnualized, maxLoss;
    HashMap<String, Long> components = new HashMap<>();

    public Equity getUnderlying() {
        return underlying.get();
    }

    public boolean isDebit() {
        return isDebit;
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getMaxGain() {
        return maxGain;
    }

    public double getMaxGainPercent() {
        return maxGainPercent;
    }

    public double getGetMaxGainPercentAnnualized() {
        return getMaxGainPercentAnnualized;
    }

    public double getMaxLoss() {
        return maxLoss;
    }

    public HashMap<String, Long> getComponents() {
        return components;
    }
}
