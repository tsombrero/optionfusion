package com.optionfusion.backend.utils;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.Option;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.models.Position;
import com.optionfusion.backend.models.StockQuote;
import com.optionfusion.backend.models.User;
import com.optionfusion.backend.models.VerticalSpread;

/**
 * Objectify service wrapper so we can statically register our persistence classes. More on
 * Objectify here : https://code.google.com/p/objectify-appengine/
 */
public final class OfyService {
    /**
     * Default constructor, never called.
     */
    private OfyService() {
    }

    static {
        factory().register(Equity.class);
        factory().register(Option.class);
        factory().register(Position.class);
        factory().register(StockQuote.class);
        factory().register(User.class);
        factory().register(VerticalSpread.class);
        factory().register(OptionChain.class);
    }

    /**
     * Returns the Objectify service wrapper.
     *
     * @return The Objectify service wrapper.
     */
    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    /**
     * Returns the Objectify factory service.
     *
     * @return The factory service.
     */
    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
