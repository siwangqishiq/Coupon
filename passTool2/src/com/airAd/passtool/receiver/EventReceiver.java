package com.airAd.passtool.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.airAd.passtool.R;
import com.airAd.passtool.TicketPanel;
import com.airAd.passtool.data.Config;

public class EventReceiver extends BroadcastReceiver {
	private Context mContext;
	private static final int NOTIFICATIONID = R.drawable.msg_icon;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.mContext = context;
		String event = intent.getStringExtra("event");
		String title = intent.getStringExtra("title");
		// int id = intent.getIntExtra("id", -1);
		String serial = intent.getStringExtra(Config.SERIAL);
		// System.out.println("--->"+event);
		showNotification(serial, event, title);
	}

	/**
	 * 显示提醒通知
	 * 
	 * @param text
	 * @param index
	 */
	private void showNotification(String serial, String title, String text) {
//		NotificationManager nManager = (NotificationManager) mContext
//				.getSystemService(Context.NOTIFICATION_SERVICE);
//		Notification notification = new Notification(R.drawable.icon, null,
//				System.currentTimeMillis());
//		// ConfigUtil.setAlarmParams(context, notification);
//		Intent it = new Intent();
//		it.setClass(mContext, TicketPanel.class);
//		it.putExtra(Config.SERIAL, serial);
//		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
//				it, 0);
//		notification.flags |= Notification.FLAG_NO_CLEAR;
//		notification.setLatestEventInfo(mContext, title, text, contentIntent);
//		nManager.notify(Config.NOTIFY_EVENT_ID, notification);
	}

}// end class
