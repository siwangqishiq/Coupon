package com.airAd.passtool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 启动Receiver
 * @author pengfan
 *
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, UpdateService.class));
    }
}
