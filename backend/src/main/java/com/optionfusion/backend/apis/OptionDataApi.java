/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.optionfusion.backend.apis;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.FusionUser;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.models.StockQuote;
import com.optionfusion.backend.utils.Constants;
import com.optionfusion.backend.utils.TextUtils;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN;
import static com.optionfusion.backend.utils.OfyService.ofy;

/**
 * An endpoint class we are exposing
 */
@Api(
        name = "optionFusion",
        version = "v1",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.ANDROID_CLIENT_ID_DEBUG},
        audiences = {Constants.ANDROID_AUDIENCE},

        namespace = @ApiNamespace(
                ownerDomain = "backend.com.optionfusion.com",
                ownerName = "backend.com.optionfusion.com",
                packagePath = ""
        )
)

public class OptionDataApi {

    private static final GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Arrays.asList(Constants.ANDROID_AUDIENCE))
            .setIssuer("https://accounts.google.com")
            .build();

    private static final int MAX_RESULTS = 50;
    private static final Logger log = Logger.getLogger(OptionDataApi.class.getSimpleName());

    @ApiMethod(httpMethod = "GET", path = "getTickersMatching")
    public final List<Equity> getTickersMatching(@Named("q") String searchString) {

        ArrayList<Equity> ret = new ArrayList<>();

        List<Equity> equities = ofy().load().type(Equity.class)
                .filter(startsWithFilter(Equity.SYMBOL, searchString.toUpperCase()))
                .limit(MAX_RESULTS)
                .list();

        if (equities != null) {
            ret.addAll(equities);
            Collections.sort(ret, Equity.TICKER_COMPARATOR);
        }

        equities = ofy().load().type(Equity.class)
                .filter(startsWithFilter(Equity.KEYWORDS, searchString.toLowerCase()))
                .limit(MAX_RESULTS)
                .list();

        if (equities != null) {
            equities.removeAll(ret);
            Collections.sort(equities, Equity.TICKER_COMPARATOR);
            ret.addAll(equities);
        }

        List<Equity> toRemove = new ArrayList<>();
        for (Equity equity : ret) {
            if (equity.getEodStockQuote() == null || equity.getEodStockQuote().getClose() <= 0d)
                toRemove.add(equity);
        }

        ret.removeAll(toRemove);

        if (ret.isEmpty()) {
            ret.add(new Equity("No Matches", "", null));
        }

        return ret;
    }

    @ApiMethod(httpMethod = "GET", path = "getEodChain")
    public final OptionChain getEodChain(HttpServletRequest req, @Named("q") String ticker, User user) throws OAuthRequestException {

        ensureLoggedIn(user, req);

        OptionChain ret = ofy().load().type(OptionChain.class).ancestor(Key.create(Equity.class, ticker)).order("-__key__").first().now();

        return ret;
    }

    @ApiMethod(httpMethod = "POST", path = "setWatchlist")
    public final List<Equity> setWatchlist(HttpServletRequest req, @Named("q") String tickers, User user) throws OAuthRequestException {

        ensureLoggedIn(user, req);

        FusionUser fuser = getFusionUser(user.getEmail());

        if (fuser == null) {
            throw new OAuthRequestException("Authenticated user not found in datastore");
        }

        List<Equity> ret = getEquityQuotes(req, tickers, user);

        fuser.setWatchlist(ret);

        ofy().save().entity(fuser).now();

        return ret;
    }

    @ApiMethod(httpMethod = "POST", path = "loginUser")
    public final FusionUser loginUser(HttpServletRequest req, FusionUser fusionUserIn, User user) throws OAuthRequestException {

        ensureLoggedIn(user, req);

        FusionUser ret = getFusionUser(user.getEmail());

        if (ret == null) {
            ret = new FusionUser(user.getEmail(), fusionUserIn.getDisplayName());
            createWatchlist(ret);
        }

        if (TextUtils.isEmpty(ret.getSessionId())) {
            String sessionId = RandomStringUtils.random(32, 0, 0, true, true, null, new SecureRandom());
            ret.setSessionId(sessionId);
        }

        ret.setLastLogin(new Date());

        if (!TextUtils.isEmpty(fusionUserIn.getDisplayName()))
            ret.setDisplayName(fusionUserIn.getDisplayName());

        if (ret.getJoinDate() == null)
            ret.setJoinDate(ret.getLastLogin());

        ofy().save().entity(ret).now();

        return ret;
    }

    @ApiMethod(httpMethod = "POST", path = "setUserData")
    public final void setUserData(HttpServletRequest req, @Named("userDataKey") String key, @Named("userDataValue") String value, User user) throws OAuthRequestException {
        FusionUser fuser = ensureLoggedIn(user, req);
        fuser.setUserData(key, value);
        ofy().save().entity(fuser).now();
    }

    private User getUserFromToken(HttpServletRequest req) throws OAuthRequestException {
        String tokenString = req.getHeader("Authorization").replace("Bearer ", "");

        GoogleIdToken token = null;
        try {
            token = GoogleIdToken.parse(new GsonFactory(), tokenString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (!verifier.verify(token)) {
                if (token.getPayload().getExpirationTimeSeconds() * 1000 < System.currentTimeMillis()) {
                    log.info("ERROR token is expired " + token.getPayload().getExpirationTimeSeconds());
                } else {
                    log.info("Verify failed");
                }
            } else {
                return new User(token.getPayload().getEmail(), "", token.getPayload().getSubject());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        throw new OAuthRequestException("Please authenticate");
    }

    @ApiMethod(httpMethod = "GET", path = "getEquityQuotes")
    public final List<Equity> getEquityQuotes(HttpServletRequest req, @Named("tickers") String tickers, User user) throws OAuthRequestException {

        ensureLoggedIn(user, req);

        return getEquities(tickers);
    }

    private List<Equity> getEquities(String tickers) throws OAuthRequestException {
        Collection<Equity> ret;

        ret = getEquityList(tickers.split(","));

        for (Equity equity : ret) {
            ensureEquityHasStockQuote(equity);
        }

        log.info("Returning list of " + ret.size() + " stock quotes");

        return new ArrayList<>(ret);
    }

    private void ensureEquityHasStockQuote(Equity equity) {
        if (equity.getEodStockQuote() == null || equity.getEodStockQuote().getPreviousClose() == 0d) {
            populateEodStockQuote(equity);
        }
    }

    private void populateEodStockQuote(Equity equity) {
        List<StockQuote> quotes = ofy().load().type(StockQuote.class).ancestor(equity).order("-__key__").limit(2).list();
        if (quotes == null || quotes.isEmpty())
            return;

        StockQuote newStockQuote = quotes.get(0);
        StockQuote existingEodStockQuote = equity.getEodStockQuote();

        if (existingEodStockQuote == null || existingEodStockQuote.getDataTimestamp() < newStockQuote.getDataTimestamp()) {
            if (newStockQuote.getPreviousClose() == 0d && quotes.size() > 1)
                newStockQuote.setPreviousClose(quotes.get(1).getClose());

            equity.setEodStockQuote(newStockQuote);

            ofy().save().entity(equity);
        }
    }

    private void createWatchlist(FusionUser fusionUser) {
        List<Equity> equityList = getEquityList(new String[]{"SPY", "AAPL", "AMZN", "CSCO", "FB", "GOOG", "NFLX", "TSLA"});
        fusionUser.setWatchlist(equityList);
    }

    private Equity getEquity(String ticker) {
        return ofy().load().key(Key.create(Equity.class, ticker)).now();
    }

    private List<Equity> getEquityList(String[] tickers) {
        return getEquityList(Arrays.asList(tickers));
    }

    private List<Equity> getEquityList(List<String> tickers) {
        List<Key<Equity>> keyList = new ArrayList<>();

        for (String ticker : tickers) {
            if (TextUtils.isEmpty(ticker))
                continue;
            keyList.add(Key.create(Equity.class, ticker));
        }

        if (keyList.isEmpty())
            return new ArrayList<>();

        return new ArrayList<>(ofy().load().keys(keyList).values());
    }

    private StockQuote getStockQuote(String ticker, DateTime dateTime) {
        if (TextUtils.isEmpty(ticker))
            return null;

        Key<Equity> equityKey = Key.create(Equity.class, ticker);
        return ofy().load().key(Key.create(equityKey, StockQuote.class, dateTime.getMillis())).now();
    }

    private FusionUser getFusionUser(String email) {
        FusionUser ret = ofy().load().key(Key.create(FusionUser.class, email))
                .now();
        return ret;
    }

    private FusionUser getFusionUserBySessionId(String sessionId) {
        if (TextUtils.isEmpty(sessionId))
            return null;

        FusionUser ret = ofy().load().type(FusionUser.class)
                .filter(new FilterPredicate("sessionId", Query.FilterOperator.EQUAL, sessionId))
                .first()
                .now();

        return ret;
    }

    private static Filter startsWithFilter(String field, String q) {
        return CompositeFilterOperator.and(
                new FilterPredicate(field, GREATER_THAN_OR_EQUAL, q),
                new FilterPredicate(field, LESS_THAN, q + Character.MAX_VALUE));
    }

    private FusionUser ensureLoggedIn(User user, HttpServletRequest req) throws OAuthRequestException {
        String sessionId = req.getHeader("SessionId");
        FusionUser fusionUser = getFusionUserBySessionId(sessionId);

        if (fusionUser != null) {
            return fusionUser;
        }

        if (user == null)
            log.severe("User is null");

        if (user == null) {
            user = getUserFromToken(req);

            if (user != null)
                log.info("Built user " + user.getEmail() + " | " + user.getUserId());
        }

        if (user == null)
            throw new OAuthRequestException("Please authenticate first");

        return getFusionUser(user.getEmail());
    }
}
