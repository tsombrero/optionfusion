package com.optionfusion.backend.utils;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.optionfusion.backend.models.Symbol;

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
        factory().register(Symbol.class);
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
