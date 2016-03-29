/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.optionfusion.backend.apis;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
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

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

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

    private static final int MAX_RESULTS = 20;
    private static final Logger log = Logger.getLogger(OptionDataApi.class.getSimpleName());

    @ApiMethod(httpMethod = "GET", path = "getTickersMatching")
    public final List<Equity> getTickersMatching(@Named("q") String searchString, User user) {

        ArrayList<Equity> ret = new ArrayList<>();

        List<Equity> equities = ofy().load().type(Equity.class)
                .filter(startsWithFilter(Equity.SYMBOL, searchString.toUpperCase()))
                .limit(MAX_RESULTS)
                .list();

        if (equities != null) {
            ret.addAll(equities);
            Collections.sort(ret, Equity.TICKER_COMPARATOR);
        }

        if (ret.size() < MAX_RESULTS) {
            equities = ofy().load().type(Equity.class)
                    .filter(startsWithFilter(Equity.KEYWORDS, searchString.toLowerCase()))
                    .limit(MAX_RESULTS - ret.size())
                    .list();

            if (equities != null) {
                equities.removeAll(ret);
                Collections.sort(equities, Equity.TICKER_COMPARATOR);
                ret.addAll(equities);
            }
        }

        List<Equity> toRemove = new ArrayList<>();
        for (Equity equity : equities) {
            if (equity.getEodStockQuote() == null)
                toRemove.add(equity);
        }

        equities.removeAll(toRemove);

        if (ret.isEmpty()) {
            ret.add(new Equity("No Matches", "", null));
        }

        return ret;
    }

    @ApiMethod(httpMethod = "GET", path = "getEodChain")
    public final OptionChain getEodChain(@Named("q") String ticker, User user) throws OAuthRequestException {

        ensureLoggedIn(user);

        Equity equity = getEquity(ticker);

        OptionChain ret = ofy().load().type(OptionChain.class).ancestor(equity).order("-__key__").first().now();

        return ret;
    }

    @ApiMethod(httpMethod = "POST", path = "loginUser")
    public final FusionUser loginUser(FusionUser fusionUserIn, User user) throws OAuthRequestException {

        ensureLoggedIn(user);

        FusionUser ret = ofy().load().type(FusionUser.class)
                .filter(new FilterPredicate("email", FilterOperator.EQUAL, user.getEmail()))
                .first()
                .now();

        if (ret == null) {
            ret = new FusionUser(user.getEmail(), fusionUserIn.getDisplayName());
            createWatchlist(ret);
        }

        ret.setLastLogin(new Date());

        if (!TextUtils.isEmpty(fusionUserIn.getDisplayName()))
            ret.setDisplayName(fusionUserIn.getDisplayName());

        if (ret.getJoinDate() == null)
            ret.setJoinDate(ret.getLastLogin());

        ofy().save().entity(ret).now();

        return ret;
    }

    @ApiMethod(httpMethod = "GET", path = "getEquityQuotes")
    public final List<Equity> getEquityQuotes(@Named("tickers") String tickers, User user) throws OAuthRequestException {

        ensureLoggedIn(user);

        Collection<Equity> ret;

        if (!TextUtils.isEmpty(tickers)) {
            ret = getEquityList(tickers.split(","));
        } else {
            FusionUser fuser = ofy().load().key(Key.create(FusionUser.class, user.getEmail())).now();

            if (fuser == null)
                throw new OAuthRequestException("Authenticated user not found in datastore");

            ret = getEquityList(fuser.getWatchlistTickers());
        }

        for (Equity equity : ret) {
            ensureEquityHasStockQuote(equity);
        }

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
        Collection<Equity> equityList = getEquityList(new String[]{"AAPL", "AMZN", "CSCO", "FB", "GOOG", "NFLX", "TSLA"});
        for (Equity equity : equityList) {
            fusionUser.addEquityToWatchlist(equity);
        }
    }

    private Equity getEquity(String ticker) {
        return ofy().load().key(Key.create(Equity.class, ticker)).now();
    }

    private Collection<Equity> getEquityList(String[] tickers) {
        return getEquityList(Arrays.asList(tickers));
    }

    private List<Equity> getEquityList(List<String> tickers) {
        List<Key<Equity>> keyList = new ArrayList<>();

        for (String ticker : tickers) {
            keyList.add(Key.create(Equity.class, ticker));
        }

        return new ArrayList<>(ofy().load().keys(keyList).values());
    }

    private StockQuote getStockQuote(String ticker, DateTime dateTime) {
        Key<Equity> equityKey = Key.create(Equity.class, ticker);
        return ofy().load().key(Key.create(equityKey, StockQuote.class, dateTime.getMillis())).now();
    }

    private static Filter startsWithFilter(String field, String q) {
        return CompositeFilterOperator.and(
                new FilterPredicate(field, GREATER_THAN_OR_EQUAL, q),
                new FilterPredicate(field, LESS_THAN, q + Character.MAX_VALUE));
    }

    private void ensureLoggedIn(User user) throws OAuthRequestException {
        if (user == null)
            throw new OAuthRequestException("Please authenticate first");
    }
}
