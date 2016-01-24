/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package com.optionfusion.backend.apis;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.optionfusion.backend.models.Symbol;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

/** An endpoint class we are exposing */
@Api(
  name = "optionFusion",
  version = "v1",
  namespace = @ApiNamespace(
    ownerDomain = "backend.com.optionfusion.com",
    ownerName = "backend.com.optionfusion.com",
    packagePath=""
  )
)
public class SymbolLookup {

    @ApiMethod(httpMethod="GET")
    public final List<Symbol> getMatching(@Named("q") String searchString) {
        return Collections.singletonList(new Symbol("FOO", "Foobar & Co"));
    }



}
