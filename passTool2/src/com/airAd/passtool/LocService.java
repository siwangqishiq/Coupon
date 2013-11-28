package com.airAd.passtool;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.Item;
import com.airAd.passtool.data.model.Loc;
import com.airAd.passtool.data.model.NotifactionCondition;
import com.airAd.passtool.data.model.PushMessage;
import com.airAd.passtool.util.DateUtil;
import com.airAd.passtool.util.LocationUtil;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.MapUtils;
import com.airAd.passtool.util.Utils;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

public class LocService extends Service {
	boolean DEBUG = false;

	private LogUtil logUtil = LogUtil.getInstance(DEBUG, "LocService");
	private static final String START_SERVICE = "com.airAd.passtool.receiver.filter.alarm";
	private TicketDataSource dataSource;// 数据库操作类
	// private LocationClient mLocationClient;
	private List<Item> itemList;
	private List<NotifactionCondition> dataList;// 原始数据列表
	private List<PushMessage> mPushMsgList;
	// private static final int RADIUS = 50;
	private static final int RADIUS = 750;
	public static final String FILEROOTPATH = android.os.Environment
			.getExternalStorageDirectory().getAbsolutePath()
			+ System.getProperty("file.separator");
	private SimpleDateFormat sdf;
	private NotificationManager nManager;// 通知管理
	private static final int NOTIFICATIONID = R.drawable.msg_icon;
	private BroadcastReceiver mReceiver;

	// GOOGLE定位
	private LocationManager mLocationManager;// 定位管理器
	private Location currentLocation;
	/*----Constant----*/
	private static final int TEN_SECONDS = 10000;// 10秒
	private static final int ALARM_DELTA = 1000 * 60 * 5;
	private static final int TIME_DELTA = 4;// 4小时

	private LocationClient mLocationClient = null;// 百度定位Client
	private BDLocationListener mBaiduListener = new BaiduLocationListener();// 百度定位监听

	public HashMap<String, ArrayList<Integer>> pushMsgMap = new HashMap<String, ArrayList<Integer>>();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public boolean isBaiduProvider() {
		return "baidu".equalsIgnoreCase(getResources().getString(
				R.string.gps_provider));
	}

	@Override
	public void onCreate() {
		super.onCreate();
		/**
		 * 创建屏幕开启或者关闭的监听器
		 */
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		mReceiver = new ScreenReceiver();
		registerReceiver(mReceiver, filter);
		switchOtiginListWithTimeAndLoaction();
		nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		setUpAlarm();
		if (intent == null) {
			if (isBaiduProvider()) {
				initBaiduLoaction();
			} else {
				initGoogleLocation();
			}
			return;
		}
		boolean screenOff = intent.getBooleanExtra("screen_state", false);
		if (!screenOff) {
			// initGoogleLocation();
			if (isBaiduProvider()) {
				initBaiduLoaction();
			} else {
				initGoogleLocation();
			}
			Log.i("background Loc", "screen is on");
		} else {
			if (mLocationManager != null) {
				mLocationManager.removeUpdates(listener);// 取消原有的监听
			}
			if (mLocationClient != null) {
				mLocationClient.stop();
				mLocationClient.unRegisterLocationListener(mBaiduListener);
			}
			Log.i("background Loc", "screen is off");
		}
		// super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			boolean isChangeData = intent.getBooleanExtra(Config.DATA_MODIFY,
					false);
			if (isChangeData) {// 数据改变
				// 更新通知栏状态
				dataSource = new TicketDataSource(this);
				try {
					dataSource.open();
				} catch (SQLException e) {
					e.printStackTrace();
					return -1;
				}
				dataList = dataSource.queryNotifactionConditions();// 从数据库获得原始数据
				mPushMsgList = dataSource.queryPushMessages();// 推送消息列表
				dataSource.close();
				showPushMessage();
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void showPushMessage() {
		/**
		 * 计算出需要取消掉的页面通知
		 */
		boolean isUpdateData = false;
		int originIndex = 0;
		String tempSerial = "";
		for (int i = 0; i < mPushMsgList.size(); i++) {// 加入新的
			PushMessage msg = mPushMsgList.get(i);
			int newId = msg.hashCode();
			ArrayList<Integer> value = pushMsgMap.get(msg.getSerialNumber());
			if (value == null) {
				value = new ArrayList<Integer>();
			} else {
				if (i == 0) {// 更新数据
					isUpdateData = true;
					originIndex = value.size();
					tempSerial = msg.getSerialNumber();
				}
			}
			value.add(newId);
			pushMsgMap.put(msg.getSerialNumber(), value);
			showNotification(msg.getChangeMessage(), msg.getLogoText(),
					msg.getSerialNumber(), newId);

		}// end for i
		if (isUpdateData) {// 若是更新 则需要删除原有的
			ArrayList<Integer> value = pushMsgMap.get(tempSerial);
			for (int i = 0; i < originIndex; i++) {
				Integer id = value.get(0);
				nManager.cancel(id);
				value.remove(0);// 删除第一个
			}// end for i
		}
		HashMap<String, ArrayList<Integer>> saveMap = new HashMap<String, ArrayList<Integer>>();
		for (int i = 0; i < dataList.size(); i++) {
			String serial = dataList.get(i).getSerialNumber();
			ArrayList<Integer> idList = pushMsgMap.get(serial);
			if (idList != null) {
				saveMap.put(serial, idList);
				pushMsgMap.remove(serial);
			}
		}// end for i
		for (String key : pushMsgMap.keySet()) {
			ArrayList<Integer> idList = pushMsgMap.get(key);
			for (int j = 0; j < idList.size(); j++) {
				nManager.cancel(idList.get(j));
			}// end for j
		}// end for each
		pushMsgMap = saveMap;// 还原
	}

	@Override
	public void onDestroy() {
		cancelNotification();
		if (mLocationClient != null) {
			mLocationClient.stop();
			mLocationClient.unRegisterLocationListener(mBaiduListener);
		}
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(listener);//
		}
		// cancelEvent();
		unregisterReceiver(mReceiver);
		Log.i("background Loc", "Loc stop");
		super.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	private void switchOtiginListWithTimeAndLoaction() {
		dataSource = new TicketDataSource(this);
		try {
			dataSource.open();
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		dataList = dataSource.queryNotifactionConditions();// 从数据库获得原始数据
		dataSource.close();

		itemList = new ArrayList<Item>();// 初始化容器
		for (int i = 0; i < dataList.size(); i++) {
			NotifactionCondition origin = dataList.get(i);// 原始数据项
			Item item = new Item();
			item.setSerialNumber(origin.getSerialNumber());
			if (origin.getDate() != null) {
				Date date = DateUtil.parseISODate(origin.getDate());
				item.setTime(date);
			}
			item.setTimeInfo(origin.getOrganizationName());
			// 处理位置JSON数据
			JSONArray jsonArray;
			if (origin.getLocations() != null) {
				try {
					jsonArray = new JSONArray(origin.getLocations());
					for (int j = 0; j < jsonArray.length(); j++) {
						JSONObject obj = jsonArray.getJSONObject(j);
						item.setLocationInfo(obj.getString("relevantText"));
						item.getLocList().add(
								new Loc(obj.getDouble("longitude"), obj
										.getDouble("latitude")));
					}// end for
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			itemList.add(item);
		}// end for
	}

	public void initShop() {
	}

	public void initEventRecommand() {
	}

	/**
	 * 设置Alarm服务每隔10分钟 启动一次Service
	 */
	private void setUpAlarm() {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(START_SERVICE);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		long atTimeInMillis = System.currentTimeMillis() + ALARM_DELTA;
		alarmManager.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
	}

	/**
	 * 监听器
	 */
	private final LocationListener listener = new LocationListener() {
		/**
		 * 位置信息改变
		 */
		@Override
		public void onLocationChanged(Location location) {
			currentLocation = location;
			sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String str = sdf.format(new Date()).toString();
			str = str + "   当前位置:" + currentLocation.getLatitude() + ","
					+ currentLocation.getLongitude();
			// System.out.println(str);
			logUtil.i(str);
			detect(currentLocation.getLatitude(),
					currentLocation.getLongitude());
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
	};

	/**
	 * 判定
	 * 
	 * @param currentLat
	 * @param currentLon
	 */
	private void detect(double currentLat, double currentLon) {
		List<Double> locationArray = MapUtils.BaiduToGoogle(currentLat,
				currentLon);
		currentLat = locationArray.get(0);
		currentLon = locationArray.get(1);
		ArrayList<Item> retList = new ArrayList<Item>();
		for (Item item : itemList) {
			if (item.getLocList().size() >= 1) {// 含有地理位置信息
				List<Loc> locList = item.getLocList();
				for (int i = 0; i < locList.size(); i++) {
					double lat, lng;
					List<Double> arr = MapUtils.transformToMars(locList.get(i)
							.getLatitude(), locList.get(i).getLongitude());
					lat = arr.get(0);
					lng = arr.get(1);
					double distance = LocationUtil.getDistance(currentLat,
							currentLon, lat, lng);// 计算距离

					String out_Str = item.getLocationInfo() + "   距离:"
							+ distance + ",   经纬度-->"
							+ locList.get(i).getLatitude() + ","
							+ locList.get(i).getLongitude();
//					System.out.println(out_Str);
					logUtil.i(out_Str);
					if (distance <= RADIUS) {// 在指定范围之内
						// 在指定时间范围之内
						// SimpleDateFormat s = new SimpleDateFormat(
						// "yyyy-MM-dd HH:mm:ss");
						// String out_Strs =
						// s.format(item.getTime()).toString();
						// logUtil.i(out_Strs);
						if (item.getTime() != null) {
							// logUtil.i("含有时间信息!"+out_Strs);
							if (DateUtil.isInTimeLength(item.getTime(),
									TIME_DELTA, new Date())) {// 在指定时间范围之内
								logUtil.i("在指定时间范围内");
								retList.add(item);
								break;
							} else {
								logUtil.i("时间信息已过");
								break;
							}
						} else {// 没有时间信息
							logUtil.i("没有时间信息!");
							retList.add(item);
							break;
						}
					}
				}// end for i
			} else if (item.getTime() != null) {// 仅有时间
				SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				logUtil.i("仅有时间哦!--->" + s.format(item.getTime()));
				if (DateUtil.isInTimeLength(item.getTime(), TIME_DELTA,
						new Date())) {
					retList.add(item);
				}
			}// end if
		}// end for

		int listSize = retList.size();
		if (listSize >= 1) {
			Item item = retList.get(Utils.genRand(listSize));
			StringBuffer sb = new StringBuffer();
			for (Item it : retList) {
				sb.append(it.getSerialNumber()).append(",");
			}// end for
			int len = sb.toString().length() - 1;
			String serial = sb.toString().substring(0, len);
			// System.out.println("size-->"+serial);
			if (item.getLocList().size() >= 1) {// 有地理位置信息
				showNotification(item.getLocationInfo(), item.getTimeInfo(),
						serial);
			} else {// 仅有时间信息
				SimpleDateFormat s = new SimpleDateFormat("HH:mm");
				showNotification(s.format(item.getTime()), item.getTimeInfo(),
						serial);
			}
		} else {
			cancelNotification();
		}
	}

	/**
	 * 谷歌定位初始化
	 */
	private void initGoogleLocation() {
		logUtil.i("GOOGLE服务");
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);// 获取系统定位服务
		final boolean gpsEnabled = mLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);// 检查GPS定位是否开启
		setUpGoogleLocation();// 加载定位服务
	}

	/**
	 * 初始化百度定位
	 */
	private void initBaiduLoaction() {
		if (mLocationClient == null) {
			mLocationClient = new LocationClient(this);// Client实例化
			LocationClientOption option = new LocationClientOption();
			// option.setOpenGps(true);// 使用GPS定位
			option.setOpenGps(false);
			option.setAddrType("detail");
			option.setCoorType("bd09ll");
			option.setScanSpan(10000);// 10秒检测一次地理位置
			mLocationClient.setLocOption(option);
			logUtil.i("Client实例化");
		} else {
			logUtil.i("重新设置mLocationClient  " + mLocationClient.isStarted());
			mLocationClient.stop();
			mLocationClient.unRegisterLocationListener(mBaiduListener);
			logUtil.i("取消原有监听");
		}
		mLocationClient.registerLocationListener(mBaiduListener);
		mLocationClient.start();
	}

	private void setUpGoogleLocation() {
		Location gpsLocation = null, networkLocation = null;
		mLocationManager.removeUpdates(listener);// 取消原有的监听
		networkLocation = requestUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
		currentLocation = networkLocation;
	}

	private Location requestUpdatesFromProvider(final String provider) {
		Location location = null;
		if (mLocationManager.isProviderEnabled(provider)) {
			mLocationManager.requestLocationUpdates(provider, TEN_SECONDS, 0,
					listener);// 每隔十秒位置更新一次
			location = mLocationManager.getLastKnownLocation(provider);
		}
		return location;
	}

	/**
	 * 取消通知
	 */
	public void cancelNotification() {
		nManager.cancel(NOTIFICATIONID);
	}

	/**
	 * 显示提醒通知
	 * 
	 * @param text
	 * @param index
	 */
	private void showNotification(String text, String title,
			String serialStrings) {
		Notification notification = new Notification(NOTIFICATIONID, null,
				System.currentTimeMillis());
		Intent intent = new Intent();

		intent.setClass(this, TicketPanel.class);
		intent.putExtra(Config.SERIAL, serialStrings);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.setLatestEventInfo(this, title, text, contentIntent);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		// System.out.println("put-->"+serialStrings);
		nManager.notify(NOTIFICATIONID, notification);
	}

	/**
	 * 显示提醒通知
	 * 
	 * @param text
	 * @param index
	 */
	private void showNotification(String text, String title,
			String serialStrings, int id) {
		Notification notification = new Notification();
		notification.icon = NOTIFICATIONID;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		// notification.defaults |= Notification.DEFAULT_SOUND;
		Intent intent = new Intent();
		intent.setClass(this, TicketPanel.class);
		intent.putExtra(Config.SERIAL, serialStrings);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, id + 1,
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.setLatestEventInfo(this, title, text, contentIntent);
		nManager.notify(id, notification);
	}

	/**
	 * 接受屏幕的改变
	 * 
	 * @author pengfan
	 * 
	 */
	private static class ScreenReceiver extends BroadcastReceiver {

		private boolean screenOff;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				screenOff = true;
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				screenOff = false;
			}
			Intent i = new Intent(context, LocService.class);
			i.putExtra("screen_state", screenOff);
			context.startService(i);
		}
	}

	/**
	 * 百度定位事件监听
	 * 
	 * @author Panyi
	 * 
	 */
	private class BaiduLocationListener implements BDLocationListener {
		@Override
		public void onReceiveLocation(BDLocation location) {
			if (location == null)
				return;
			sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			String str = sdf.format(new Date()).toString();
			str = str + "   当前位置:" + location.getLatitude() + ","
					+ location.getLongitude();
			logUtil.i(str);
			// 转成谷歌坐标
			detect(location.getLatitude(), location.getLongitude());
		}

		@Override
		public void onReceivePoi(BDLocation poiLocation) {
		}
	}// end inner class
}
