package com.optionfusion.backend.admin;

import com.google.appengine.api.datastore.Query;
import com.google.gcloud.storage.Blob;
import com.google.gcloud.storage.BlobId;
import com.google.gcloud.storage.Storage;
import com.google.gcloud.storage.StorageOptions;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.backend.utils.Util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.optionfusion.backend.protobuf.OptionChainProto.OptionQuote.OptionType.CALL;
import static com.optionfusion.backend.protobuf.OptionChainProto.OptionQuote.OptionType.PUT;
import static com.optionfusion.backend.utils.OfyService.ofy;

public class GetEodDataWorkerServlet extends HttpServlet {

    // 2/10/2016 04:00:00 PM -0500
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormat.forPattern("M/d/yyyy hh:mm:ss aa Z");
    private static final DateTimeFormatter EXPIRATION_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        try {
            DateTime todayEod = Util.getEodDateTime();

            for (int i = 0; i < 90; i++) {
                DateTime fetchDate = todayEod.minusDays(i);
                if (fetchDate.isAfter(DateTime.now()))
                    continue;

                if (chainsExistForDate(fetchDate, "AAPL"))
                    break;

                processOptionsFileForDate(fetchDate);
            }


        } catch (Throwable e) {
            resp.getWriter().println("Failed");
            e.printStackTrace();
            throw e;
        }
    }

    private void processOptionsFileForDate(DateTime dateTime) throws IOException {
        InputStream in = getRemoteFileInputStream(dateTime);

        if (in == null) {
            log("No options input data for date " + dateTime);
            return;
        }

        Reader reader = new InputStreamReader(in);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader(OptionsColumns.getNamesArray()).parse(reader);

        OptionChainBuilder optionChainBuilder = null;
        String symbol = null;
        String exchange = null;
        for (CSVRecord record : records) {
            symbol = record.get(OptionsColumns.UnderlyingSymbol);
            exchange = record.get(OptionsColumns.Exchange);

            if (!"*".equals(exchange)
                    && !"W".equals(exchange)
                    && !"Q".equals(exchange))
                continue;

            if (optionChainBuilder == null || !optionChainBuilder.getSymbol().equals(symbol)) {
                if (optionChainBuilder != null)
                    commitChain(optionChainBuilder.build());
                optionChainBuilder = new OptionChainBuilder(newStockQuote(record));
            }
            optionChainBuilder.addRecord(record);
        }
        commitChain(optionChainBuilder.build());
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

    private class OptionChainBuilder {
        Map<String, List<CSVRecord>> subchainsByExp = new HashMap<>();
        OptionChainProto.StockQuote stockQuote;

        public OptionChainBuilder(OptionChainProto.StockQuote stockQuote) {
            this.stockQuote = stockQuote;
        }

        public void addRecord(CSVRecord record) {
            if ("0".equals(record.get(OptionsColumns.OpenInterest)) && "0".equals(record.get(OptionsColumns.Volume)))
                return;

            if (Double.valueOf(record.get(OptionsColumns.Ask)) < 0.05D && Double.valueOf(record.get(OptionsColumns.Bid)) < 0.05D)
                return;

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
            OptionChainProto.OptionQuote.Builder builder =
                    OptionChainProto.OptionQuote.newBuilder()
                            .setAsk(Double.valueOf(record.get(OptionsColumns.Ask)))
                            .setBid(Double.valueOf(record.get(OptionsColumns.Bid)))
                            .setOptionType(record.get(OptionsColumns.Type).toUpperCase().startsWith("C") ? CALL : PUT)
                            .setStrike(Double.valueOf(record.get(OptionsColumns.Strike)))

                            .setOpenInterest(Integer.valueOf(record.get(OptionsColumns.OpenInterest)))
                            .setLast(Double.valueOf(record.get(OptionsColumns.Last)))
                            .setVolume(Integer.valueOf(record.get(OptionsColumns.Volume)));

            Double val = Double.valueOf(record.get(OptionsColumns.Delta));
            if (val != 0D)
                builder.setDelta(val);

            val = Double.valueOf(record.get(OptionsColumns.Gamma));
            if (val != 0D)
                builder.setGamma(val);

            val = Double.valueOf(record.get(OptionsColumns.IV));
            if (val != 0D)
                builder.setIv(val);

            return builder.build();
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

    private InputStream getRemoteFileInputStream(DateTime date) throws IOException {
        getFileName(date);
        Storage storage = StorageOptions.defaultInstance().service();
        BlobId blobId = BlobId.of("optionfusion_com", getFileName(date));
        Blob blob = storage.get(blobId);

        if (blob != null) {
            try {
                InputStream stream = Channels.newInputStream(blob.reader());
                ZipInputStream zipInputStream = new ZipInputStream(stream);
                zipInputStream.getNextEntry();
                return zipInputStream;
            } catch (Exception e) {
                log(e.toString());
            }
        }
        return null;
    }

    private boolean chainsExistForDate(DateTime date, String symbol) {
        List<OptionChain> existingOptionChain = ofy().load().type(OptionChain.class)
                .filter(Query.CompositeFilterOperator.and(
                        new Query.FilterPredicate("symbol", Query.FilterOperator.EQUAL, symbol),
                        new Query.FilterPredicate("timestamp", Query.FilterOperator.EQUAL, date.getMillis())
                ))
                .limit(1)
                .list();

        return !existingOptionChain.isEmpty();
    }

    public String getFileName(DateTime date) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendMonthOfYear(2)
                .appendDayOfMonth(2)
                .toFormatter();

        return "csv/options_" + date.toString(dateTimeFormatter) + ".csv.zip";
    }
}
