package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.optionfusion.backend.utils.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Entity
@Cache
public class FusionUser {

    @Id
    String email;

    @Load
    transient List<Ref<Equity>> watchlistRefs = new ArrayList<>();

    @Ignore
    List<Equity> materializedWatchlist = new ArrayList<>();

    Map<String, String> userDataMap = new HashMap<>();

    List<Position> savedPositions = new ArrayList<>();

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

    public Map<String, String> getUserDatamap() {
        return userDataMap;
    }

    public List<Equity> getMaterializedWatchlist() {
        return materializedWatchlist;
    }

    @OnLoad
    public void deRef() {
        if (watchlistRefs != null) {
            materializedWatchlist = new ArrayList<>();
            for (Ref<Equity> equityRef : watchlistRefs) {
                if (equityRef.isLoaded()) {
                    materializedWatchlist.add(equityRef.get());
                }
            }
        }
    }

    public void setWatchlist(List<Equity> equityList) {
        watchlistRefs.clear();
        materializedWatchlist.clear();
        HashSet<String> tickers = new HashSet<>();

        for (Equity equity : equityList) {
            if (tickers.contains(equity.getSymbol()))
                continue;

            watchlistRefs.add(Ref.create(equity));
            materializedWatchlist.add(equity);
            tickers.add(equity.getSymbol());
        }
    }

    public void setUserData(String userDataKey, String userDataValue) {
        if (TextUtils.isEmpty(userDataKey))
            return;

        if (TextUtils.isEmpty(userDataValue))
            userDataMap.remove(userDataKey);
        else
            userDataMap.put(userDataKey, userDataValue);
    }

    public String getUserData(String userDataKey) {
        return userDataMap.get(userDataKey);
    }

    public List<Position> getSavedPositions() {
        return savedPositions;
    }
}
