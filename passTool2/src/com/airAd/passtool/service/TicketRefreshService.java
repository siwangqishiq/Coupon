package com.airAd.passtool.service;

import android.content.Context;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.model.TicketIdentifier;

/**
 * 票卷更新接口
 * @author pengfan
 *
 */
public class TicketRefreshService extends TicketService {

    public static final int RefreshSuccess = 200;
    public static final int NoNeedToRefresh = 304;

    private TicketIdentifier ticketIdentifier;

    public TicketRefreshService(Context context) {
        super(context, TYPE_POST);
        setUpdate(false);
    }

    public TicketIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(TicketIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
        addHeader("authorization", "ApplePass " + ticketIdentifier.getAuthenticationToken());
        addHeader("If-Modified-Since", ticketIdentifier.getLastUpdateTime());
    }

    @Override
    public String getRemoteUrl() {
        Config config = MyApplication.getCurrentApp().getConfig();
        StringBuffer sb = new StringBuffer(ticketIdentifier.getWebServiceUrl());
        sb.append("/").append(config.getVersion());
        sb.append("/passes/").append(ticketIdentifier.getPassTypeIdentifier());
        sb.append("/").append(ticketIdentifier.getSerialNumber());
        return sb.toString();
    }
}
