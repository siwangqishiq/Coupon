package com.airAd.passtool.service;

import org.apache.http.HttpResponse;

import android.content.Context;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.model.TicketIdentifier;
import com.airAd.passtool.util.net.BasicService;
import com.airAd.passtool.util.net.Response;

/**
 * 下载票据的接口
 * @author pengfan
 *
 */
public class TicketDeleteService extends BasicService {

    public static final int successStatus = 201;

    private TicketIdentifier ticketIdentifier;

    public TicketDeleteService(Context context) {
        super(TYPE_DELETE);
    }

    @Override
    public void handleResponse(HttpResponse httpResponse, Response rsp) {
    }

    public TicketIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(TicketIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
        addHeader("authorization", "ApplePass " + ticketIdentifier.getAuthenticationToken());
    }

    //TODO:暂时返回一个
    /**
     * 暂时返回一个
     */
    /*http://webserviceURL/{version}/devices/{deviceLibraryIdentifier}/registrations/{passTypeIdentifier}?passesUpdatedSince={passesUpdatedSince}   
     * method_type=get*/
    @Override
    public String getRemoteUrl() {
        Config config = MyApplication.getCurrentApp().getConfig();
        StringBuffer sb = new StringBuffer(ticketIdentifier.getWebServiceUrl());
        sb.append("/").append(config.getVersion());
        sb.append("/devices/").append(config.getDeviceId());
        sb.append("/registrations/").append(ticketIdentifier.getPassTypeIdentifier());
        sb.append("/").append(ticketIdentifier.getSerialNumber());
        sb.append("?os=").append(config.getOSType());
        return sb.toString();
    }
}
