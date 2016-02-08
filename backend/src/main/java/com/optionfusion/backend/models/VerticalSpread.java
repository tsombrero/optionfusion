package com.optionfusion.backend.models;

import com.googlecode.objectify.annotation.Subclass;

import java.util.Date;

@Subclass(index = true)
public class VerticalSpread extends Position {
    boolean isBullish;
    double iv;
    Date expiration;
    long daysToExp;
    double breakEven;
    double distanceToBreakEven;
    double weightedRisk;

    public boolean isBullish() {
        return isBullish;
    }

    public double getIV() {
        return iv;
    }

    public Date getExpiration() {
        return expiration;
    }

    public long getDaysToExp() {
        return daysToExp;
    }

    public double getBreakEven() {
        return breakEven;
    }

    public double getDistanceToBreakEven() {
        return distanceToBreakEven;
    }

    public double getWeightedRisk() {
        return weightedRisk;
    }
}
