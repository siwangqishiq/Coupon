package com.airAd.passtool.receiver;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.airAd.passtool.UpdateService;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.TicketIdentifier;
import com.airAd.passtool.service.TicketRefreshService;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.net.Response;
import com.airAd.passtool.worker.FileNetWorker;
import com.airAd.passtool.worker.FileNetWorkerHandler;

import cn.jpush.android.api.JPushInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * 接收推送
 * 
 * @author panyi
 * 
 */
public class PushReceiver extends BroadcastReceiver {
	protected Context mContext;
	private TicketDataSource dataSource;
	protected List<String> serialNumbers;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (JPushInterface.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {// 推送的消息
			mContext = context;
			serialNumbers = new ArrayList<String>(3);
			Bundle bundle = intent.getExtras();
			String orignJson = bundle.getString(JPushInterface.EXTRA_MESSAGE);
			System.out.println("自定义消息-->" + orignJson);
//			Toast.makeText(mContext, orignJson, Toast.LENGTH_LONG).show();
			resolveOriginJson(orignJson);
			updateCoupons();
		}
	}

	/**
	 * 解析原始JSON数据
	 * 
	 * @param origin
	 */
	private void resolveOriginJson(String origin) {
		try {
			JSONObject obj = new JSONObject(origin);
			JSONArray array = obj.getJSONArray("sn");
			for (int i = 0, length = array.length(); i < length; i++) {
				serialNumbers.add(array.getString(i));
			}// end for i
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	FileNetWorkerHandler refreshHandler = new FileNetWorkerHandler() {
		@Override
		public void handleData(Response data) {
			LogUtil.e(UpdateService.class, "返回状态 : " + data.getStatus());
			
		}

		@Override
		public void progressUpdate(Object[] vals) {
		}
	};

	/**
	 * 更新需要变更的券
	 */
	private void updateCoupons() {
		dataSource = new TicketDataSource(mContext);
		try {
			dataSource.open();
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		List<TicketIdentifier> ticketIds = dataSource
				.queryTicketIdentifiers(serialNumbers);
		LogUtil.i(UpdateService.class, "need register list" + ticketIds.size());
		dataSource.close();
		for (TicketIdentifier ti : ticketIds) {
			TicketRefreshService refreshServiceItem = new TicketRefreshService(
					mContext);
			refreshServiceItem.setExtern(ti.getSerialNumber());
			refreshServiceItem.setTicketIdentifier(ti);
//			System.out.println("请求连接 : " + ti);
			(new FileNetWorker(mContext)).request(refreshServiceItem,
					refreshHandler);
		}// end for
	}

}// end class
