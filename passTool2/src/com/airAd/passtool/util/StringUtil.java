package com.airAd.passtool.util;

public class StringUtil {
	public static boolean isBlank(String str) {
		if (null == str || "".equals(str)) {
			return true;
		}
		return false;
	}
}
