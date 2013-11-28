package com.airAd.passtool.data.model;

import java.io.Serializable;
import java.util.List;

/**
 * 票据的模型
 * @author pengfan
 *
 *
 */
public class Ticket implements Serializable {

    private static final long serialVersionUID = 8188054556083392819L;
    
    private String organizationName;//所属名
    private int backgroundColor;//背景色
    private int labelColor;//提示文字颜色
    private int foregroundColor;//文字颜色

    private String logoText;//主标题
    private String folderName;//存储的文件夹名字 
    private List<Field> primaryField;//主显示区域
    private List<Field> headFields;//头部显示区域
    private List<Field> secondaryFields;//第二显示区域
    private List<Field> auxiliaryFields;//附加显示区域
    private List<Field> backFields; //详情显示区域

    private TicketIdentifier ticketIdentifier;
    private BarCode barcode; //二维码
	private String shareUrl;
    
    private boolean isMonkeyCoupon=false;//显示是否是代金券
    private String monkey;//金额

    //优惠劵
    public static final int TYPE_COUPON = 0;
    //事件类型
    public static final int TYPE_EVENT = 1;

    public Ticket() {
        ticketIdentifier = new TicketIdentifier();
    }
    
    public String getShareUrl() {
		return shareUrl;
	}

	public void setShareUrl(String shareUrl) {
		this.shareUrl = shareUrl;
	}

    public TicketIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(TicketIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    public int getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(int foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public String getLogoText() {
        return logoText;
    }

    public void setLogoText(String logoText) {
        this.logoText = logoText;
    }

    public List<Field> getPrimaryField() {
        return primaryField;
    }

    public void setPrimaryField(List<Field> primaryField) {
        this.primaryField = primaryField;
    }

    public List<Field> getHeadFields() {
        return headFields;
    }

    public void setHeadFields(List<Field> headFields) {
        this.headFields = headFields;
    }

    public List<Field> getSecondaryFields() {
        return secondaryFields;
    }

    public void setSecondaryFields(List<Field> secondaryFields) {
        this.secondaryFields = secondaryFields;
    }

    public List<Field> getAuxiliaryFields() {
        return auxiliaryFields;
    }

    public void setAuxiliaryFields(List<Field> auxiliaryFields) {
        this.auxiliaryFields = auxiliaryFields;
    }

    public List<Field> getBackFields() {
        return backFields;
    }

    public void setBackFields(List<Field> backFields) {
        this.backFields = backFields;
    }

    public BarCode getBarcode() {
        return barcode;
    }

    public void setBarcode(BarCode barcode) {
        this.barcode = barcode;
    }

}
