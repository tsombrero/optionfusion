package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.Date;

@Entity
public class User {
    @Id
    Long id;

    ArrayList<Ref<Equity>> watchlist = new ArrayList<>();
    ArrayList<Ref<Position>> favorites = new ArrayList<>();
    String email;
    String displayName;
    Date joinDate, lastLogin;
}
