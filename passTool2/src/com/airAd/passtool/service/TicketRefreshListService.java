package com.airAd.passtool.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.StringUtil;
import com.airAd.passtool.util.net.BasicService;
import com.airAd.passtool.util.net.Response;

/**
 * 下载票据的接口
 * 
 * @author pengfan
 * 
 */
public class TicketRefreshListService extends BasicService {
	public static final String HEADER_LAST_MODIFIED = "Last-Modified";

	public TicketRefreshListService(Context context) {
		super(TYPE_GET);
	}

	@Override
	public void handleResponse(HttpResponse httpResponse, Response rsp) {
		String oRsp = null;
		List<String> res = new ArrayList<String>();
		try {
			oRsp = EntityUtils.toString(httpResponse.getEntity());
			JSONObject jRsp = new JSONObject(oRsp);
			String lastUpdatedTime = "";
			if (!jRsp.isNull("lastUpdated")) {
				lastUpdatedTime = jRsp.getString("lastUpdated");
			}
			MyApplication.getCurrentApp().getConfig()
					.updateLastTime(lastUpdatedTime);
			JSONArray array = jRsp.getJSONArray("serialNumbers");
			for (int i = 0; i < array.length(); i++) {
				res.add(array.getString(i));
			}
			
			//检查是否存在之前更新失败的券 一起加入新的更新任务中
			String unUpdateTickets = MyApplication.getCurrentApp().getConfig()
					.getUnUpdateItems();
			if (!StringUtil.isBlank(unUpdateTickets)) {
				String[] tickets = unUpdateTickets.split(",");
				for (int i = 0; i < tickets.length; i++) {
					res.add(tickets[i]);
				}// end for i
			}//end if
		} catch (Exception e) {
			e.printStackTrace();
		}
		rsp.setData(res);
	}

	// TODO:暂时返回一个
	/**
	 * 暂时返回一个
	 */
	/*
	 * http://webserviceURL/{version}/devices/{deviceLibraryIdentifier}/
	 * registrations
	 * /{passTypeIdentifier}?passesUpdatedSince={passesUpdatedSince}
	 * method_type=get
	 */
	@Override
	public String getRemoteUrl() {
		Config config = MyApplication.getCurrentApp().getConfig();
		Set<String> set = config.getWebServiceHost();

		// Set<String> set = new HashSet<String>();
		// set.add(Config.HOSTS);

		Iterator<String> iterator = set.iterator();
		StringBuffer sb = new StringBuffer();
		if (iterator.hasNext()) {
			sb.append(iterator.next());
		} else {
			return null;
		}
		sb.append("/").append(config.getVersion());
		sb.append("/devices/").append(config.getDeviceId());
		sb.append("/registrations/").append(config.getPassTypeIdentifier());
		sb.append("?passesUpdatedSince=").append(config.getLastUpdateTime());
		String url = sb.toString();
		LogUtil.i(TicketRefreshListService.class, "------->" + url);
		return url;
	}
}
