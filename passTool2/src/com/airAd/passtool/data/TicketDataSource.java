package com.airAd.passtool.data;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.R;
import com.airAd.passtool.data.model.BarCode;
import com.airAd.passtool.data.model.Coupon;
import com.airAd.passtool.data.model.Field;
import com.airAd.passtool.data.model.NotifactionCondition;
import com.airAd.passtool.data.model.PushMessage;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.data.model.TicketIdentifier;
import com.airAd.passtool.service.TicketConfirmService;
import com.airAd.passtool.service.TicketDeleteService;
import com.airAd.passtool.service.TicketService;
import com.airAd.passtool.util.ConfigUtil;
import com.airAd.passtool.util.FileUtil;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.worker.NetWorker;
import com.google.zxing.client.android.encode.QREncoder;

/**
 * 票卷数据源
 * 
 * @author pengfan
 * 
 */
public class TicketDataSource {

	private Context context;
	private MySQLiteOpenHelper dbHelper;
	private SQLiteDatabase database;
	private static TicketDataSource thisOne;

	// 对应数据库各个字段的名字
	public static final String PASS_TYPE_IDENTIFIER = "passTypeIdentifier";
	public static final String SERIAL_NUMBER = "serialNumber";
	public static final String CHANGE_MESSAGE = "changeMessage";
	public static final String NOTIFY_FLAG = "notifyFlag";
	public static final String LOGO_TEXT = "logoText";
	public static final String FOLDER_NAME = "folderName";
	public static final String ALL = "allMessage";
	public static final String STATUS = "status";

	private static final String AUTHENTICATION_TOKEN = "authenticationToken";
	private static final String TYPE = "type";
	private static final String UPDATE_URL = "webServiceURL";
	private static final String LAST_UPDATE_DATE = "lastUpdateTime";
	private static final String LOCATIONS = "locations";
	private static final String ORGANIZATION_NAME = "organizationName";
	private static final String RELEVANT_DATE = "relevantDate";
	private static final String AUTO_UPDATE = "autoUpdate";
	private static final String BAR_CODE = "barcode";

	public static final String TABLE_NAME = "tickets";
	public static final String NOTIFICATION_TABLE_NAME = "notification";

	public static final int UPDATE_SUCCESS = 2;
	public static final int INSERT_SUCCESS = 1;
	public static final int PERSIST_FAIL = -1;
	public static final int FORMAT_FAIL = -2;// 数据格式异常
	public static final int REGISTER_FAIL = -3;// 注册异常

	public static final int AUTO_UPDATE_ENABLED = 1;
	public static final int AUTO_UPDATE_DISABLED = 0;

	public static final int STATUS_UNREGISTER = 0;
	public static final int STATUS_REGISTERED = 1;

	private static final String[] queryTicketCOLUMNS = new String[] { TYPE,
			ALL, FOLDER_NAME, SERIAL_NUMBER, PASS_TYPE_IDENTIFIER,
			AUTHENTICATION_TOKEN, UPDATE_URL, LAST_UPDATE_DATE, AUTO_UPDATE };

	private static final String[] queryTicketIdentifierCOLUMNS = new String[] {
			SERIAL_NUMBER, PASS_TYPE_IDENTIFIER, AUTHENTICATION_TOKEN,
			UPDATE_URL, LAST_UPDATE_DATE, FOLDER_NAME };
	private static final String[] queryTicketExisted = new String[] {
			SERIAL_NUMBER, FOLDER_NAME };
	private static final String[] queryNoficationCOLUMNS = new String[] {
			SERIAL_NUMBER, LOCATIONS, RELEVANT_DATE, ORGANIZATION_NAME,
			FOLDER_NAME };
	private static final String querySerialNumberParam = SERIAL_NUMBER + " = ?";
	private static final String queryStatus = STATUS + " = ?";

	public TicketDataSource(Context context) {
		this.context = context;
		dbHelper = new MySQLiteOpenHelper(context);
	}

	public static TicketDataSource getInstance(Context context) {
		if (thisOne == null) {
			thisOne = new TicketDataSource(context);
		}
		return thisOne;
	}

	public void open() throws SQLException {
		if (database != null && database.isOpen()) {
			dbHelper.close();
		}
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	// TODO:insert会出现异常(serial number 如果重复会插入失败)
	/**
	 * 增加或者更新，结果通知
	 * 
	 * @param needRegister
	 *            是否需要注册
	 */
	public int insertOrUpdate(String json_str, String folderName,
			boolean needRegister,Date lastModifyTime) {
		JSONObject obj = null;
		QREncoder encoder = new QREncoder(context.getResources()
				.getDimensionPixelSize(R.dimen.qrcode_size));

		try {
			obj = new JSONObject(json_str);
		} catch (JSONException e1) {
			LogUtil.w(TicketDataSource.class, json_str + " insert fail!");
			return FORMAT_FAIL;
		}
		try {
			if (obj != null) {
				Config config = MyApplication.getCurrentApp().getConfig();
				TicketIdentifier ticketIdentifier = new TicketIdentifier();
				if (!obj.isNull(SERIAL_NUMBER)) {
					String serialNumber = obj.getString(SERIAL_NUMBER);
					// 用于注册请求
					ticketIdentifier.setSerialNumber(serialNumber);
					ContentValues values = new ContentValues();
					values.put(FOLDER_NAME, folderName);
					if (!obj.isNull(PASS_TYPE_IDENTIFIER)) {
						String passTypeIdentifier = obj
								.getString(PASS_TYPE_IDENTIFIER);
						values.put(PASS_TYPE_IDENTIFIER, passTypeIdentifier);
						// 缓存
						if (config.getPassTypeIdentifier() == null) {
							config.setPassTypeIdentifier(passTypeIdentifier);
						}
						ticketIdentifier
								.setPassTypeIdentifier(passTypeIdentifier);
					} else {
						values.putNull(PASS_TYPE_IDENTIFIER);
					}

					PushMessageManager pushMessageCreator = new PushMessageManager(
							ticketIdentifier.getPassTypeIdentifier(),
							ticketIdentifier.getSerialNumber(), database);
					// 如果校验参数失败返回失败代码
					if (!pushMessageCreator.validateAndGeneratePushMessage(obj)) {
						return REGISTER_FAIL;
					}
					if (!obj.isNull(AUTHENTICATION_TOKEN)) {
						String authenticationToken = obj
								.getString(AUTHENTICATION_TOKEN);
						values.put(AUTHENTICATION_TOKEN, authenticationToken);
						ticketIdentifier
								.setAuthenticationToken(authenticationToken);
					} else {
						values.putNull(AUTHENTICATION_TOKEN);
					}
					if (!obj.isNull(UPDATE_URL)) {
						String updateUrl = obj.getString(UPDATE_URL);
						values.put(UPDATE_URL, updateUrl);
						ticketIdentifier.setWebServiceUrl(updateUrl);
					} else {
						values.putNull(UPDATE_URL);
					}
					if (!obj.isNull(BAR_CODE)) {
						JSONObject barcode_json = obj.getJSONObject(BAR_CODE);
						encoder.encode(folderName,
								generateBarCode(barcode_json));
					} else {
						QREncoder.deleteQRCodeFile(folderName);
					}
//					config.addWebServiceHost(ticketIdentifier
//							.getWebServiceUrl());
					// 默认是优惠劵类型
					if (!obj.isNull(TYPE)) {
						values.put(TYPE, obj.getLong(TYPE));
					} else {
						values.put(TYPE, Ticket.TYPE_COUPON);
					}
					values.put(ALL, json_str);
					if (!obj.isNull(LOCATIONS)) {
						values.put(LOCATIONS, obj.getJSONArray(LOCATIONS)
								.toString());
					} else {
						values.putNull(LOCATIONS);
					}
					if (!obj.isNull(ORGANIZATION_NAME)) {
						values.put(ORGANIZATION_NAME,
								obj.getString(ORGANIZATION_NAME));
					} else {
						values.putNull(ORGANIZATION_NAME);
					}
					if (!obj.isNull(RELEVANT_DATE)) {
						values.put(RELEVANT_DATE, obj.getString(RELEVANT_DATE));
					} else {
						values.putNull(RELEVANT_DATE);
					}
					
					//改为从服务器端获取时间
//					values.put(LAST_UPDATE_DATE, (new Date()).getTime());
					values.put(LAST_UPDATE_DATE, lastModifyTime.getTime());
					// 如果已存在则进行更新
					TicketIdentifier ti = isExisted(serialNumber);
					if (ti != null && ti.getFolderName() != null) {
						FileUtil.delete(TicketService.parentFolderPath
								+ ti.getFolderName());
						database.update(TABLE_NAME, values,
								querySerialNumberParam,
								new String[] { serialNumber });
						notifyService();
						return UPDATE_SUCCESS;
					} else {
						values.put(SERIAL_NUMBER, serialNumber);
						values.put(STATUS, STATUS_UNREGISTER);
						database.insert(TABLE_NAME, null, values);
						cleanPassDirectory();
						notifyService();
						// 异步发送注册请求
						TicketConfirmService service = new TicketConfirmService(
								context);
						service.setTicketIdentifier(ticketIdentifier);
						new NetWorker(context).request(service, null);
						return INSERT_SUCCESS;
					}
				}
			}
		} catch (JSONException e) {
			LogUtil.w(TicketDataSource.class, e.getMessage());
			e.printStackTrace();
		}
		FileUtil.delete(TicketService.parentFolderPath + folderName);
		return FORMAT_FAIL;
	}

	public List<PushMessage> queryPushMessages() {
		return new PushMessageManager(database).queryPushMessages();
	}

	/**
	 * 查询相同的是否有相同的节点。
	 * 
	 * @param serialNumber
	 * @return 该节点的存储路径
	 */
	public TicketIdentifier isExisted(String serialNumber) {
		Cursor cursor = database.query(TABLE_NAME,
				queryTicketIdentifierCOLUMNS, querySerialNumberParam,
				new String[] { serialNumber }, null, null, null);
		cursor.moveToFirst();
		TicketIdentifier res = null;
		if (!cursor.isAfterLast()) {
			res = generateTicketIdentifier(cursor);
		}
		cursor.close();
		return res;
	}

	/**
	 * 更新最后更新时间
	 * 
	 * @param serialNumber
	 */
	public void updateLastTime(String serialNumber) {
		ContentValues values = new ContentValues();
		values.put(LAST_UPDATE_DATE, (new Date()).getTime());
		database.update(TABLE_NAME, values, querySerialNumberParam,
				new String[] { serialNumber });
	}

	/**
	 * 更新是否自动更新标志
	 * 
	 * @param serialNumber
	 */
	public void setAutoUpdate(String serialNumber, boolean autoUpdate) {
		ContentValues values = new ContentValues();
		values.put(AUTO_UPDATE, ConfigUtil.parseEnabledAutoUpdate(autoUpdate));
		database.update(TABLE_NAME, values, querySerialNumberParam,
				new String[] { serialNumber });
	}

	/**
	 * 删除一条记录
	 * 
	 * @param serialNumber
	 * @return
	 */
	public boolean delete(String serialNumber) {
		TicketIdentifier ti = isExisted(serialNumber);
		if (ti != null) {
			int num = database.delete(TABLE_NAME, querySerialNumberParam,
					new String[] { serialNumber });
			close();
			if (num > 0) {
				FileUtil.delete(TicketService.parentFolderPath
						+ ti.getFolderName());
				NetWorker netWorker = new NetWorker(context);
				TicketDeleteService service = new TicketDeleteService(context);
				service.setTicketIdentifier(ti);
				netWorker.request(service, null);
				notifyService();
				return true;
			}
		}
		return false;

	}

	/**
	 * 返回Ticket票据
	 * 
	 * @param serialNumbers
	 *            可以为null，如果为null的情况，则查询全部
	 * @return 如果为null 则表示数据结构不对。
	 */
	public List<Ticket> query(List<String> serialNumbers) {
		List<Ticket> result = new ArrayList<Ticket>();
		Cursor cursor = null;
		if (serialNumbers != null && !serialNumbers.isEmpty()) {
			StringBuffer params = new StringBuffer();
			params.append(SERIAL_NUMBER).append(" IN (");
			for (int i = 0; i < serialNumbers.size(); i++) {
				if (i != 0) {
					params.append(",");
				}
				params.append("?");
			}
			params.append(")");
			String[] paramArray = new String[serialNumbers.size()];
			cursor = database.query(TABLE_NAME, queryTicketCOLUMNS,
					params.toString(), serialNumbers.toArray(paramArray), null,
					null, null);
		} else {
			cursor = database.query(TABLE_NAME, queryTicketCOLUMNS, null, null,
					null, null, LAST_UPDATE_DATE + " desc");
		}
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Ticket ticket = generateTicket(cursor);
			if (ticket != null) {
				result.add(ticket);
			}
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}

	/**
	 * 查找所有包含提醒的请求
	 * 
	 * @return
	 */
	public List<NotifactionCondition> queryNotifactionConditions() {
		List<NotifactionCondition> result = new ArrayList<NotifactionCondition>();
		Cursor cursor = database.query(TABLE_NAME, queryNoficationCOLUMNS,
				null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			NotifactionCondition nc = generateNotifactionCondition(cursor);
			result.add(nc);
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}

	/**
	 * 仅仅自动更新是开启状态的标志位（用于更新数据）
	 * 
	 * @return
	 */
	public List<TicketIdentifier> queryUnRegisterTicketIdentifiers() {
		List<TicketIdentifier> result = new ArrayList<TicketIdentifier>();
		Cursor cursor = null;
		cursor = database.query(TABLE_NAME, queryTicketIdentifierCOLUMNS,
				queryStatus, new String[] { STATUS_UNREGISTER + "" }, null,
				null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			TicketIdentifier ti = generateTicketIdentifier(cursor);
			if (ti != null) {
				result.add(ti);
			}
			cursor.moveToNext();
		}
		cursor.close();
		return result;
	}

	/**
	 * 更新注册状态
	 * 
	 * @param serialNumber
	 */
	public void updateRegisterStatus(String serialNumber) {
		ContentValues values = new ContentValues();
		values.put(STATUS, STATUS_REGISTERED);
		database.update(TABLE_NAME, values, querySerialNumberParam,
				new String[] { serialNumber });
	}

	/**
	 * 仅仅自动更新是开启状态的标志位（用于更新数据）
	 * 
	 * @return
	 */
	public List<TicketIdentifier> queryTicketIdentifiers(
			List<String> serialNumbers) {
		List<TicketIdentifier> result = new ArrayList<TicketIdentifier>();
		Cursor cursor = null;
		if (serialNumbers != null && !serialNumbers.isEmpty()) {
			StringBuffer params = new StringBuffer();
			params.append(SERIAL_NUMBER).append(" IN (");
			for (int i = 0; i < serialNumbers.size(); i++) {
				if (i != 0) {
					params.append(",");
				}
				params.append("?");
			}
			params.append(")");
			params.append(" and ").append(AUTO_UPDATE).append(" = ?");
			String[] paramArray = new String[serialNumbers.size() + 1];
			paramArray = serialNumbers.toArray(paramArray);
			paramArray[serialNumbers.size()] = TicketDataSource.AUTO_UPDATE_ENABLED
					+ "";
			cursor = database.query(TABLE_NAME, queryTicketIdentifierCOLUMNS,
					params.toString(), paramArray, null, null, null);
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				TicketIdentifier ti = generateTicketIdentifier(cursor);
				if (ti != null) {
					result.add(ti);
				}
				cursor.moveToNext();
			}
			cursor.close();
		}
		return result;
	}

	/**
	 * 通知service重启
	 */
	private void notifyService() {
		Intent intent = new Intent();
		intent.setAction(ConfigUtil.REFRESH_SERVICE_ACTION_NAME);
		context.sendBroadcast(intent);
	}

	/**
	 * 生成二维码
	 * 
	 * @param obj
	 * @return
	 */
	private BarCode generateBarCode(JSONObject obj) {
		BarCode res = new BarCode();
		res.setContent(getString(obj, "message"));
		res.setEncoding(getString(obj, "messageEncoding"));
		res.setAltText(getString(obj, "altText"));
		return res;
	}

	/**
	 * 生成单个ticket标志
	 * 
	 * @param cursor
	 * @return
	 */
	private TicketIdentifier generateTicketIdentifier(Cursor cursor) {
		TicketIdentifier ti = new TicketIdentifier();
		ti.setSerialNumber(cursor.getString(0));
		ti.setPassTypeIdentifier(cursor.getString(1));
		ti.setAuthenticationToken(cursor.getString(2));
		ti.setWebServiceUrl(cursor.getString(3));
		ti.setLastUpdateTime(new Date(cursor.getLong(4)));
		ti.setFolderName(cursor.getString(5));
		return ti;
	}

	/**
	 * 生成单个提醒
	 * 
	 * @param cursor
	 * @return
	 */
	private NotifactionCondition generateNotifactionCondition(Cursor cursor) {
		NotifactionCondition nc = new NotifactionCondition();
		nc.setSerialNumber(cursor.getString(0));
		nc.setLocations(cursor.getString(1));
		nc.setDate(cursor.getString(2));
		nc.setOrganizationName(cursor.getString(3));
		nc.setFolderName(cursor.getString(4));
		return nc;
	}

	/**
	 * 生成票卷
	 * 
	 * @param cursor
	 * @return
	 * 
	 *         { TYPE, ALL, FOLDER_NAME, SERIAL_NUMBER, PASS_TYPE_IDENTIFIER,
	 *         AUTHENTICATION_TOKEN, UPDATE_URL, LAST_UPDATE_DATE, AUTO_UPDATE
	 *         };
	 */
	private Ticket generateTicket(Cursor cursor) {
		int type = cursor.getInt(0);
		String folderName = cursor.getString(2);
		String json_str = cursor.getString(1);
		JSONObject json_obj = null;
		try {
			json_obj = new JSONObject(json_str);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		Ticket ticket = null;
		switch (type) {
		case Ticket.TYPE_COUPON: {
			Coupon eventTicket = new Coupon();
			JSONObject eventTicket_obj = null;

			if (!json_obj.isNull("coupon")) {
				try {
					eventTicket_obj = json_obj.getJSONObject("coupon");
				} catch (JSONException e) {
					e.printStackTrace();
					break;
				}
				if (!eventTicket_obj.isNull("primaryFields")) {
					eventTicket.setPrimaryField(getFeildList(eventTicket_obj,
							"primaryFields"));
				}
			} else if (!json_obj.isNull("storeCard")) {
				try {
					eventTicket_obj = json_obj.getJSONObject("storeCard");
				} catch (JSONException e) {
					e.printStackTrace();
					break;
				}
				if (!eventTicket_obj.isNull("headerFields")) {
					eventTicket.setHeadFields(getFeildList(eventTicket_obj,
							"headerFields"));
				}
			}

			if (!eventTicket_obj.isNull("backFields")) {
				eventTicket.setBackFields(getFeildList(eventTicket_obj,
						"backFields"));
			}
			if (!eventTicket_obj.isNull("secondaryFields")) {
				eventTicket.setSecondaryFields(getFeildList(eventTicket_obj,
						"secondaryFields"));
			}
			if (!eventTicket_obj.isNull("auxiliaryFields")) {
				eventTicket.setAuxiliaryFields(getFeildList(eventTicket_obj,
						"auxiliaryFields"));
			}
//			ArrayList<Field>l=new ArrayList<Field>();
//			eventTicket.setHeadFields(l);
//			eventTicket.getHeadFields().add(new Field("￥10.00","价格"));
			ticket = eventTicket;
			break;
		}
		}
		/**
		 * 数据不符合格式
		 */
		if (ticket == null) {
			return null;
		}

		try {
			if (!json_obj.isNull(BAR_CODE)) {
				ticket.setBarcode(generateBarCode(json_obj
						.getJSONObject(BAR_CODE)));
			}
		} catch (JSONException e) {
		}

		/**
		 * { TYPE, ALL, FOLDER_NAME, SERIAL_NUMBER, PASS_TYPE_IDENTIFIER,
		 * AUTHENTICATION_TOKEN, UPDATE_URL, LAST_UPDATE_DATE, AUTO_UPDATE };
		 */
		ticket.getTicketIdentifier().setSerialNumber(cursor.getString(3));
		ticket.getTicketIdentifier().setPassTypeIdentifier(cursor.getString(4));
		ticket.getTicketIdentifier()
				.setAuthenticationToken(cursor.getString(5));
		ticket.getTicketIdentifier().setWebServiceUrl(cursor.getString(6));
		ticket.getTicketIdentifier().setLastUpdateTime(
				new Date(cursor.getLong(7)));
		ticket.getTicketIdentifier().setAutoUpdate(cursor.getInt(8));
		ticket.setFolderName(folderName);
		if (!json_obj.isNull("organizationName")) {
			ticket.setOrganizationName(getString(json_obj, "organizationName"));
		}
		if (!json_obj.isNull("backgroundColor")) {
			ticket.setBackgroundColor(getColor(getString(json_obj,
					"backgroundColor")));
		}
		if (!json_obj.isNull("labelColor")) {
			ticket.setLabelColor(getColor(getString(json_obj, "labelColor")));
		}
		if (!json_obj.isNull("foregroundColor")) {
			ticket.setForegroundColor(getColor(getString(json_obj,
					"foregroundColor")));
		}
		if (!json_obj.isNull("logoText")) {
			ticket.setLogoText(getString(json_obj, "logoText"));
		}
		return ticket;
	}

	public void clear() {
		dbHelper.clear();
	}

	/**
	 * 清除缓存中无效的目录和文件
	 */
	public void cleanPassDirectory() {
		Cursor cursor = null;
		List<String> folderNameList = new ArrayList<String>();
		cursor = database.query(TABLE_NAME, new String[] { FOLDER_NAME }, null,
				null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			folderNameList.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();
		File directory = new File(TicketService.parentFolderPath);
		if (directory.exists()) {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()
						&& !folderNameList.contains(file.getName())) {
					FileUtil.deleteDirectory(file);
				} else if (file.isFile()) {
					file.delete();
				}
			}
		}

	}

	private static String getString(JSONObject obj, String name) {
		String res = null;
		try {
			res = obj.getString(name);
		} catch (JSONException e) {
			LogUtil.w(TicketDataSource.class, e.getMessage());
		}
		return res;
	}

	private static int getColor(String str) {
		return ConfigUtil.parseColor(str);

	}

	private static List<Field> getFeildList(JSONObject parObj, String name) {
		List<Field> list = new ArrayList<Field>();
		JSONArray array;
		try {
			array = parObj.getJSONArray(name);
			for (int i = 0; i < array.length(); i++) {
				JSONObject obj = array.optJSONObject(i);
				if (obj != null) {
					Field field = new Field(getString(obj, "label"), getString(
							obj, "value"));
					list.add(field);
				}
			}
		} catch (JSONException e) {
			LogUtil.w(TicketDataSource.class, e.getMessage());
		}
		return list;
	}
}
