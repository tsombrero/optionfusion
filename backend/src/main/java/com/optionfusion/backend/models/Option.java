package com.optionfusion.backend.models;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.Date;

@Entity
public class Option {
    @Id
    String optionSymbol;

    long timestamp;

    double strike;
    Date expiration;
    long openInterest, shortInterest;
    double bid, ask;
}
