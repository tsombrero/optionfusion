package com.optionfusion.backend.admin;

import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.util.Base64;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.backend.utils.Util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.admin.AdminServlet.PASSWORD;
import static com.optionfusion.backend.admin.AdminServlet.USERNAME;
import static com.optionfusion.backend.protobuf.OptionChainProto.OptionQuote.OptionType.CALL;
import static com.optionfusion.backend.protobuf.OptionChainProto.OptionQuote.OptionType.PUT;
import static com.optionfusion.backend.utils.OfyService.ofy;

public class GetEodDataWorkerServlet extends HttpServlet {

    private static final String BASE_URI = "http://www.deltaneutral.com/dailydata/dbupdate/";
    private static final String TEST_URI = "https://dl.dropboxusercontent.com/u/29448741/test.zip";
    private static final String OPTIONS_FILENAME_PREFIX = "options_";

    // 2/10/2016 04:00:00 PM -0500
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("M/d/yyyy hh:mm:ss aa Z");
    private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {

            String username = req.getParameter(USERNAME);
            String password = req.getParameter(PASSWORD);

            ZipInputStream in = new ZipInputStream(getRemoteFileInputStream(username, password, LocalDate.now()));

            ZipEntry zipEntry = in.getNextEntry();

            while (zipEntry != null) {
                try {
                    if (!zipEntry.getName().toLowerCase().startsWith(OPTIONS_FILENAME_PREFIX))
                        continue;

                    processOptionsFile(in);
                    break;
                } finally {
                    zipEntry = in.getNextEntry();
                }
            }
        } catch (Throwable e) {
            resp.getWriter().println("Failed");
            e.printStackTrace();
            throw e;
        }
    }

    enum OptionsColumns {
        UnderlyingSymbol, UnderlyingPrice, Exchange, OptionSymbol, Blank, Type, Expiration, DataDate, Strike, Last, Bid, Ask, Volume, OpenInterest, IV, Delta, Gamma, Theta, Vega;

        private static String[] namesAsArray;

        public static String[] getNamesArray() {
            if (namesAsArray == null) {
                namesAsArray = Util.getEnumNamesArray(values());
            }
            return namesAsArray;
        }

    }

    private void processOptionsFile(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(OptionsColumns.getNamesArray()).parse(reader);

        OptionChainBuilder optionChainBuilder = null;
        String symbol = null;
        for (CSVRecord record : records) {
            symbol = record.get(OptionsColumns.UnderlyingSymbol);
            if (optionChainBuilder == null || !optionChainBuilder.getSymbol().equals(symbol)) {
                commitChain(optionChainBuilder.build());
                optionChainBuilder = new OptionChainBuilder(record);
            }
            optionChainBuilder.addRecord(record);
        }
        commitChain(optionChainBuilder.build());
    }

    private class OptionChainBuilder {
        Map<String, List<CSVRecord>> subchainsByExp = new HashMap<>();
        OptionChainProto.StockQuote stockQuote;

        public OptionChainBuilder(CSVRecord record) {
            stockQuote = newStockQuote(record);
        }

        public void addRecord(CSVRecord record) {
            String exp = record.get(OptionsColumns.Expiration);
            if (!subchainsByExp.containsKey(exp)) {
                subchainsByExp.put(exp, new ArrayList<CSVRecord>());
            }
            subchainsByExp.get(exp).add(record);
        }

        OptionChainProto.OptionChain build() {
            OptionChainProto.OptionChain.Builder ret = OptionChainProto.OptionChain.newBuilder()
                    .setStockquote(stockQuote)
                    .setTimestamp(stockQuote.getTimestamp());

            for (String exp : subchainsByExp.keySet()) {
                ret.addOptionDates(newOptionDateChain(subchainsByExp.get(exp)));
            }

            return ret.build();
        }

        private OptionChainProto.OptionDateChain newOptionDateChain(List<CSVRecord> csvRecords) {
            long exp = EXPIRATION_FORMATTER.parseMillis(csvRecords.get(0).get(OptionsColumns.Expiration));
            OptionChainProto.OptionDateChain.Builder dateChainBuilder = OptionChainProto.OptionDateChain.newBuilder();
            dateChainBuilder.setExpiration(exp);
            for (CSVRecord record : csvRecords) {
                dateChainBuilder.addOptions(newOptionQuote(record));
            }
            return dateChainBuilder.build();
        }

        private OptionChainProto.OptionQuote newOptionQuote(CSVRecord record) {
            return OptionChainProto.OptionQuote.newBuilder()
                    .setAsk(Double.valueOf(record.get(OptionsColumns.Ask)))
                    .setBid(Double.valueOf(record.get(OptionsColumns.Bid)))
                    .setDelta(Double.valueOf(record.get(OptionsColumns.Delta)))
                    .setGamma(Double.valueOf(record.get(OptionsColumns.Gamma)))
                    .setIv(Double.valueOf(record.get(OptionsColumns.IV)))
                    .setLast(Double.valueOf(record.get(OptionsColumns.Last)))
                    .setOpenInterest(Integer.valueOf(record.get(OptionsColumns.OpenInterest)))
                    .setOptionType(record.get(OptionsColumns.Type).toUpperCase().startsWith("C") ? CALL : PUT)
                    .setStrike(Double.valueOf(record.get(OptionsColumns.Strike)))
                    .setVolume(Integer.valueOf(record.get(OptionsColumns.Volume)))
                    .build();
        }

        public String getSymbol() {
            return stockQuote.getSymbol();
        }
    }

    private OptionChainProto.StockQuote newStockQuote(CSVRecord record) {
        long timestamp = TIMESTAMP_FORMATTER.parseMillis(record.get(OptionsColumns.DataDate) + " -0500");

        return OptionChainProto.StockQuote.newBuilder()
                .setSymbol(record.get(OptionsColumns.UnderlyingSymbol))
                .setClose(Double.valueOf(record.get(OptionsColumns.UnderlyingPrice)))
                .setTimestamp(timestamp)
                .build();
    }

    private void commitChain(OptionChainProto.OptionChain currentChain) {
        List<OptionChain> existingOptionChain = ofy().load().type(OptionChain.class)
                .filter(Query.CompositeFilterOperator.and(
                        new Query.FilterPredicate("symbol", Query.FilterOperator.EQUAL, currentChain.getStockquote().getSymbol()),
                        new Query.FilterPredicate("timestamp", Query.FilterOperator.EQUAL, currentChain.getTimestamp())
                ))
                .limit(1)
                .list();

        if (existingOptionChain.isEmpty()) {
            ofy().save().entity(new OptionChain(currentChain)).now();
        }
    }

    private InputStream getRemoteFileInputStream(String username, String password, LocalDate date) throws IOException {
        URL url = new URL(getFileUri(date));
        HttpURLConnection uc = (HttpURLConnection) url.openConnection();
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(Base64.encode(userpass.getBytes(), 0, userpass.length(), Base64.getWebsafeAlphabet(), true));
        uc.setRequestProperty("Authorization", basicAuth);
        uc.setConnectTimeout(60000);
        uc.setReadTimeout(120000);
        uc.connect();
        if (uc.getResponseCode() >= 400 && date.isAfter(LocalDate.now().minusDays(10))) {
            return getRemoteFileInputStream(username, password, date.minusDays(1));
        }

        return uc.getInputStream();
    }

    public String getFileUri(LocalDate date) {
        return TEST_URI;
//        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
//                .appendYear(4, 4)
//                .appendMonthOfYear(2)
//                .appendDayOfMonth(2)
//                .toFormatter();
//
//        return BASE_URI + "options_" + date.toString(dateTimeFormatter) + ".zip";
    }

}
