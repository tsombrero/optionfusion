package com.optionfusion.backend.admin;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.compute.ComputeCredential;
import com.google.api.client.googleapis.extensions.appengine.auth.oauth2.AppIdentityCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Operation;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.utils.Util;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.utils.OfyService.ofy;

public class AdminServlet extends HttpServlet {

    public static final String APPLICATION_NAME = "option-fusion-api";
    public static final String EOD_DOWNLOADER_INSTANCE = "eod-downloader-instance";
    public static final String EOD_DOWNLOADER_INSTANCE_ZONE = "us-central1-b";
    public static final String LOOKUP_CSV_FILE_URI = "lookupCsvFile";
    public static final String clientSecretsJson = "{" +
            "  \"installed\": {" +
            "    \"client_id\": \"Enter Client ID\"," +
            "    \"client_secret\": \"Enter Client Secret\"" +
            "  }" +
            "}";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        if (req.getParameter(LOOKUP_CSV_FILE_URI) != null) {
            Queue queue = QueueFactory.getDefaultQueue();
            TaskHandle t = queue.add(TaskOptions.Builder.withUrl("/equitylookupdataworker").param(LOOKUP_CSV_FILE_URI, req.getParameter(LOOKUP_CSV_FILE_URI)));
            resp.getWriter().println("Equity Lookup Data Job added to queue: " + t.getName());
        } else if (req.getParameter(GetEodDataWorkerServlet.PARAM_DAYS_TO_SEARCH) != null) {
            try {
                Integer days = Integer.valueOf(req.getParameter(GetEodDataWorkerServlet.PARAM_DAYS_TO_SEARCH));
                if (days != null) {
                    populateBlobStorage(resp);
                    getEodData(days);
                }
            } catch (Exception e) {
                log("Failed", e);
            }
        }
        resp.getWriter().write("Work submitted");
    }

    // Download the csv data from the provider by starting the compute instance and letting it do its thing. It shuts down automatically.
    // The getEodData tasks are delayed 5 mins to give this time to finish.
    private void populateBlobStorage(HttpServletResponse resp) {

        try {
            JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            AppIdentityCredential credential =
                    new AppIdentityCredential(Arrays.asList(ComputeScopes.COMPUTE));

            Compute compute = new Compute.Builder(
                    httpTransport, JSON_FACTORY, null)
                    .setApplicationName(APPLICATION_NAME)
                    .setHttpRequestInitializer(credential).build();

            resp.getWriter().write("Starting " + EOD_DOWNLOADER_INSTANCE);

            Operation i = compute.instances().start(APPLICATION_NAME, EOD_DOWNLOADER_INSTANCE_ZONE, EOD_DOWNLOADER_INSTANCE).execute();
            if (i != null) {
                resp.getWriter().write(i + " | " + i.getError() + " | " + i.getHttpErrorStatusCode());
            }
        } catch (Throwable t) {
            log("Failed populating blob storage", t);
            try {
                resp.getWriter().write("Failed populating blob storage " + t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getEodData(int daysToSearch) {
        DateTime todayEod = Util.getEodDateTime();
        DateTime quoteDate = todayEod.minusDays(daysToSearch);

        int dayCounter = 1;

        while (!quoteDate.isAfter(DateTime.now())) {
            if (Util.getBlobFromStorage(Util.getOptionsFileName(quoteDate)) != null) {
                if (!chainsExistForDate(quoteDate)) {
                    getEodDataShards(quoteDate, dayCounter);
                    dayCounter++;
                } else {
                    log("Already got chains for date " + quoteDate);
                }
            } else {
                log("No data for " + quoteDate);
            }
            quoteDate = quoteDate.plusDays(1);

//            if (dayCounter >= 10) {
//                log("processed 10 days, exiting");
//                break;
//            }
        }
    }

    private void getEodDataShards(DateTime dateTime, int dayCounter) {

        Queue queue = QueueFactory.getQueue("eoddataqueue");

        for (char c = 'A'; c <= 'Z'; c++) {
            queue.add(TaskOptions.Builder.withUrl("/eoddataworker")
                    .param(GetEodDataWorkerServlet.PARAM_DATE_TO_SEARCH, String.valueOf(dateTime.getMillis()))
                    .param(GetEodDataWorkerServlet.PARAM_INITIAL_LETTER_SHARD, String.valueOf(c))
                    .countdownMillis(dayCounter * TimeUnit.MINUTES.toMillis(4))
            );
            log("Added job for " + c + " " + dateTime);
        }
    }

    private boolean chainsExistForDate(DateTime date) {
        try {
            Map<Key<OptionChain>, OptionChain> chains = ofy().cache(false).load()
                    .keys(
                            Util.getOptionChainKey("ZX", date),
                            Util.getOptionChainKey("ZUMZ", date),
                            Util.getOptionChainKey("ZTS", date));

            return chains.size() > 0;
        } catch (Exception e) {
            log("Failed checking for chains by date", e);
        }

        return false;
    }
}
