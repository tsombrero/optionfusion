package com.optionfusion.gcutils;

import com.google.appengine.api.datastore.*;
import com.google.appengine.repackaged.com.google.api.client.util.store.DataStore;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;

public class LookupEntryParser {
    private File csvFile;

    LookupEntryParser(File csvFile) {
        this.csvFile = csvFile;
    }

    enum Columns {
        Ticker, Name, Sector, Industry, Price, Collection
    }

    public void parse() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();

        long duration = System.currentTimeMillis();

        try {
            Reader in = new FileReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withHeader().parse(in);
            for (CSVRecord record : records) {
                System.out.println(record.get(Columns.Ticker) + " : " + record.get(Columns.Name) + " " + Arrays.toString(getKeywords(record.get(Columns.Name))));
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

    private String[] getKeywords(String description) {
        return description
                .toLowerCase()
                .replaceAll("[^A-Za-z0-9]", " ")
                .replaceAll(" inc\\b", "")
                .replaceAll(" corp\\b", "")
                .replaceAll(" corporation\\b","")
                .replaceAll(" ltd\\b", "")
                .replaceAll(" co\\b", "")
                .split(" +");
    }
}
