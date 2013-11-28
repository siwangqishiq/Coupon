package com.airAd.passtool.receiver;

import com.airAd.passtool.R;
import com.airAd.passtool.data.Config;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 取消通知栏信息
 * 
 * @author Administrator
 * 
 */
public class CancelEventReceiver extends BroadcastReceiver {
	private Context context;
	private static final int NOTIFICATIONID = R.drawable.msg_icon;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		int id = intent.getIntExtra("id", -1);
		NotificationManager nManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nManager.cancel(Config.NOTIFY_EVENT_ID);
	}
}// end class
