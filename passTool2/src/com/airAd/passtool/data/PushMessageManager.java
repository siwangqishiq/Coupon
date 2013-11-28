package com.airAd.passtool.data;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.airAd.passtool.data.model.PushMessage;

/**
 * 通知消息管理器
 * 
 * @author pengfan
 * 
 */
public class PushMessageManager {

	private String passTypeIdentifier;
	private String serialNumber;
	private SQLiteDatabase database;
	private static final String deleteParams = TicketDataSource.PASS_TYPE_IDENTIFIER
			+ " = ? & " + TicketDataSource.SERIAL_NUMBER + " = ?";
	private static final String[] queryNotificationCOLUMNS = new String[] {
			TicketDataSource.PASS_TYPE_IDENTIFIER,
			TicketDataSource.SERIAL_NUMBER, TicketDataSource.CHANGE_MESSAGE,
			TicketDataSource.LOGO_TEXT, TicketDataSource.NOTIFY_FLAG };

	public PushMessageManager(SQLiteDatabase database) {
		this.database = database;
	}

	public PushMessageManager(String passTypeIdentifier, String serialNumber,
			SQLiteDatabase database) {
		this(database);
		this.passTypeIdentifier = passTypeIdentifier;
		this.serialNumber = serialNumber;
	}

	/**
	 * 查找可以提醒的pushMessage，查找完后将所有
	 * 
	 * @return
	 */
	public List<PushMessage> queryPushMessages() {
		List<PushMessage> result = new ArrayList<PushMessage>();
		Cursor cursor = database.query(
				TicketDataSource.NOTIFICATION_TABLE_NAME,
				queryNotificationCOLUMNS, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			PushMessage msg = new PushMessage();
			msg.setPassTypeIdentifier(cursor.getString(0));
			msg.setSerialNumber(cursor.getString(1));
			msg.setChangeMessage(cursor.getString(2));
			msg.setLogoText(cursor.getString(3));
			result.add(msg);
			cursor.moveToNext();
		}
		cursor.close();
		database.delete(TicketDataSource.NOTIFICATION_TABLE_NAME, null, null);
		return result;
	}

	/**
	 * 验证和插入push的消息 
	 * 
	 * @param obj
	 * @return
	 * @throws JSONException
	 */
	public boolean validateAndGeneratePushMessage(JSONObject obj) {
		String tag = null;
		if (!obj.isNull("coupon"))
			tag = "coupon";
		else if (!obj.isNull("storeCard")) {
			tag = "storeCard";
		}
		/*
		 * else if (!obj.isNull("boardingPass")) tag = "boardingPass"; else if
		 * (!obj.isNull("eventTicket")) tag = "eventTicket"; else if
		 * (!obj.isNull("generic")) tag = "generic"; else if
		 * (!obj.isNull("storeCard")) tag = "storeCard";
		 */
		if (tag == null) {
			return false;
		} else {
			database.delete(TicketDataSource.NOTIFICATION_TABLE_NAME,
					deleteParams, new String[] { passTypeIdentifier,
							serialNumber });
			String logoText = obj.optString("logoText", "");
			JSONObject typeObj = obj.optJSONObject(tag);
			persistentPushMessageIn(typeObj, "headerFields", logoText);
			persistentPushMessageIn(typeObj, "primaryFields", logoText);
			persistentPushMessageIn(typeObj, "secondaryFields", logoText);
			persistentPushMessageIn(typeObj, "auxiliaryFields", logoText);
			persistentPushMessageIn(typeObj, "backFields", logoText);
			return true;
		}
	}

	public void deleteNotification() {
		database.delete(TicketDataSource.NOTIFICATION_TABLE_NAME, deleteParams,
				new String[] { passTypeIdentifier, serialNumber });
	}

	private void persistentPushMessageIn(JSONObject typeObj, String fieldName,
			String logoText) {
		if (!typeObj.isNull(fieldName)) {
			JSONArray fieldArray = typeObj.optJSONArray(fieldName);
			for (int i = 0; i < fieldArray.length(); i++) {
				JSONObject fieldObj = fieldArray.optJSONObject(i);
				if (fieldObj != null) {
					insertPushMessage(fieldObj, logoText);
				}
			}
		}
	}

	private void insertPushMessage(JSONObject fieldObj, String logoText) {
		String changeMessageVal = null;
		try {
			changeMessageVal = fieldObj.getString("changeMessage");
		} catch (JSONException e) {

		}
		String value = fieldObj.optString("value", "");
		if (changeMessageVal != null) {
			// 默认显示字段
			String changeMessage = "changed";
			if (changeMessageVal.contains("%@")) {
				changeMessage = changeMessageVal.replace("%@", value);
			}
			ContentValues values = new ContentValues();
			values.put(TicketDataSource.PASS_TYPE_IDENTIFIER,
					passTypeIdentifier);
			values.put(TicketDataSource.SERIAL_NUMBER, serialNumber);
			values.put(TicketDataSource.CHANGE_MESSAGE, changeMessage);
			values.put(TicketDataSource.LOGO_TEXT, logoText);
			database.insert(TicketDataSource.NOTIFICATION_TABLE_NAME, null,
					values);
		}

	}
}
