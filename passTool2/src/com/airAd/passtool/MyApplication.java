package com.airAd.passtool;

import java.util.Stack;

import org.json.JSONException;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;

import cn.jpush.android.api.JPushInterface;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.exception.CrashHandler;
import com.airAd.sentry.Sentry;
import com.airAd.sentry.Sentry.SentryEventBuilder;
import com.airAd.sentry.Sentry.SentryEventCaptureListener;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/**
 * 程序运行时环境和应用环境配置
 * 
 * @author pengfan
 * 
 */
public class MyApplication extends Application {

	private DisplayMetrics metrics = new DisplayMetrics();
	private Stack<Object> stack;
	private Config config;
	private static MyApplication currentApp;

	protected CrashHandler crashHandler;

	private IWXAPI mWxApi;

	@Override
	public void onCreate() {
		super.onCreate();
		/*
		 * CrashHandler crashHandler = CrashHandler.getInstance();
		 * crashHandler.init(getApplicationContext());
		 */
		// System.out.println("-->"+(new Config(this)).getDeviceId());

		try {
			JPushInterface.init(this);
			JPushInterface.setAliasAndTags(this,
					(new Config(this)).getDeviceId(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//注册微信
		mWxApi = WXAPIFactory.createWXAPI(this, Config.WENXIN_APP_ID, false);
		mWxApi.registerApp(Config.WENXIN_APP_ID);

		currentApp = this;
		stack = new Stack<Object>();
		config = new Config(this);

		Sentry.setCaptureListener(new SentryEventCaptureListener() {
			@Override
			public SentryEventBuilder beforeCapture(SentryEventBuilder builder) {
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager
						.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				try {
					builder.getExtra().put("wifi",
							String.valueOf(mWifi.isConnected()));
					PackageManager packageManager = getPackageManager();
					// getPackageName()是你当前类的包名，0代表是获取版本信息
					PackageInfo packInfo = packageManager.getPackageInfo(
							getPackageName(), 0);
					String version = packInfo.versionName;
					builder.getExtra().put("version", version);
				} catch (Exception e) {
				}
				return builder;
			}
		});
		// Sentry will look for uncaught exceptions from previous runs and send
		// them
		Sentry.init(this, CrashHandler.rawDsn);
		crashHandler = new CrashHandler();
		crashHandler.init(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public DisplayMetrics getMetrics(Activity act) {
		if (metrics.widthPixels <= 0) {
			act.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		}
		return metrics;
	}

	public Object pop() {
		if (stack.isEmpty()) {
			return null;
		}
		return stack.pop();
	}

	public void push(Object obj) {
		stack.push(obj);
	}

	public Config getConfig() {
		return config;
	}

	public static MyApplication getCurrentApp() {
		return currentApp;
	}

}
