package com.airAd.passtool.data;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.airAd.passtool.R;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.StringUtil;

/**
 * 应用参数的运行时配置读取
 * 
 * @author pengfan
 * 
 */
public class Config {
	private Context context;

	public static final String HOME_NAV_URL = "http://youhui.airad.com/couponDetail?cpyId=69&c=18";
	public static final String UPDATE_HOST = "http://emms.airad.com/";

	public static final String PRESET_URL = "preSetURL";
	public static final String PASS_TYPE_IDENTIFIER = "passTypeIdentifier";
	public static final String HOST = "host";
	public static final String OS_TYPE = "1";
	public static final String VERSION = "v1";
	public static final String LAST_UPDATE_TIME = "lastUpdateTime";
	public static final SimpleDateFormat DATE_FORMATER = new SimpleDateFormat(
			"yyyyMMddHHmmss");
	private static SimpleDateFormat GMT = new SimpleDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
	public static final String HOST_DEFAULT = "";// 如果取不到的默认情况
	public static final String SERIAL = "serial";
	public static final int NOTIFY_EVENT_ID = 101;
	public static final int PUSH_MSG_BASECODE = 2000;
	public static final String DATA_MODIFY = "data_modify";
	public static final String HOSTS = "http://emms.airad.com/passapi/";
	public static final String UNUPDATE_ITEMS = "unUpdateItems";

	// public static final String HOSTS="http://mail.airad.com:8840/";
	
	public static final String WENXIN_APP_ID="wxb96e11dd3b6e43ab";//微信APP_ID

	public synchronized String getUnUpdateItems() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sp.getString(UNUPDATE_ITEMS, "");
	}

	public synchronized void removeUnUpdateItem(String removeItem) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		String origin = sp.getString(UNUPDATE_ITEMS, "");
		StringBuffer sb = new StringBuffer();
		String[] origins = origin.split(",");
		for (int i = 0; i < origins.length; i++) {
			if (!removeItem.equalsIgnoreCase(origins[i])) {
				if (i == 0) {
					sb.append(origins[i]);
				} else {
					sb.append(",").append(origins[i]);
				}
			}
		}// end for i
		sp.edit().putString(UNUPDATE_ITEMS, sb.toString()).commit();
		//LogUtil.e(Config.class, "更新失败券表--->"+sb.toString());
	}

	public synchronized void addUnUpdateItem(String item) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		String origin = sp.getString(UNUPDATE_ITEMS, "");
		if (StringUtil.isBlank(origin)) {
			sp.edit().putString(UNUPDATE_ITEMS, item).commit();
		} else {
			sp.edit().putString(UNUPDATE_ITEMS, origin + "," + item).commit();
		}
		//LogUtil.e(Config.class, "增加失败券表--->"+origin + "," + item);
	}

	public Config(Context context) {
		this.context = context;
		Properties properties = new Properties();
		try {
			properties.load(context.getResources().openRawResource(R.raw.host));
//			HOME_NAV_URL = properties.getProperty("home.nav.url");
//			UPDATE_HOST = properties.getProperty("update.host");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置预先加载的url
	 */
	public void setPreSetUrl(String url) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		sp.edit().putString(PRESET_URL, url).commit();
	}

	/**
	 * 是否预先加载
	 * 
	 * @param url
	 * @return
	 */
	public boolean isPreSetUrlLoaded(String url) {
		String persistURL = PreferenceManager.getDefaultSharedPreferences(
				context).getString(PRESET_URL, null);
		if (url.equals(persistURL)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取唯一标示
	 * 
	 * @return
	 */
	public String getPassTypeIdentifier() {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString(PASS_TYPE_IDENTIFIER, null);
	}

	public void setPassTypeIdentifier(String value) {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		sp.edit().putString(PASS_TYPE_IDENTIFIER, value).commit();
	}

	public void clear() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		sp.edit().clear().commit();
	}

	/**
	 * 获取远程接口的url
	 */
	public Set<String> getWebServiceHost() {
		Set<String> set = new HashSet<String>();
		// String string_str =
		// PreferenceManager.getDefaultSharedPreferences(context).getString(HOST,
		// null);
		String string_str = Config.HOSTS;
		if (string_str != null) {
			for (String str : string_str.split(",")) {
				if (!"".equals(str)) {
					set.add(str);
				}
			}
		}
		return set;
	}

	/**
	 * 把webService的host进行缓存。
	 * 
	 * @param host
	 */
	public void addWebServiceHosts(String host) {
		// Set<String> set = getWebServiceHost();
		// SharedPreferences sp =
		// PreferenceManager.getDefaultSharedPreferences(context);
		// if (!set.contains(host)) {
		// String string_str = sp.getString(HOST, "");
		// string_str = string_str + "," + host;
		// sp.edit().putString(HOST, string_str).commit();
		// }
	}

	public String getOSType() {
		return OS_TYPE;
	}

	/**
	 * 获取上一次更新时间
	 * 
	 * @return
	 */
	public String getLastUpdateTime() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sp.getString(LAST_UPDATE_TIME, "");
	}

	/**
	 * 从请求头中获取的服务器头信息来更新时间戳
	 */
	public void updateLastTime(String headerLastModify) {
		LogUtil.e(Config.class, "更新时间" + headerLastModify);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		sp.edit().putString(LAST_UPDATE_TIME, headerLastModify).commit();
	}

	/**
	 * 获取唯一设备号
	 * 
	 * @return
	 */
	public String getDeviceId() {
		TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getDeviceId();
	}

	public String getVersion() {
		return VERSION;
	}
}
