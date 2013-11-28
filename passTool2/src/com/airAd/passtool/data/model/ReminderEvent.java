package com.airAd.passtool.data.model;

import java.util.Date;

public class ReminderEvent {
	private Integer id;
	private Date time;
	private String title;
	private String event;
	private boolean register;
	private String serialNumber;

	public ReminderEvent(Integer id, Date time, String event, String title) {
		super();
		this.id = id;
		this.time = time;
		this.event = event;
		this.title = title;
		register = false;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public boolean getRegister() {
		return register;
	}

	public void setRegister(boolean register) {
		this.register = register;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
}// end class
