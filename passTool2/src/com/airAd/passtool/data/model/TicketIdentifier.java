package com.airAd.passtool.data.model;

import java.io.Serializable;
import java.util.Date;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;

/**
 * 用于标识一个ticket
 * @author pengfan
 *
 */
public class TicketIdentifier implements Serializable {

    private static final long serialVersionUID = -8698611770048865988L;

    private String passTypeIdentifier;
    private String serialNumber;
    private String authenticationToken;
    private String webServiceUrl;
    private Date lastUpdateTime;
    private String folderName;
    private boolean autoUpdate;

    public TicketIdentifier() {

    }

    public String getPassTypeIdentifier() {
        return passTypeIdentifier;
    }

    public void setPassTypeIdentifier(String passTypeIdentifier) {
        this.passTypeIdentifier = passTypeIdentifier;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(String authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    public String getWebServiceUrl() {
        return webServiceUrl;
    }

    public void setWebServiceUrl(String webServiceUrl) {
        this.webServiceUrl = webServiceUrl;
    }

    public String getLastUpdateTime() {
        return Config.DATE_FORMATER.format(lastUpdateTime);
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(int autoUpdate) {
        if (autoUpdate == TicketDataSource.AUTO_UPDATE_ENABLED) {
            this.autoUpdate = true;
        } else {
            this.autoUpdate = false;
        }

    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public String toString() {
        return getSerialNumber() + "," + getWebServiceUrl() + "," + getLastUpdateTime() + "," + isAutoUpdate() + "," + getFolderName();
    }

}
