package com.airAd.passtool.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 支持苹果的时间格式
 * @author pengfan
 *
 */
public class DateUtil {

    private static final SimpleDateFormat formater = new SimpleDateFormat("HH:mm");

    /**
     * 
     * @param iso8601string
     * @return
     */
    public static Date parseISODate(final String iso8601string) {
        String s = iso8601string.replace("Z", "+00:00");
        if (!s.contains(":")) {
            s = s + ":00";
        }
        try {
            s = s.substring(0, 22) + s.substring(23);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 转换时间格式
     * @param date
     * @return
     */
    public static String format(Date date) {
        return formater.format(date);
    }
    
    /**
     * 判断指定时间是否在设定时间范围之内
     * @param setDate
     * @param hours
     * @param date
     * @return
     */
    public static boolean isInTimeLength(Date setDate,int hours,Date date){
    	SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	Date startTime=new Date(setDate.toGMTString());
    	startTime.setHours(startTime.getHours()-hours);
    	Date endTime=new Date(setDate.toGMTString());
    	endTime.setHours(endTime.getHours()+hours);
    	if(date.after(startTime) && date.before(endTime)){
    		return true;
    	}
    	return false;
    }
}
