package com.airAd.passtool.data.model;

public class PushMessage {

    public static final int FLAG_NO_NOTIFY = 1;
    public static final int FLAG_NOTIFY = 0;

    private String serialNumber;
    private String passTypeIdentifier;
    private String changeMessage;
    private String logoText;
    private int notifyFlag;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getPassTypeIdentifier() {
        return passTypeIdentifier;
    }

    public void setPassTypeIdentifier(String passTypeIdentifier) {
        this.passTypeIdentifier = passTypeIdentifier;
    }

    public String getChangeMessage() {
        return changeMessage;
    }

    public void setChangeMessage(String changeMessage) {
        this.changeMessage = changeMessage;
    }

    public int getNotifyFlag() {
        return notifyFlag;
    }

    public void setNotifyFlag(int notifyFlag) {
        this.notifyFlag = notifyFlag;
    }

    public String getLogoText() {
        return logoText;
    }

    public void setLogoText(String logoText) {
        this.logoText = logoText;
    }

}
