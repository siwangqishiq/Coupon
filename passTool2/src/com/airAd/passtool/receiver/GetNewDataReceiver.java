package com.airAd.passtool.receiver;

import com.airAd.passtool.LocService;
import com.airAd.passtool.data.Config;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 接收重启提醒服务广播，如果接收到 则重启服务以更新数据
 * @author Panyi
 *
 */
public class GetNewDataReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent it = new Intent(context, LocService.class);
		it.putExtra(Config.DATA_MODIFY, true);
		context.stopService(it);//关闭当前服务
		context.startService(it);//重启服务
	}
}//end class
