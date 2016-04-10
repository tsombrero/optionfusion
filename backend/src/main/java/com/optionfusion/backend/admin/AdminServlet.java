package com.optionfusion.backend.admin;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.repackaged.org.joda.time.DateTimeConstants;
import com.google.appengine.repackaged.org.joda.time.LocalDate;
import com.googlecode.objectify.Key;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.utils.Constants;
import com.optionfusion.backend.utils.GoogleApiUtils;
import com.optionfusion.backend.utils.Util;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.utils.OfyService.ofy;

public class AdminServlet extends HttpServlet {

    public static final String LOOKUP_CSV_FILE_URI = "lookupCsvFile";

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
                    boolean dataDownload = populateBlobStorage(resp);
                    getEodData(days, dataDownload);
                }
            } catch (Exception e) {
                log("Failed", e);
            }
        }
        resp.getWriter().write("Work submitted");
    }

    // Download the csv data from the provider by starting the compute instance and letting it do its thing. It shuts down automatically.
    // The getEodData tasks are delayed 5 mins to give this time to finish.
    private boolean populateBlobStorage(HttpServletResponse resp) {

        boolean needsData = false;
        for (int i = 0; i < 10; i++) {
            DateTime quoteDay = Util.getEodDateTime().minusDays(i);

            if (quoteDay.isAfterNow())
                continue;

            if (isMarketOpenOn(quoteDay) && !chainsExistForDate(quoteDay)) {
                needsData = true;
                break;
            }
        }

        if (!needsData)
            return false;

        try {
            Compute compute = GoogleApiUtils.getCompute();

            resp.getWriter().write("Starting " + Constants.EOD_DOWNLOADER_INSTANCE);

            Operation i = compute.instances().start(Constants.APPLICATION_NAME, Constants.EOD_DOWNLOADER_INSTANCE_ZONE, Constants.EOD_DOWNLOADER_INSTANCE).execute();

            if (i != null) {
                resp.getWriter().write(i + " | " + i.getError() + " | " + i.getHttpErrorStatusCode());
            }

            return true;
        } catch (Throwable t) {
            log("Failed populating blob storage", t);
            try {
                resp.getWriter().write("Failed populating blob storage " + t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return needsData;
    }

    public static LocalDate[] MARKET_HOLIDAYS = new LocalDate[]{
            new LocalDate(2016, 5, 30),
            new LocalDate(2016, 7, 4),
            new LocalDate(2016, 9, 5),
            new LocalDate(2016, 11, 24),
            new LocalDate(2016, 12, 26),
            new LocalDate(2017, 1, 2),
            new LocalDate(2017, 1, 16),
            new LocalDate(2017, 2, 20),
            new LocalDate(2017, 4, 14),
            new LocalDate(2017, 5, 29),
            new LocalDate(2017, 7, 4),
            new LocalDate(2017, 9, 4),
            new LocalDate(2017, 11, 23),
            new LocalDate(2017, 12, 25)
    };

    private boolean isMarketOpenOn(DateTime quoteDay) {
        if (quoteDay.getDayOfWeek() == DateTimeConstants.SATURDAY || quoteDay.getDayOfWeek() == DateTimeConstants.SUNDAY)
            return false;

        for (LocalDate holiday : MARKET_HOLIDAYS) {
            if (quoteDay.getYear() == holiday.getYear()
                    && quoteDay.getDayOfYear() == holiday.getDayOfYear())
                return false;
        }
        return true;
    }

    private void getEodData(int daysToSearch, boolean dataDownload) {
        DateTime todayEod = Util.getEodDateTime();
        DateTime quoteDate = todayEod.minusDays(daysToSearch);

        int dayCounter = 0;

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

            if (dayCounter >= 10) {
                log("processed 10 days, exiting");
                break;
            }
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
