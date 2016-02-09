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
import com.google.appengine.api.users.User;
import com.googlecode.objectify.cmd.Query;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class SymbolLookup {

    private static final int MAX_RESULTS = 20;

    @ApiMethod(httpMethod = "GET")
    public final List<Equity> getMatching(@Named("q") String searchString, User user) {

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

    Filter startsWithFilter(String field, String q) {
        return CompositeFilterOperator.and(
                new FilterPredicate(field, GREATER_THAN_OR_EQUAL, q),
                new FilterPredicate(field, LESS_THAN, q + Character.MAX_VALUE));
    }
}
