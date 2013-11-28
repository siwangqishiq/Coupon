package com.airAd.passtool.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.airAd.passtool.LocService;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// context.startService(service)
		// System.out.println("接收服务!!!!");
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		if(sp.getBoolean("setLocService", true)){
			Intent it = new Intent(context, LocService.class);
			context.startService(it);
		}
	}
}
