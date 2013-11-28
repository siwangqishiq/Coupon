package com.airAd.passtool.provider;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.airAd.passtool.data.MySQLiteOpenHelper;
import com.airAd.passtool.data.TicketDataSource;

public class TicketHeaderProvider extends ContentProvider {

    public static final String AUTHORITY = "com.airAd.passtool.provider.TicketHeaderProvider";

    private MySQLiteOpenHelper dbHelper;

    private static HashMap<String, String> sTicketHeadersProjectionMap;
    private static final UriMatcher sUriMatcher;
    private static final int HEADERS = 1;

    static {

        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(AUTHORITY, "ticket_heads", HEADERS);

        sTicketHeadersProjectionMap = new HashMap<String, String>();
        sTicketHeadersProjectionMap.put(TicketDataSource.PASS_TYPE_IDENTIFIER, TicketDataSource.PASS_TYPE_IDENTIFIER);
        sTicketHeadersProjectionMap.put(TicketDataSource.SERIAL_NUMBER, TicketDataSource.SERIAL_NUMBER);
        sTicketHeadersProjectionMap.put(TicketDataSource.FOLDER_NAME, TicketDataSource.FOLDER_NAME);
        sTicketHeadersProjectionMap.put(TicketDataSource.ALL, TicketDataSource.ALL);

    }

    @Override
    public boolean onCreate() {
        dbHelper = new MySQLiteOpenHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(TicketDataSource.TABLE_NAME);
        switch (sUriMatcher.match(uri)) {
            case HEADERS :
                qb.setProjectionMap(sTicketHeadersProjectionMap);
                break;
            default :
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }
        Cursor c = qb.query(db, projection, null, null, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}
