package com.optionfusion.backend.admin;

import com.google.appengine.api.datastore.Query;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.FusionUser;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN;
import static com.optionfusion.backend.utils.OfyService.ofy;


public class PingServlet extends HttpServlet {
    private static Query.Filter startsWithFilter(String field, String q) {
        return Query.CompositeFilterOperator.and(
                new Query.FilterPredicate(field, GREATER_THAN_OR_EQUAL, q),
                new Query.FilterPredicate(field, LESS_THAN, q + Character.MAX_VALUE));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long t = System.currentTimeMillis();

        List<Equity> list = ofy().load().type(Equity.class)
                .filter(startsWithFilter(Equity.SYMBOL, "AA"))
                .limit(5)
                .list();

        if (list == null || list.isEmpty()) {
            resp.getWriter().print("Symbol lookup failed");
        }

        FusionUser user = ofy().load().type(FusionUser.class)
                .filter(new Query.FilterPredicate("sessionId", Query.FilterOperator.EQUAL, "G2Ar92whOPzLEa4mrsOpDowrAPuZx4K8"))
                .first()
                .now();

        if (user == null) {
            resp.getWriter().print("User lookup failed");
        }

        resp.getWriter().print((System.currentTimeMillis() - t) + "ms");
    }

}
