package com.optionfusion.db;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.sqlite.database.DatabaseErrorHandler;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteOpenHelper;

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
        createTable(db, Schema.Favorites.values());
        createTable(db, Schema.StockQuotes.values());

//        createUniqueIndex(db, Schema.VerticalSpreads.BUY_SYMBOL, Schema.VerticalSpreads.SELL_SYMBOL);
        createUniqueIndex(db, Schema.Options.OPTION_TYPE, Schema.Options.UNDERLYING_SYMBOL, Schema.Options.SYMBOL, Schema.Options.EXPIRATION);
        createUniqueIndex(db, Schema.Favorites.BUY_SYMBOL, Schema.Favorites.SELL_SYMBOL);

        execSql(db, Schema.vw_Favorites.values()[0].getViewSql());
    }

    private void createUniqueIndex(SQLiteDatabase db, Schema.DbColumn... columns) {
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
        dropAll(db);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropAll(db);
        onCreate(db);
    }

    private void dropAll(SQLiteDatabase db) {
        dropTable(db, Schema.Options.TABLE_NAME);
        dropTable(db, Schema.VerticalSpreads.TABLE_NAME);
        dropTable(db, Schema.StockQuotes.TABLE_NAME);
        dropTable(db, Schema.Favorites.TABLE_NAME);
        dropView(db, Schema.vw_Favorites.VIEW_NAME);
    }

    private void dropTable(SQLiteDatabase db, String col) {
        execSql(db, "DROP TABLE IF EXISTS " + col);
    }

    private void dropView(SQLiteDatabase db, String col) {
        execSql(db, "DROP VIEW IF EXISTS " + col);
    }


}
