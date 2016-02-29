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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.oauth.OAuthServiceFailureException;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.cmd.Query;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.FusionUser;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.utils.Constants;
import com.optionfusion.backend.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
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

    @ApiMethod(httpMethod = "GET")
    public final List<Equity> getTickersMatching(@Named("q") String searchString, User user) {

        ArrayList<Equity> ret = new ArrayList<>();

        List<Equity> equities = ofy().load().type(Equity.class)
                .filter(startsWithFilter("ticker", searchString.toUpperCase()))
                .limit(MAX_RESULTS)
                .list();

        if (equities != null) {
            ret.addAll(equities);
            Collections.sort(ret, Equity.TICKER_COMPARATOR);
        }

        if (ret.size() < MAX_RESULTS) {
            equities = ofy().load().type(Equity.class)
                    .filter(startsWithFilter("keywords", searchString.toLowerCase()))
                    .limit(MAX_RESULTS - ret.size())
                    .list();

            if (equities != null) {
                equities.removeAll(ret);
                Collections.sort(equities, Equity.TICKER_COMPARATOR);
                ret.addAll(equities);
            }
        }

        if (ret.isEmpty()) {
            ret.add(new Equity("No Matches", "", null));
        }

        return ret;
    }

    private static final Logger log = Logger.getLogger(OptionDataApi.class.getSimpleName());

    @ApiMethod(httpMethod = "GET")
    public final OptionChain getEodChain(@Named("q") String ticker, User user) {

        if (user != null) {
            log.info("User e=" + user.getEmail() + ":n=" + user.getNickname() + ":id=" + user.getUserId() + ":fid=" + user.getFederatedIdentity() + ":ad=" + user.getAuthDomain() + " requested eod chain " + ticker);
        } else {
            log.info("Null user requested eod chain " + ticker);
        }

        List<OptionChain> ret = ofy().load().type(OptionChain.class)
                .filter(new FilterPredicate("symbol", EQUAL, ticker))
                .order("-quote_timestamp")
                .limit(1)
                .list();

        if (!ret.isEmpty()) {
            return ret.get(0);
        }
        return null;
    }

    protected User getAuthenticatedUser() {
        OAuthService oauth = OAuthServiceFactory.getOAuthService();
        String scope = "https://www.googleapis.com/auth/userinfo.email";
        Set<String> allowedClients = new HashSet<>();
        allowedClients.add("386969647168-q03ig3s5jmp26onj5bfn0bobuvpatdm2.apps.googleusercontent.com"); // list your client ids here
        allowedClients.add("386969647168-j056r2g7hkuk0f11mm1eribp3cmd8p6b.apps.googleusercontent.com"); // list your client ids here
        allowedClients.add("386969647168-mc9ps4shq75slsuajtjuaj41nesabuva.apps.googleusercontent.com"); // list your client ids here

        try {
            User user = oauth.getCurrentUser(scope);
            String tokenAudience = oauth.getClientId(scope);
            if (!allowedClients.contains(tokenAudience)) {
                throw new OAuthRequestException("audience of token '" + tokenAudience
                        + "' is not in allowed list " + allowedClients);
            }
            return user;
        } catch (OAuthRequestException ex) {
        } catch (OAuthServiceFailureException ex) {
        }
        return null;
    }

    Filter buildFilterForLatestChain(String ticker) {
        return CompositeFilterOperator.and(
                new FilterPredicate("symbol", EQUAL, ticker),
                new FilterPredicate("quote_timestamp", com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL, Util.getEodDateTime().getMillis()));
    }

    Filter startsWithFilter(String field, String q) {
        return CompositeFilterOperator.and(
                new FilterPredicate(field, GREATER_THAN_OR_EQUAL, q),
                new FilterPredicate(field, LESS_THAN, q + Character.MAX_VALUE));
    }

    @ApiMethod(httpMethod = "GET")
    public final FusionUser getUserData(User user) {
        List<FusionUser> ret = ofy().load().type(FusionUser.class)
                .filter(new FilterPredicate("email", com.google.appengine.api.datastore.Query.FilterOperator.EQUAL, user.getUserId()))
                .limit(1)
                .list();
        if (!ret.isEmpty()) {
            return ret.get(0);
        }
        //TODO create a new User object with some default stuff
        return null;
    }
}
