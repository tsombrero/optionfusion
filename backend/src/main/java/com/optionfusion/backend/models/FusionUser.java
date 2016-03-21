package com.optionfusion.backend.models;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Cache
public class FusionUser {

    public static final String EMAIL = "email";
    public static final String DISPLAY_NAME = "displayName";

    @Id
    String email;

    List<String> watchlistTickers = new ArrayList<>();

    @Index
    String displayName;

    Date joinDate;
    Date lastLogin;

    public FusionUser() {
    }

    public FusionUser(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public List<String> getWatchlistTickers() {
        return watchlistTickers;
    }

    public void addEquityToWatchlist(Equity equity) {
        if (!watchlistTickers.contains(equity.getTicker()))
            watchlistTickers.add(equity.getTicker());
    }

    public void addPositionToFavorite(Position position) {

    }
}
