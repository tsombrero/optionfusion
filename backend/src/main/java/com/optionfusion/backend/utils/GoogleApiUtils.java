package com.optionfusion.backend.utils;

import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class GoogleApiUtils {
    public static Compute getCompute() throws GeneralSecurityException, IOException {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        AppIdentityCredential credential =
                new AppIdentityCredential(Arrays.asList(ComputeScopes.COMPUTE));

        return new Compute.Builder(
                httpTransport, JSON_FACTORY, null)
                .setApplicationName(Constants.APPLICATION_NAME)
                .setHttpRequestInitializer(credential).build();
    }
}
