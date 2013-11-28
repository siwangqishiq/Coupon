package com.airAd.passtool.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 商户地理位置
 * @author pp
 *
 */
public class ShopLocation {
    public List<Loc> getLocList() {
		return locList;
	}
	public void setLocList(List<Loc> locList) {
		this.locList = locList;
	}
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public List<Loc> getLocationList() {
		return locList;
	}
	public void setLocationList(List<Loc> locList) {
		this.locList = locList;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	public String getRelevantText() {
		return relevantText;
	}
	public void setRelevantText(String relevantText) {
		this.relevantText = relevantText;
	}
	private List<Loc> locList;
	private String relevantText;
	private String folderName;
	private String serialNumber;
	public ShopLocation(String relevantText) {
		super();
		this.relevantText = relevantText;
		locList=new ArrayList<Loc>();
	}
	public ShopLocation() {
		super();
		locList=new ArrayList<Loc>();
	}
}//end class
