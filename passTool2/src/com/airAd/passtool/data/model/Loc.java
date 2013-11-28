package com.airAd.passtool.data.model;

public class Loc {
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	private double longitude;//经度
	private double latitude;//维度
	public Loc(double longitude, double latitude) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
	}
}
