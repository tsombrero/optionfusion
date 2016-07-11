package com.optionfusion.backend.admin;

import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.opencsv.CSVReader;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.common.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.admin.AdminServlet.LOOKUP_CSV_FILE_URI;
import static com.optionfusion.backend.utils.OfyService.ofy;

public class PopulateEquityLookupDbWorkerServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String lookupFileParam = req.getParameter(LOOKUP_CSV_FILE_URI);

        if (lookupFileParam != null) {
            try {
                URL lookupCsvFile = new URL(lookupFileParam);
                InputStream inputStream = lookupCsvFile.openStream();
                Reader in = new InputStreamReader(inputStream);
                CSVReader records = new CSVReader(in);
                for (String[] record : records) {
                    addRecord(record, resp);
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp.getWriter().println("Error " + e);
            }
        }
    }

    enum Columns {
        Ticker, Name
    }

    private void addRecord(String[] record, HttpServletResponse resp) throws IOException {
        String description = getDescription(record);
        String ticker = record[Columns.Ticker.ordinal()];

        if (Strings.isNullOrEmpty(description)) {
            resp.getWriter().println("ERROR no description for ticker " + ticker);
        }

        List<String> keywords = getKeywords(description);

        keywords.remove(description);
        keywords.remove(ticker);

        if (keywords.size() == 0)
            resp.getWriter().println(ticker + " : '" + description + " keywords:[]");

        Equity oldEquity = ofy().load().key(Key.create(Equity.class, ticker)).now();
        if (oldEquity == null) {
            Equity equity = new Equity(ticker, description, keywords);
            ofy().save().entity(equity).now();
        } else if (!TextUtils.equals(description, oldEquity.getDescription())) {
            oldEquity.setDescription(description);
            oldEquity.setKeywords(keywords);
            ofy().save().entity(oldEquity).now();
        }
    }

    //$FIXUP find a csv that doesn't suck or make a rest call to "http://www.google.com/finance/match?matchtype=matchall&q=T"
    static Map<String, String> descriptionFixupMap = new HashMap<>();

    enum DescriptionFixup {
        BGS("B&G Foods, Inc."),
        CFFI("C&F Financial Corp."),
        GK("G&K Services, Inc."),
        HEES("H&E Equipment Services, Inc."),
        HRB("H&R Block, Inc."),
        JJSF("J&J Snack Foods Corp."),
        NBTF("NB&T Financial Group, Inc."),
        PBY("Pep Boys - Manny Moe & Jack"),
        PCG("PG&E Corporation"),
        PFIN("P&F Industries, Inc."),
        SANW("S&W Seed Company"),
        SSNC("SS&C Technologies Holdings, Inc."),
        STBA("S&T Bancorp, Inc."),
        T("AT&T Inc."),
        WTI("W&T Offshore, Inc.");

        DescriptionFixup(String desc) {
            descriptionFixupMap.put(name(), desc);
        }
    }

    static {
        DescriptionFixup foo = DescriptionFixup.BGS;
    }

    private String getDescription(String[] record) {
        String ret = record[Columns.Name.ordinal()];

        if (Strings.isNullOrEmpty(ret)) {
            String symbol = record[Columns.Ticker.ordinal()].toUpperCase().trim();
            ret = descriptionFixupMap.get(symbol);
        }
        return ret;
    }

    private static final List<String> reject = Arrays.asList("", "inc", "corp", "co", "corporation", "ltd", "llc", "L", "C", "P");

    private ArrayList<String> getKeywords(String description) {
        ArrayList<String> ret = new ArrayList<>(Arrays.asList(
                description
                        .toLowerCase()
                        .replaceAll("'", "")
                        .replaceAll("[^A-Za-z0-9&]", " ")
                        .split(" +")));

        ret.removeAll(reject);
        return ret;
    }
}
