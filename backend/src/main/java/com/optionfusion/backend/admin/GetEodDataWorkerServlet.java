package com.optionfusion.backend.admin;

import com.google.appengine.api.datastore.Query;
import com.google.gcloud.storage.Blob;
import com.google.gcloud.storage.BlobId;
import com.google.gcloud.storage.Storage;
import com.google.gcloud.storage.StorageOptions;
import com.googlecode.objectify.Key;
import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.models.StockQuote;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.backend.utils.TextUtils;
import com.optionfusion.backend.utils.Util;

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
import java.util.Collections;
import java.util.Date;
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
    private static final DateTimeFormatter STOCKQUOTE_DATE_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    public static final String PARAM_DAYS_TO_SEARCH = "DAYS_TO_SEARCH";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        log("Job Starting " + req.getRequestedSessionId());
        int daysToSearch = 1;

        String p = req.getParameter(PARAM_DAYS_TO_SEARCH);
        if (!TextUtils.isEmpty(p)) {
            daysToSearch = Math.max(daysToSearch, Integer.valueOf(p));
        }

        try {
            DateTime todayEod = Util.getEodDateTime();

            for (int i = daysToSearch - 1; i > 0; i--) {
                DateTime quoteDate = todayEod.minusDays(i);
                if (quoteDate.isAfter(DateTime.now()))
                    break;

                if (chainsExistForDate(quoteDate, "ZX") || chainsExistForDate(quoteDate, "ZUMZ") || chainsExistForDate(quoteDate, "ZTS")) {
                    log("Already processed options for date " + quoteDate);
                    continue;
                }

                processFilesForDate(quoteDate);
            }
        } catch (Throwable e) {
            resp.getWriter().println("Failed");
            e.printStackTrace();
            throw e;
        }
        log("Job Finished");
    }

    private void processFilesForDate(DateTime dateTime) throws IOException {
        InputStream optionsIn = null;
        InputStream stocksIn = null;
        CSVIterator optionRecordIterator = null;
        CSVIterator stockQuoteRecordIterator = null;

        try {
            optionsIn = getRemoteFileInputStream(getOptionsFileName(dateTime));
            stocksIn = getRemoteFileInputStream(getStockQuotesFileName(dateTime));

            if (optionsIn == null) {
                log("No options input data for date " + dateTime);
                return;
            }

            Reader chainReader = new InputStreamReader(optionsIn);
            Reader stockQuoteReader = new InputStreamReader(stocksIn);
            optionRecordIterator = new CSVIterator(new CSVReader(chainReader));
            stockQuoteRecordIterator = new CSVIterator(new CSVReader(stockQuoteReader));

            String[] firstOptionRecordForNextSymbol = null;
            OptionChainBuilder currentOptionChainBuilder = null;
            StockQuote currentStockQuote = null;

            // Looping through two iterators. The strategy is:
            // - Keep a reference to the current quote and optionchain
            // - Compare the chain's underlying symbol to the stockquote symbol
            // - If they match, write them together and null the references.
            // - Else write whichever one comes first and null the reference.
            // At most one write per type per iteration

            while (optionRecordIterator.hasNext() || stockQuoteRecordIterator.hasNext()) {
                if (currentOptionChainBuilder == null && optionRecordIterator.hasNext()) {
                    currentOptionChainBuilder = new OptionChainBuilder();
                    firstOptionRecordForNextSymbol = currentOptionChainBuilder.addRecords(firstOptionRecordForNextSymbol, optionRecordIterator);
                }

                if (currentStockQuote == null && stockQuoteRecordIterator.hasNext()) {
                    currentStockQuote = getNextStockQuote(stockQuoteRecordIterator);
                }

                // get the symbols to compare
                String stockQuoteSymbol = null;
                String optionChainSymbol = null;

                if (currentStockQuote != null)
                    stockQuoteSymbol = currentStockQuote.getTicker();

                if (currentOptionChainBuilder != null)
                    optionChainSymbol = currentOptionChainBuilder.getSymbol();

                int compare = TextUtils.compare(optionChainSymbol, stockQuoteSymbol);

                if (compare == 0) {
                    commit(currentOptionChainBuilder, currentStockQuote);
                    currentOptionChainBuilder = null;
                    currentStockQuote = null;
                } else if (compare > 0) {
                    //stockQuoteSymbol is first
                    commit(currentStockQuote);
                    currentStockQuote = null;
                } else {
                    //optionChainSymbol is first
                    commit(currentOptionChainBuilder.build());
                    currentOptionChainBuilder = null;
                }
            }
        } finally {
            if (optionsIn != null)
                optionsIn.close();
            if (stocksIn != null)
                stocksIn.close();
        }
    }

    private StockQuote getNextStockQuote(CSVIterator stockQuoteRecords) {
        if (stockQuoteRecords == null || !stockQuoteRecords.hasNext())
            return null;

        String[] record = stockQuoteRecords.next();

        DateTime dateTime = STOCKQUOTE_DATE_FORMATTER
                .parseDateTime(record[StockQuoteColumns.DATE.ordinal()]);

        dateTime = Util.getEodDateTime(dateTime);

        StockQuote ret = new StockQuote(record[StockQuoteColumns.SYMBOL.ordinal()], dateTime.getMillis());

        ret.setOpen(doubleValueOf(record, StockQuoteColumns.OPEN));
        ret.setClose(doubleValueOf(record, StockQuoteColumns.CLOSE));
        ret.setLo(doubleValueOf(record, StockQuoteColumns.LOW));
        ret.setHi(doubleValueOf(record, StockQuoteColumns.HIGH));
        ret.setVolume(longValueOf(record, StockQuoteColumns.VOLUME));

        return ret;
    }

    private long longValueOf(String[] record, StockQuoteColumns col) {
        try {
            return Long.valueOf(record[col.ordinal()]);
        } catch (Throwable t) {
            log("Failed converting column " + col + " to long");
            return 0l;
        }
    }

    private double doubleValueOf(String[] record, Enum<?> col) {
        try {
            return Double.valueOf(record[col.ordinal()]);
        } catch (Throwable t) {
            log("Failed converting column " + col + " to double");
            return 0d;
        }
    }

    enum OptionsColumns {
        UNDERLYING_SYMBOL, UNDERLYING_PRICE, EXCHANGE, OPTION_SYMBOL, BLANK, TYPE, EXPIRATION, DATA_DATE, STRIKE, LAST, BID, ASK, VOLUME, OPEN_INTEREST, IV, DELTA, GAMMA, THETA, VEGA;

        private static String[] namesAsArray;

        public static String[] getNamesArray() {
            if (namesAsArray == null) {
                namesAsArray = Util.getEnumNamesArray(values());
            }
            return namesAsArray;
        }

    }

    enum StockQuoteColumns {
        SYMBOL, DATE, OPEN, HIGH, LOW, CLOSE, VOLUME;

        private static String[] namesAsArray;

        public static String[] getNamesArray() {
            if (namesAsArray == null) {
                namesAsArray = Util.getEnumNamesArray(values());
            }
            return namesAsArray;
        }

    }

    private class OptionChainBuilder {
        Map<String, List<String[]>> subchainsByExp = new HashMap<>();
        OptionChainProto.StockQuote stockQuote;
        String exchange;

        public OptionChainBuilder(OptionChainProto.StockQuote stockQuote) {
            this.stockQuote = stockQuote;
        }

        public OptionChainBuilder() {
        }

        public String[] addRecords(String[] firstRecord, CSVIterator parser) {
            addRecord(firstRecord);
            // NOTE CSVParser.hasNext() sucks, it increments the iterator and loses the record. Sad clown.
            // FIXME find a better csv iterator
            while (parser.hasNext()) {
                String[] record = parser.next();
                exchange = record[OptionsColumns.EXCHANGE.ordinal()];

                if (!"*".equals(exchange)
                        && !"W".equals(exchange)
                        && !"Q".equals(exchange))
                    continue;

                if (stockQuote == null || TextUtils.equals(stockQuote.getSymbol(), record[OptionsColumns.UNDERLYING_SYMBOL.ordinal()])) {
                    addRecord(record);
                } else {
                    // We've got the first record for the next chain, return it
                    return record;
                }
            }
            return null;
        }

        public void addRecord(String[] record) {
            if (record == null)
                return;

            if (stockQuote == null) {
                long timestamp = TIMESTAMP_FORMATTER.parseMillis(record[OptionsColumns.DATA_DATE.ordinal()] + " -0500");

                stockQuote = OptionChainProto.StockQuote.newBuilder()
                        .setSymbol(record[OptionsColumns.UNDERLYING_SYMBOL.ordinal()])
                        .setClose(doubleValueOf(record, OptionsColumns.UNDERLYING_PRICE))
                        .setTimestamp(timestamp)
                .build();
            }

            if ("0".equals(record[OptionsColumns.OPEN_INTEREST.ordinal()]) && "0".equals(record[OptionsColumns.VOLUME.ordinal()]))
                return;

            if (Double.valueOf(record[OptionsColumns.ASK.ordinal()]) < 0.05D && Double.valueOf(record[OptionsColumns.BID.ordinal()]) < 0.05D)
                return;

            String exp = record[OptionsColumns.EXPIRATION.ordinal()];

            if (!subchainsByExp.containsKey(exp)) {
                subchainsByExp.put(exp, new ArrayList<String[]>());
            }
            subchainsByExp.get(exp).add(record);
        }

        OptionChainProto.OptionChain build() {  //TODO update last close price on skinny stockquote
            OptionChainProto.OptionChain.Builder ret = OptionChainProto.OptionChain.newBuilder()
                    .setStockquote(stockQuote)
                    .setTimestamp(stockQuote.getTimestamp());

            ArrayList<String> exps = new ArrayList<>(subchainsByExp.keySet());
            Collections.sort(exps);

            for (String exp : exps) {
                OptionChainProto.OptionDateChain dateChain = newOptionDateChain(subchainsByExp.get(exp));
                ret.addOptionDates(dateChain);
            }

            return ret.build();
        }

        private OptionChainProto.OptionDateChain newOptionDateChain(List<String[]> csvRecords) {
            long exp = EXPIRATION_FORMATTER.parseMillis(csvRecords.get(0)[OptionsColumns.EXPIRATION.ordinal()]);
            OptionChainProto.OptionDateChain.Builder dateChainBuilder = OptionChainProto.OptionDateChain.newBuilder();
            dateChainBuilder.setExpiration(exp);
            for (String[] record : csvRecords) {
                dateChainBuilder.addOptions(newOptionQuote(record));
            }
            return dateChainBuilder.build();
        }

        private OptionChainProto.OptionQuote newOptionQuote(String[] record) {
            OptionChainProto.OptionQuote.Builder builder =
                    OptionChainProto.OptionQuote.newBuilder()
                            .setAsk(Double.valueOf(record[OptionsColumns.ASK.ordinal()]))
                            .setBid(Double.valueOf(record[OptionsColumns.BID.ordinal()]))
                            .setOptionType(record[OptionsColumns.TYPE.ordinal()].toUpperCase().startsWith("C") ? CALL : PUT)
                            .setStrike(Double.valueOf(record[OptionsColumns.STRIKE.ordinal()]))

                            .setOpenInterest(Integer.valueOf(record[OptionsColumns.OPEN_INTEREST.ordinal()]))
                            .setLast(Double.valueOf(record[OptionsColumns.LAST.ordinal()]))
                            .setVolume(Integer.valueOf(record[OptionsColumns.VOLUME.ordinal()]));

            Double val = doubleValueOf(record, OptionsColumns.DELTA);
            if (val != 0D)
                builder.setDelta(val);

            val = doubleValueOf(record, OptionsColumns.GAMMA);
            if (val != 0D)
                builder.setGamma(val);

            val = doubleValueOf(record, OptionsColumns.IV);
            if (val != 0D)
                builder.setIv(val);

            return builder.build();
        }

        public String getSymbol() {
            if (stockQuote == null)
                return null;

            return stockQuote.getSymbol();
        }

        public void setStockQuote(StockQuote stockQuote) {
            this.stockQuote = OptionChainProto.StockQuote.newBuilder()
                    .setSymbol(stockQuote.getTicker())
                    .setClose(stockQuote.getClose())
                    .setTimestamp(stockQuote.getDataTimestamp())
                    .build();
        }
    }

    private void commit(OptionChainBuilder chainBuilder, StockQuote stockQuote) {
        //TODO transaction
        chainBuilder.setStockQuote(stockQuote);
        commit(chainBuilder.build());
        commit(stockQuote);
    }

    private void commit(OptionChainProto.OptionChain currentChain) {
        OptionChain existingOptionChain = ofy().load().type(OptionChain.class)
                .filter(Query.CompositeFilterOperator.and(
                        new Query.FilterPredicate("symbol", Query.FilterOperator.EQUAL, currentChain.getStockquote().getSymbol()),
                        new Query.FilterPredicate("quote_timestamp", Query.FilterOperator.EQUAL, new Date(currentChain.getTimestamp()))
                ))
                .first().now();

        if (existingOptionChain == null) {
            ofy().save().entity(new OptionChain(currentChain)).now();
        }
    }

    private void commit(StockQuote stockQuote) {
        Equity equity = getEquity(stockQuote.getTicker());
        Key<Equity> equityKey = Key.create(Equity.class, stockQuote.getTicker());

        if (equity == null) {
            equity = new Equity(stockQuote.getTicker(), "No Description", new ArrayList<String>());
            equityKey = ofy().save().entity(equity).now();
        }

        if (equity.getEodStockQuote() == null || equity.getEodStockQuote().getDataTimestamp() < stockQuote.getDataTimestamp()) {
            if (equity.getEodStockQuote() != null) {
                stockQuote.setPreviousClose(equity.getEodStockQuote().getClose());
            }
            equity.setEodStockQuote(stockQuote);
            ofy().save().entity(equity).now();
        }

        StockQuote existingStockQuote = ofy().load().key(Key.create(equityKey, StockQuote.class, stockQuote.getDataTimestamp())).now();
        if (existingStockQuote == null) {
            ofy().save().entity(stockQuote).now();
        }
    }

    private InputStream getRemoteFileInputStream(String fileName) throws IOException {
        Storage storage = StorageOptions.defaultInstance().service();
        BlobId blobId = BlobId.of("optionfusion_com", fileName);
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
                        new Query.FilterPredicate("quote_timestamp", Query.FilterOperator.EQUAL, date.toDate())
                ))
                .limit(1)
                .list();

        return !existingOptionChain.isEmpty();
    }

    public String getOptionsFileName(DateTime date) {
        return "csv/options_" + getDateTimeStringForFilename(date) + ".csv.zip";
    }

    public String getStockQuotesFileName(DateTime date) {
        return "csv/stockquotes_" + getDateTimeStringForFilename(date) + ".csv.zip";
    }

    public String getDateTimeStringForFilename(DateTime date) {
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendYear(4, 4)
                .appendMonthOfYear(2)
                .appendDayOfMonth(2)
                .toFormatter();

        return date.toString(dateTimeFormatter);
    }

    private Equity getEquity(String ticker) {
        return ofy().load().key(Key.create(Equity.class, ticker)).now();
    }


}
