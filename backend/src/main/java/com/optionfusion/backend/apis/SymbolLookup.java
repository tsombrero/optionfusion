/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.optionfusion.backend.apis;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.users.User;
import com.optionfusion.backend.models.Symbol;
import com.optionfusion.backend.utils.Constants;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

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

    @ApiMethod(httpMethod = "GET")
    public final List<Symbol> getMatching(@Named("q") String searchString, User user) {
        if (user == null) {
            return Collections.singletonList(new Symbol("FOO", "Foobar & Co [null]"));
        }
        return Collections.singletonList(new Symbol("FOO", "Foobar & Co " + user.getEmail() + " " + user.getNickname() + " " + user.getUserId() + " " + user.getAuthDomain()));
    }


}
