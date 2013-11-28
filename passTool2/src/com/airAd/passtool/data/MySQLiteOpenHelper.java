package com.airAd.passtool.data;

import java.util.Properties;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.airAd.passtool.R;
import com.airAd.passtool.service.TicketService;
import com.airAd.passtool.util.FileUtil;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "tickets.db";
	private static final int DATABASE_VERSION = 3;

	private Properties properties = new Properties();

	public MySQLiteOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		try {
			properties.loadFromXML(context.getResources().openRawResource(
					R.raw.db_init_sql));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(properties.getProperty("tickets.create"));
		db.execSQL(properties.getProperty("notification.create"));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(MySQLiteOpenHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		// 版本1升级到版本2:增加一个notification表
		if (oldVersion == 1) {
			db.execSQL(properties.getProperty("notification.create"));
		}
		db.execSQL(properties.getProperty("tickets.update.addstatus"));
		db.execSQL(properties.getProperty("tickets.update.addstatus.default"));
	}

	public void clear() {
		FileUtil.delete(TicketService.parentFolderPath);
		getWritableDatabase().execSQL(properties.getProperty("tickets.delete"));
		getWritableDatabase().execSQL(
				properties.getProperty("notification.delete"));
	}
}
