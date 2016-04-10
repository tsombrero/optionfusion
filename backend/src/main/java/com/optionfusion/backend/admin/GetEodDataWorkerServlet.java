package com.optionfusion.backend.admin;

import com.google.gcloud.storage.Blob;
import com.googlecode.objectify.Key;
import com.opencsv.CSVIterator;
import com.opencsv.CSVReader;
import com.optionfusion.backend.models.Equity;
import com.optionfusion.backend.models.OptionChain;
import com.optionfusion.backend.models.StockQuote;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.backend.utils.Constants;
import com.optionfusion.backend.utils.GoogleApiUtils;
import com.optionfusion.backend.utils.TextUtils;
import com.optionfusion.backend.utils.Util;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
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
    public static final String PARAM_DATE_TO_SEARCH = "DATE_TO_SEARCH";
    public static final String PARAM_INITIAL_LETTER_SHARD = "INITIAL_LETTER_SHARD";

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // The compute instance tries to shut itself down after downloading the data files into storage
        // but sometimes it fails and sits there running. That's bad because the download script only
        // runs on startup. The GetEodDataWorkerServlets run delayed so we can call shutdown here just to
        // be safe.
        stopComputeInstance();

        String initialLetter = req.getParameter(PARAM_INITIAL_LETTER_SHARD);
        DateTime dateToSearch = null;
        try {
            dateToSearch = new DateTime(Long.valueOf(req.getParameter(PARAM_DATE_TO_SEARCH)));
        } catch (Exception e) {
            log("Failed", e);
            return;
        }

        log("Job Starting " + initialLetter + " " + dateToSearch);

        try {
            processFilesForDate(dateToSearch, initialLetter);
        } catch (Throwable e) {
            resp.getWriter().println("Failed");
            e.printStackTrace();
            throw e;
        }
        log("Job Finished");
    }

    private void stopComputeInstance() {
        try {
            GoogleApiUtils.getCompute().instances().stop(Constants.APPLICATION_NAME, Constants.EOD_DOWNLOADER_INSTANCE_ZONE, Constants.EOD_DOWNLOADER_INSTANCE).execute();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void processFilesForDate(DateTime dateTime, String initialLetter) throws IOException {
        InputStream optionsIn = null;
        InputStream stocksIn = null;
        CSVIterator optionRecordIterator = null;
        CSVIterator stockQuoteRecordIterator = null;

        try {
            optionsIn = getRemoteFileInputStream(Util.getOptionsFileName(dateTime));
            stocksIn = getRemoteFileInputStream(Util.getStockQuotesFileName(dateTime));

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

            {
                String symbol = null;
                while (optionRecordIterator.hasNext()) {
                    firstOptionRecordForNextSymbol = optionRecordIterator.next();
                    symbol = firstOptionRecordForNextSymbol[OptionsColumns.UNDERLYING_SYMBOL.ordinal()];
                    if (symbol.startsWith(initialLetter) && isRecordValid(firstOptionRecordForNextSymbol)) {
                        break;
                    }
                }

                String[] record;
                while (stockQuoteRecordIterator.hasNext()) {
                    record = stockQuoteRecordIterator.next();
                    symbol = record[StockQuoteColumns.SYMBOL.ordinal()];

                    if (symbol.startsWith(initialLetter)) {
                        currentStockQuote = createStockQuote(record);
                        break;
                    }
                }

                log("Starting " + initialLetter + ":" + dateTime + " shard (" + symbol + "/" + currentStockQuote + ")");
            }

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

                if (currentStockQuote != null) {
                    stockQuoteSymbol = currentStockQuote.getSymbol();
                    if (!stockQuoteSymbol.startsWith(initialLetter))
                        currentStockQuote = null;
                }

                if (currentOptionChainBuilder != null) {
                    optionChainSymbol = currentOptionChainBuilder.getSymbol();
                    if (!optionChainSymbol.startsWith(initialLetter))
                        currentOptionChainBuilder = null;
                }

                if (currentOptionChainBuilder == null && currentStockQuote == null) {
                    log("Finished " + initialLetter + ":" + dateTime + " shard (" + stockQuoteSymbol + "/" + optionChainSymbol + ")");
                    break;
                }

                int compare = TextUtils.compare(optionChainSymbol, stockQuoteSymbol);

                if (compare == 0) {
                    commit(currentOptionChainBuilder, currentStockQuote);
                    currentOptionChainBuilder = null;
                    currentStockQuote = null;
                } else if (compare > 0 && currentStockQuote != null) {
                    //stockQuoteSymbol is first
                    commit(currentStockQuote);
                    currentStockQuote = null;
                } else if (currentOptionChainBuilder != null) {
                    //optionChainSymbol is first
                    commit(currentOptionChainBuilder);
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

        return createStockQuote(stockQuoteRecords.next());
    }

    private StockQuote createStockQuote(String[] record) {
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

    private long longValueOf(String[] record, Enum<?> col) {
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
        String symbol;
        long timestamp;
        double underlyingPrice;

        public String[] addRecords(String[] firstRecord, CSVIterator parser) {
            addRecord(firstRecord);
            while (parser.hasNext()) {
                String[] record = parser.next();
                if (!isRecordValid(record))
                    continue;

                if (symbol == null || TextUtils.equals(symbol, record[OptionsColumns.UNDERLYING_SYMBOL.ordinal()])) {
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

            if (symbol == null) {
                symbol = record[OptionsColumns.UNDERLYING_SYMBOL.ordinal()];
                timestamp = TIMESTAMP_FORMATTER.parseMillis(record[OptionsColumns.DATA_DATE.ordinal()] + " -0500");
                timestamp = Util.getEodDateTime(new DateTime(timestamp)).getMillis();
                underlyingPrice = doubleValueOf(record, OptionsColumns.UNDERLYING_PRICE);
            }

            if ("0".equals(record[OptionsColumns.OPEN_INTEREST.ordinal()]) && "0".equals(record[OptionsColumns.VOLUME.ordinal()]))
                return;

            if (doubleValueOf(record, OptionsColumns.ASK) < 0.05D && doubleValueOf(record, OptionsColumns.BID) < 0.05D)
                return;

            String exp = record[OptionsColumns.EXPIRATION.ordinal()];

            if (!subchainsByExp.containsKey(exp)) {
                subchainsByExp.put(exp, new ArrayList<String[]>());
            }
            subchainsByExp.get(exp).add(record);
        }

        OptionChainProto.OptionChain build() {  //TODO update last close price on skinny stockquote
            OptionChainProto.OptionChain.Builder ret = OptionChainProto.OptionChain.newBuilder()
                    .setSymbol(symbol)
                    .setTimestamp(timestamp)
                    .setUnderlyingPrice(underlyingPrice);

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
                            .setAsk(doubleValueOf(record, OptionsColumns.ASK))
                            .setBid(doubleValueOf(record, OptionsColumns.BID))
                            .setOptionType(record[OptionsColumns.TYPE.ordinal()].toUpperCase().startsWith("C") ? CALL : PUT)
                            .setStrike(doubleValueOf(record, OptionsColumns.STRIKE))
                            .setOpenInterest((int) longValueOf(record, OptionsColumns.OPEN_INTEREST))
                            .setLast(doubleValueOf(record, OptionsColumns.LAST))
                            .setVolume(longValueOf(record, OptionsColumns.VOLUME));

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
            return symbol;
        }
    }

    private boolean isRecordValid(String[] record) {
        String exchange = record[OptionsColumns.EXCHANGE.ordinal()];

        return ("*".equals(exchange)
                || "W".equals(exchange)
                || "Q".equals(exchange));
    }

    private void commit(OptionChainBuilder chainBuilder, StockQuote stockQuote) {
        //TODO transaction
        commit(chainBuilder.build());
        commit(stockQuote);
    }

    private void commit(OptionChainBuilder optionChainBuilder) {
        commit(optionChainBuilder.build());

        StockQuote stockQuote = new StockQuote(optionChainBuilder.getSymbol(), optionChainBuilder.timestamp);
        stockQuote.setClose(optionChainBuilder.underlyingPrice);
        commit(stockQuote);
    }

    private void commit(OptionChainProto.OptionChain currentChain) {
        OptionChain existingOptionChain = ofy().cache(false).load()
                .key(Util.getOptionChainKey(currentChain.getSymbol(), currentChain.getTimestamp()))
                .now();

        if (existingOptionChain == null) {
            ofy()
                    .cache(false)
                    .save().entity(new OptionChain(currentChain)).now();
        }
    }

    private void commit(StockQuote stockQuote) {
        Equity equity = getEquity(stockQuote.getSymbol());

        if (equity == null) {
            equity = new Equity(stockQuote.getSymbol(), "No Description", new ArrayList<String>());
        }

        if (equity.getEodStockQuote() == null || equity.getEodStockQuote().getDataTimestamp() < stockQuote.getDataTimestamp()) {
            if (equity.getEodStockQuote() != null) {
                stockQuote.setPreviousClose(equity.getEodStockQuote().getClose());
            }
            equity.setEodStockQuote(stockQuote);
            ofy()
                    .cache(false)
                    .save().entity(equity).now();
        }

        StockQuote existingStockQuote = ofy()
                .cache(false)
                .load()
                .key(Util.getStockQuoteKey(stockQuote.getSymbol(), stockQuote.getDataTimestamp()))
                .now();

        if (existingStockQuote == null) {
            ofy()
                    .cache(false)
                    .save().entity(stockQuote).now();
        }
    }

    private InputStream getRemoteFileInputStream(String fileName) throws IOException {
        Blob blob = Util.getBlobFromStorage(fileName);

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

    private Equity getEquity(String ticker) {
        return ofy()
                .cache(false)
                .load()
                .key(Key.create(Equity.class, ticker)).now();
    }


}
