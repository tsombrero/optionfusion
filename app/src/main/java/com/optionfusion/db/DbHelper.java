package com.optionfusion.db;

import android.content.Context;

import org.sqlite.database.DatabaseErrorHandler;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DbHelper";

    {
        System.loadLibrary("sqliteX");
    }

    public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, Schema.Options.values());
        createTable(db, Schema.VerticalSpreads.values());

//        createUniqueIndex(db, Schema.VerticalSpreads.BUY_SYMBOL, Schema.VerticalSpreads.SELL_SYMBOL);
        createUniqueIndex(db, Schema.Options.OPTION_TYPE, Schema.Options.UNDERLYING_SYMBOL, Schema.Options.SYMBOL, Schema.Options.EXPIRATION);
    }

    private void createUniqueIndex(SQLiteDatabase db, Schema.DbColumn ... columns) {
        String indexName = "INDEX_" + TextUtils.join("_", Schema.getColumnNames(columns));
        String cmd = new StringBuilder("CREATE UNIQUE INDEX IF NOT EXISTS ")
                .append(indexName)
                .append(" ON ")
                .append(columns[0].getClass().getSimpleName())
                .append(" (")
                .append(TextUtils.join(",", Schema.getColumnNames(columns)))
                .append(") ")
                .toString();

        execSql(db, cmd);
    }

    private void createTable(SQLiteDatabase db, Schema.DbColumn[] columns) {
        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        sb.append(columns[0].getClass().getSimpleName());
        sb.append(" ( ");

        List<String> columndefs = new ArrayList<>();

        for (Schema.DbColumn col : columns) {
            StringBuffer sbcol = new StringBuffer(col.name())
                    .append(" ")
                    .append(col.dataType())
                    .append(" ")
                    .append(TextUtils.join(" ", Schema.getEnumNames(col.constraints())).replace("_", " "));
            columndefs.add(sbcol.toString());
        }

        sb.append(TextUtils.join(",", columndefs));

        sb.append(" ) ");

        execSql(db, sb.toString());
    }

    private void execSql(SQLiteDatabase db, String cmd) {
        Log.d(TAG, cmd);
        db.execSQL(cmd);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db, Schema.Options.ASK);
        dropTable(db, Schema.VerticalSpreads.BUFFER_TO_BREAK_EVEN);
        dropTable(db, Schema.StockQuotes.CHANGE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db, Schema.Options.ASK);
        dropTable(db, Schema.VerticalSpreads.BUFFER_TO_BREAK_EVEN);
        dropTable(db, Schema.StockQuotes.CHANGE);
        onCreate(db);
    }

    private void dropTable(SQLiteDatabase db, Schema.DbColumn col) {
        execSql(db, "DROP TABLE IF EXISTS " + col.getClass().getSimpleName());
    }


}
