package com.optionfusion.backend.admin;

import com.google.appengine.repackaged.org.joda.time.LocalDate;
import com.google.appengine.repackaged.org.joda.time.format.DateTimeFormatter;
import com.google.appengine.repackaged.org.joda.time.format.DateTimeFormatterBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.admin.AdminServlet.PASSWORD;
import static com.optionfusion.backend.admin.AdminServlet.USERNAME;

public class GetEodDataWorkerServlet extends HttpServlet {

    private final String BASE_URI = "http://www.deltaneutral.com/dailydata/dbupdate/";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String username = req.getParameter(USERNAME);
        String password = req.getParameter(PASSWORD);

        ZipInputStream in = new ZipInputStream(getZippedFileStream(username, password));



    }

    public InputStream getZippedFileStream(String username, String password) throws IOException {
        URL url = new URL(getFileUri(LocalDate.now()));

        URLConnection uc = url.openConnection();
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        uc.setRequestProperty("Authorization", basicAuth);
        return uc.getInputStream();
    }

    public String getFileUri(LocalDate date) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendMonthOfYear(2)
                .appendDayOfMonth(2)
                .toFormatter();

        return BASE_URI + "options_" + date.toString(dateTimeFormatter);
    }

}
