package com.airAd.passtool.util;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import com.airAd.passtool.R;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.service.TicketService;

/**
 * 应用级别的配置工具
 * @author pengfan
 *
 */
public class ConfigUtil {

    public static final String NOTIFICATION_ICON_FILE_NAME = "icon@2x.png";
    public static final String LOGO_FILE_NAME = "logo@2x.png";
    public static final String STRIP_FILE_NAME = "strip@2x.png";
    public static final String QR_CODE_FILE_NAME = "chart.png";

    public static final String REFRESH_SERVICE_ACTION_NAME = "com.airAd.passtool.filter.getnewdata";

    /**
     * 根据系统设置，对提醒进行参数设置
     * 
     * @param notification
     */
    public static Notification setAlarmParams(Context context, Notification notification) {
        AudioManager volMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        switch (volMgr.getRingerMode()) {
            // 获取系统设置的铃声模式
            case AudioManager.RINGER_MODE_SILENT :
                // 静音模式，值为0，这时候不震动，不响铃
                notification.sound = null;
                notification.vibrate = null;
                break;
            case AudioManager.RINGER_MODE_VIBRATE :
                // 震动模式，值为1，这时候震动，不响铃
                notification.sound = null;
                notification.defaults |= Notification.DEFAULT_VIBRATE;
                break;
            case AudioManager.RINGER_MODE_NORMAL :
                // 常规模式，值为2，分两种情况：1_响铃但不震动，2_响铃+震动
                Uri ringTone = null;
                // 获取软件的设置
                // SharedPreferences sp =
                // PreferenceManager.getDefaultSharedPreferences(this);
                String ringFile = Settings.System.getString(context.getContentResolver(), Settings.System.NOTIFICATION_SOUND);
                if (ringFile == null) {
                    // 无值，为空，不播放铃声
                    ringTone = null;
                } else if (!TextUtils.isEmpty(ringFile)) {
                    // 有铃声：1，默认2自定义，都返回一个uri
                    ringTone = Uri.parse(ringFile);
                }
                notification.sound = ringTone;
                // boolean vibrate = sp.getBoolean(SystemUtil.KEY_NEW_MAIL_VIBRATE,
                // true);

                // 否则就是需要震动，这时候要看系统是怎么设置的：不震动=0;震动=1；仅在静音模式下震动=2；
                if (volMgr.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_OFF) {
                    // 不震动
                    notification.vibrate = null;
                } else if (volMgr.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER) == AudioManager.VIBRATE_SETTING_ONLY_SILENT) {
                    // 只在静音时震动
                    notification.vibrate = null;
                } else {
                    // 震动
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                }
                // 都给开灯
                break;
            default :
                break;
        }
        return notification;
    }

    /**
     * 获取通知提醒的图片
     * @param folderName
     * @return
     */
    public static Bitmap getNotificationIcon(String folderName) {
        return ImageUtil.getBitmapFromDisk(TicketService.parentFolderPath + folderName + "/" + NOTIFICATION_ICON_FILE_NAME);
    }

    /**
     * 获取logo图片
     * @param folderName
     * @return
     */
    public static Bitmap getLogo(String folderName, Context context) {
        int logo_size = context.getResources().getDimensionPixelSize(R.dimen.logo_size);
        return ImageUtil.getBitmapFromDisk(TicketService.parentFolderPath + folderName + "/" + LOGO_FILE_NAME, logo_size, logo_size);
    }

    /**
     * 获取二维码
     * @param folderName
     * @return
     */
    public static Bitmap getQRCode(String folderName) {
        return ImageUtil.getBitmapFromDisk(TicketService.parentFolderPath + folderName + "/" + QR_CODE_FILE_NAME);
    }

    /**
     * 获取中间缩略图
     * @param folderName
     * @return
     */
    public static Bitmap getStrip(String folderName, Context context) {
        return ImageUtil.getBitmapFromDisk(TicketService.parentFolderPath + folderName + "/" + STRIP_FILE_NAME, -1, -1);
    }

    /**
     * 转换rgb编码到颜色
     * rgb(255,255,255)
     * @param rgb 
     * @return
     */
    public static int parseColor(String rgb) {
        String[] split = rgb.split(",");
        int r = Integer.parseInt(split[0].substring(4));
        int g = Integer.parseInt(split[1]);
        int b = Integer.parseInt(split[2].substring(0, split[2].length() - 1));
        return Color.rgb(r, g, b);
    }

    /**
     * 将boolean转换成int
     * @param autoUpdate
     * @return
     */
    public static int parseEnabledAutoUpdate(boolean autoUpdate) {
        if (autoUpdate) {
            return TicketDataSource.AUTO_UPDATE_ENABLED;
        } else {
            return TicketDataSource.AUTO_UPDATE_DISABLED;
        }

    }
}
