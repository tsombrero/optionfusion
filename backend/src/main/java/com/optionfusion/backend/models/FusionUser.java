package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.Date;

@Entity
public class FusionUser {

    public static final String USER_ID = "userId";
    public static final String EMAIL = "email";
    public static final String DISPLAY_NAME = "displayName";

    @Id
    Long id;

    String userId;
    ArrayList<Ref<Equity>> watchlist = new ArrayList<>();
    ArrayList<Ref<Position>> favorites = new ArrayList<>();
    String email;
    String displayName;
    Date joinDate, lastLogin;


    public FusionUser(String userId, String email, String displayName) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public ArrayList<Ref<Equity>> getWatchlist() {
        return watchlist;
    }

    public ArrayList<Ref<Position>> getFavorites() {
        return favorites;
    }

    public void addEquityToWatchlist(Equity equity) {
        if (watchlist.contains(equity))
    }

    public void addPositionToFavorite(Position position) {

    }
}
