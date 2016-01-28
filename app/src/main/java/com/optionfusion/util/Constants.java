package com.optionfusion.util;

import com.optionfusion.BuildConfig;

public class Constants {
    /**
     * Substitute you own sender ID here. This is the project number you got from the API Console,
     * as described in "Getting Started."
     */
    static final String SENDER_ID = BuildConfig.SENDER_ID;

    /**
     * Web client ID from Google Cloud console.
     */
    public static final String WEB_CLIENT_ID = BuildConfig.WEB_CLIENT_ID;

    /**
     * The web client ID from Google Cloud Console.
     */
    public static final String AUDIENCE_ANDROID_CLIENT_ID =
            "server:client_id:" + WEB_CLIENT_ID;

    public static final String ANDROID_CLIENT_ID_DEBUG = "386969647168-j056r2g7hkuk0f11mm1eribp3cmd8p6b.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID = "386969647168-mc9ps4shq75slsuajtjuaj41nesabuva.apps.googleusercontent.com";

    /**
     * The URL to the API. Default when running locally on your computer:
     * "http://10.0.2.2:8080/_ah/api/"
     */
    public static final String ROOT_URL = BuildConfig.ROOT_URL;

    /**
     * Defines whether authentication is required or not.
     */
    public static final boolean SIGN_IN_REQUIRED = BuildConfig.SIGN_IN_REQUIRED;

    public static final String EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email";

    /**
     * Default constructor, never called.
     */
    private Constants() {
    }
}
