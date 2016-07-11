package com.optionfusion.common;

import com.optionfusion.common.protobuf.OptionChainProto;

import java.text.ParseException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OptionKey implements Comparable<OptionKey> {
    final String underlyingSymbol;
    final double strike;
    final long expiration;
    final boolean isCall;
    final String key;

    public OptionKey(String underlyingSymbol, long expiration, boolean isCall, Double strike) {
        this.underlyingSymbol = underlyingSymbol;
        this.expiration = expiration;
        this.isCall = isCall;
        this.strike = strike;
        key = getKey(underlyingSymbol, expiration, isCall, strike);
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public double getStrike() {
        return strike;
    }

    public long getExpiration() {
        return expiration;
    }

    public boolean isCall() {
        return isCall;
    }

    public static OptionKey parse(String symbol) throws ParseException {
        if (symbol == null)
            return null;

        String[] parts = symbol.split("\\|");
        if (parts.length != 4) throw new ParseException(symbol, 0);

        try {
            return new OptionKey(parts[0], Long.valueOf(parts[1]), "C".equals(parts[2]), Double.valueOf(parts[3]));
        } catch (Throwable e) {
            throw new ParseException(symbol, 0);
        }
    }

    public String getKey() {
        return key;
    }

    public static String getKey(String underlying, long expiration, boolean isCall, Double strike) {
        if (TextUtils.isEmpty(underlying))
            throw new IllegalArgumentException("Underlying symbol cannot be empty");
        if (expiration < TimeUnit.DAYS.toMillis(365 * 30))
            throw new IllegalArgumentException("Illegal expiration value");
        if (strike == 0D)
            throw new IllegalArgumentException("Illegal strike value");

        return String.format(Locale.US, "%s|%d|%s|%.4f", underlying, expiration, isCall ? "C" : "P", strike);
    }

    @Override
    public String toString() {
        return key;
    }

    @Override
    public int compareTo(OptionKey o) {
        return key.compareTo(o.key);
    }

    public OptionChainProto.OptionQuote.OptionType getOptionType() {
        return isCall ? OptionChainProto.OptionQuote.OptionType.CALL : OptionChainProto.OptionQuote.OptionType.PUT;
    }
}
