package com.airAd.passtool.service;

import java.io.File;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.R;
import com.airAd.passtool.util.FileUtil;
import com.airAd.passtool.util.net.FileService;
import com.airAd.passtool.util.net.RemoteService;
import com.airAd.passtool.util.net.Response;
import com.airAd.passtool.worker.FileNetWorkerHandler;

/**
 * apk下载的url
 * 
 * @author pengfan
 * 
 */
public class ApkDownloadService extends FileService {
	private static final File tempApkFile = FileUtil.getDiskCacheDir(
			MyApplication.getCurrentApp().getApplicationContext(),
			"passbook.apk");
	private static final int DOWNLOAD_NOTIFICATIONID = R.drawable.msg_downloading;

	private String apkUrl;
	private Context appContext;
	private Notification notification;
	private long pre_time = 0;

	public ApkDownloadService() {
		super(TYPE_GET);
		setDownloadFile(tempApkFile);
		appContext = MyApplication.getCurrentApp().getApplicationContext();
	}

	public void setApkUrl(String apkUrl) {
		this.apkUrl = apkUrl;
	}

	@Override
	public String getRemoteUrl() {
		return apkUrl;
	}

	@Override
	public void completeDownload(long size, Response rsp,Date date) {
		showCompleteNotification();
	}

	public FileNetWorkerHandler getFileNetWorkerHandler() {
		return new FileNetWorkerHandler() {

			@Override
			public void handleData(Response rsp) {
				NotificationManager nManager = (NotificationManager) appContext
						.getSystemService(Context.NOTIFICATION_SERVICE);
				nManager.cancel(DOWNLOAD_NOTIFICATIONID);
				if (rsp.getStatus() == RemoteService.RES_OK) {
					showCompleteNotification();
				} else {
					Toast.makeText(appContext, R.string.network_error,
							Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void progressUpdate(Object[] vals) {
				if (vals.length == 2) {
					long cur_time = System.currentTimeMillis();

					long current = (Long) vals[0];
					long all = (Long) vals[1];
					if (pre_time == 0 || cur_time - pre_time > 1000) {
						pre_time = cur_time;
						int progressInt = (int) (current * 100 / all);
						String progress = progressInt + "%";
						String status = appContext.getResources().getString(
								R.string.process_downloading_apk);
						showDownloadingNotification(status, progress,
								progressInt);
					}
				}
			}

		};
	}

	/**
	 * 显示提醒通知
	 * 
	 * @param text
	 * @param index
	 */
	private void showDownloadingNotification(String title, String text,
			int progress) {

		if (notification == null) {
			notification = new Notification();
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			RemoteViews contentView = new RemoteViews(
					appContext.getPackageName(), R.layout.download_notify);
			PendingIntent empty = PendingIntent.getActivity(appContext, 0,
					new Intent(), PendingIntent.FLAG_CANCEL_CURRENT);
			notification.contentView = contentView;
			notification.contentIntent = empty;
			notification.icon = R.drawable.msg_downloading;
		}
		notification.contentView.setTextViewText(R.id.notifyTitle, title);
		notification.contentView.setProgressBar(R.id.notifyProgressBar, 100,
				progress, false);
		notification.contentView.setTextViewText(R.id.notifyTextView, text);
		NotificationManager nManager = (NotificationManager) appContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		nManager.notify(DOWNLOAD_NOTIFICATIONID, notification);
	}

	/**
	 * 显示下载完成并通知安装
	 * 
	 * @param text
	 * @param index
	 */
	private void showCompleteNotification() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(tempApkFile),
				"application/vnd.android.package-archive");
		appContext.startActivity(intent);
	}
}
