package com.airAd.passtool.util;

import java.util.ArrayList;
import java.util.List;

public class MapUtils {
	public static final double X_PI = Math.PI * 3000.0f / 180.0f;

	public static List<Double> GoogleToBaidu(double gg_lat, double gg_lon) {
		List<Double> ret = new ArrayList<Double>(2);

		double bd_lat, bd_lon;

		double x = gg_lon, y = gg_lat;
		double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * X_PI);
		double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * X_PI);
		bd_lon = z * Math.cos(theta) + 0.0065;
		bd_lat = z * Math.sin(theta) + 0.006;

		ret.add(bd_lat);
		ret.add(bd_lon);

		return ret;
	}

	public static List<Double> BaiduToGoogle(double bd_lat, double bd_lon) {
		List<Double> ret = new ArrayList<Double>(2);

		double x = bd_lon - 0.0065, y = bd_lat - 0.006;
		double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
		double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
		double gg_lon = z * Math.cos(theta);
		double gg_lat = z * Math.sin(theta);

		ret.add(gg_lat);
		ret.add(gg_lon);

		return ret;
	}
	
	
	public static List<Double> transformToMars(double wgLat, double wgLon)
    {
		double pi = Math.PI;
		double a = 6378245.0;
	    double ee = 0.00669342162296594323;
		List<Double> ret = new ArrayList<Double>(2);
        double dLat = transformLat(wgLon - 105.0, wgLat - 35.0);
        double dLon = transformLon(wgLon - 105.0, wgLat - 35.0);
        double radLat = wgLat / 180.0 * pi;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
        double mgLat = wgLat + dLat;
        double mgLon = wgLon + dLon;
        
        ret.add(mgLat);
        ret.add(mgLon);
        
        return ret;
    }
	

	public static double transformLat(double x, double y) {
		double pi = Math.PI;
		double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y
				+ 0.2 * Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
		ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
		return ret;
	}

	public static double transformLon(double x, double y) {
		double pi = Math.PI;
		double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1
				* Math.sqrt(Math.abs(x));
		ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
		ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
		ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0
				* pi)) * 2.0 / 3.0;
		return ret;
	}
}// end class
