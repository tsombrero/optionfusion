package com.optionfusion.backend.admin;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.optionfusion.backend.models.OptionChain;
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
                if (days != null)
                    getEodData(days);
            } catch (Exception e) {
                log("Failed", e);
            }
        }
        resp.getWriter().write("Work submitted");
    }

    private void getEodData(int daysToSearch) {
        DateTime todayEod = Util.getEodDateTime();
        DateTime quoteDate = todayEod.minusDays(daysToSearch);

        int dayCounter = 0;

        while (!quoteDate.isAfter(DateTime.now())) {
            if (Util.getBlobFromStorage(Util.getOptionsFileName(quoteDate)) != null) {
                if (chainsExistForDate(quoteDate)) {
                    log("Already got chains for date " + quoteDate);
                    continue;
                }
                getEodDataShards(quoteDate, dayCounter);
                dayCounter++;
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
                    .countdownMillis(dayCounter * TimeUnit.MINUTES.toMillis(5))
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
