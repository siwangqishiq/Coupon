package com.airAd.passtool.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据项 包含位置以及 时间信息
 * @author Administrator
 *
 */
public class Item {
	private String serialNumber;
	private String locationInfo;
	private String timeInfo;
	private List<Loc> locList;
	private Date time;
	
	public Item(){
		super();
		locList= new ArrayList<Loc>();
	}
	
	public String getSerialNumber() {
		return serialNumber;
	}
	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	public String getLocationInfo() {
		return locationInfo;
	}
	public void setLocationInfo(String locationInfo) {
		this.locationInfo = locationInfo;
	}
	public String getTimeInfo() {
		return timeInfo;
	}
	public void setTimeInfo(String timeInfo) {
		this.timeInfo = timeInfo;
	}
	public List<Loc> getLocList() {
		return locList;
	}
	public void setLocList(List<Loc> locList) {
		this.locList = locList;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
}//end class
