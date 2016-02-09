package com.optionfusion.backend.admin;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AdminServlet extends HttpServlet {

    public static final String LOOKUP_CSV_FILE_URI = "lookupCsvFile";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Queue queue = QueueFactory.getDefaultQueue();
        if (req.getParameter(LOOKUP_CSV_FILE_URI) != null) {
            TaskHandle t = queue.add(TaskOptions.Builder.withUrl("/adminworker").param(LOOKUP_CSV_FILE_URI, req.getParameter(LOOKUP_CSV_FILE_URI)));
            resp.getWriter().println("Job added to queue: " + t.getName());
        }
        resp.getWriter().println("Tasks in progress: " + queue.fetchStatistics().getRequestsInFlight());
    }
}
