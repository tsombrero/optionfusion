package com.optionfusion.gcutils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LookupEntryParser {
    private File csvFile;

    LookupEntryParser(File csvFile) {
        this.csvFile = csvFile;
    }

    enum Columns {
        Ticker, Name, Sector, Industry, Price, Collection
    }

    public void parse() {
//        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
//        Transaction txn = datastore.beginTransaction();

        long duration = System.currentTimeMillis();

        try {
            Reader in = new FileReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
            for (CSVRecord record : records) {
                String[] keywords = getKeywords(record.get(Columns.Name));
                if (keywords.length == 0)
                    System.out.println(record.get(Columns.Ticker) + " : '" + record.get(Columns.Name) + "' " + Arrays.toString(keywords));
//                addRecord(datastore, record);
            }
//            txn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Caught error" + e);
        } finally {
//            if (txn.isActive()) {
//                txn.rollback();
//            }
        }
        duration = System.currentTimeMillis() - duration;
        System.out.println("Duration: " + duration);
    }

    private void addRecord(DatastoreService dataStore, CSVRecord record) {
        Key symbolKey = KeyFactory.createKey("Lookup", record.get(Columns.Ticker));
        Entity symbolLookup = new Entity(symbolKey);
        symbolLookup.setProperty("keywords", getKeywords(record.get(Columns.Name)));
        symbolLookup.setProperty("name", record.get(Columns.Name));

//            datastore.put(symbolLookup);
    }

    private static final List<String> reject = Arrays.asList("", "inc", "corp", "co", "corporation", "ltd");

    private String[] getKeywords(String description) {
        ArrayList<String> ret = new ArrayList(Arrays.asList(
                description
                        .toLowerCase()
                        .replaceAll("[^A-Za-z0-9]", " ")
                        .split(" +")));

        ret.removeAll(reject);
        return ret.toArray(new String[ret.size()]);
    }
}
