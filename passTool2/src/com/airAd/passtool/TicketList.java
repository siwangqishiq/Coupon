package com.airAd.passtool;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.airAd.passtool.TicketDownloadingService.DownloadStatus;
import com.airAd.passtool.codeResult.WebActivity;
import com.airAd.passtool.data.BuildInPassManager;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.service.APKUpdateService;
import com.airAd.passtool.service.ApkDownloadService;
import com.airAd.passtool.ui.ticket.ColorProgress;
import com.airAd.passtool.ui.ticket.ListItem;
import com.airAd.passtool.util.ConfigUtil;
import com.airAd.passtool.util.FileUtil;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.ViewUtil;
import com.airAd.passtool.util.net.Response;
import com.airAd.passtool.worker.FileNetWorker;
import com.airAd.passtool.worker.NetWorker;
import com.airAd.passtool.worker.NetWorkerHandler;
import com.google.zxing.client.android.CaptureActivity;

/**
 * 优惠券列表
 * 
 * @author p
 * 
 */
public class TicketList extends BaseActivity {
	public static final String URL="download_url";
	public static final String DOWNLOAD_TICKETS_BROADCAST = "download_new_tickets";

	public static final int DOWNLOAD_TICKET = 400;
	public static final int RES_REFRESH = 500;
	public static final int RES_OPENURL = 501;
	public static final int ALREADY_DOWNLOAD = 200;

	private ListView listView;
	private NetWorker netWorker;
	private TicketDataSource dataSource;
	private List<Ticket> dataList = new ArrayList<Ticket>();
	private TicketAdapter adapter;
	private BroadcastReceiver receiver;

	private Handler handler;
	private Messenger serviceMessager;
	private ServiceConnection conn;
	private StatusBarMananger statusManager;
	private boolean alreayUpdate;
	private String apkUpdateStr;
	private LinearLayout emptyBox;
	protected LayoutInflater mInflater;
	protected DownloadNewTickets downloadReceiver;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.ticket_list);
		mInflater = LayoutInflater.from(this);
		// 注册广播接受者
		IntentFilter downloadFilter = new IntentFilter();
		downloadFilter.addAction(DOWNLOAD_TICKETS_BROADCAST);
		downloadReceiver = new DownloadNewTickets();
		registerReceiver(downloadReceiver, downloadFilter);

		if (FileUtil.isSDCardMounted()) {
			BuildInPassManager buildInPassManager = new BuildInPassManager(this);
			buildInPassManager.addPassFromAssert();// 载入自动打包的优惠券
			emptyBox = (LinearLayout) findViewById(R.id.empty_box);
			listView = (ListView) findViewById(R.id.listView);
			adapter = new TicketAdapter();
			listView.setAdapter(adapter);
			netWorker = new NetWorker(this);
			statusManager = new StatusBarMananger(findViewById(R.id.status_bar));
			initServiceConnection();
			dataSource = new TicketDataSource(this);
			try {
				dataSource.open();
			} catch (SQLException e) {
				e.printStackTrace();
				finish();
			}
			dataList = dataSource.query(null);

			if (dataList.size() > 0) {
				listView.setVisibility(View.VISIBLE);
				emptyBox.setVisibility(View.GONE);
			} else {
				listView.setVisibility(View.GONE);
				emptyBox.setVisibility(View.VISIBLE);
			}

			listView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
						int pos, long arg3) {
					List<String> ticketIds = new ArrayList<String>();
					ticketIds.add(dataList.get(pos).getTicketIdentifier()
							.getSerialNumber());
					MyApplication.getCurrentApp().push(ticketIds);
					startActivityForResult(new Intent(TicketList.this,
							TicketPanel.class), 0);
				}
			});
			receiver = new RefreshReceiver();
			dataSource.close();
			startService(new Intent(this, LocService.class));
			IntentFilter filter = new IntentFilter(
					ConfigUtil.REFRESH_SERVICE_ACTION_NAME);
			registerReceiver(receiver, filter);
			autoUpdate();
		}
	}

	/**
	 * 扫描二维码 返回值
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case DOWNLOAD_TICKET:
			if (resultCode == RESULT_OK) {
				String url = data.getStringExtra(CaptureActivity.FLAG);
				downloadTicket(url);
			} else if (resultCode == RES_OPENURL) {
				data.setClass(this, WebActivity.class);
				startActivityForResult(data, requestCode);
			}
			break;
		default:
		}

	}

	// 开启扫描界面
	public void scan(View view) {
		startActivityForResult((new Intent(this, CaptureActivity.class)),
				DOWNLOAD_TICKET);
	}

	// 打开引导页
	public void openPage(View view) {
		Intent intent = new Intent(this, WebActivity.class);
		intent.putExtra(CaptureActivity.FLAG, Config.HOME_NAV_URL);
		startActivityForResult(intent, DOWNLOAD_TICKET);
	}

	@Override
	protected void onStart() {
		if (checkSDcard()) {
			String url = getIntent().getDataString();
			String preLoadUrl = getResources().getString(
					R.string.preset_address);
			LogUtil.i(TicketList.class, "create url:" + url);
			LogUtil.i(TicketList.class, "primary_field_height :"
					+ getResources().getDimension(R.dimen.primary_field_height));
			// 扫描进入的情形
			if (url != null && getIntent().getFlags() != ALREADY_DOWNLOAD) {
				downloadTicket(url);
				getIntent().setFlags(ALREADY_DOWNLOAD);
			} else if (preLoadUrl != null
					&& !"".equals(preLoadUrl)
					&& !MyApplication.getCurrentApp().getConfig()
							.isPreSetUrlLoaded(preLoadUrl)) {
				downloadTicket(preLoadUrl);
				MyApplication.getCurrentApp().getConfig()
						.setPreSetUrl(preLoadUrl);
			}
		}

		super.onStart();
	}

	public void autoUpdate() {
		netWorker.request(new APKUpdateService(this), new NetWorkerHandler() {

			@Override
			public void handleData(Response rsp) {
				String updateStr = (String) rsp.getData();
				if (updateStr != null) {
					if (!statusManager.isShow()) {
						alertUpdateDialog(updateStr);
					} else {
						TicketList.this.apkUpdateStr = updateStr;
					}
				}
			}

			@Override
			public void progressUpdate(long current, long allLength) {

			}
		});

	}

	private void initServiceConnection() {

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				boolean isInit = msg.arg2 == TicketDownloadingService.IN_STATUS_IS_INIT;
				switch (msg.what) {
				case TicketDownloadingService.OUT_SERVICE_RUNNING_NORMAL: {
					DownloadStatus status = (DownloadStatus) msg.obj;
					statusManager.showStatus(status, isInit);
					break;
				}
				case TicketDownloadingService.OUT_SERVICE_RUNNING_PROGRESS: {
					int porgress = msg.arg1;
					DownloadStatus status = (DownloadStatus) msg.obj;
					statusManager.showProgressBar(porgress, status, isInit);
					break;
				}
				case TicketDownloadingService.OUT_SERVICE_END: {
					DownloadStatus status = (DownloadStatus) msg.obj;
					statusManager.showResult(status);
					break;
				}
				case TicketDownloadingService.OUT_SERVICE_ALEADY_RUNNING: {
					break;
				}
				case TicketDownloadingService.OUT_SERVICE_NOT_RUNNING: {
					DownloadStatus status = (DownloadStatus) msg.obj;
					// 如果是初始状态的返回
					if (isInit && status.isNeedRedownload()) {
						statusManager.showResult(status);
					} else {
						statusManager.hidden(false);
						// UpdateService启动时会删除无效的临时数据，
						// 如果下载未完成会将临时文件夹删除，导致插入失败。
						// 故将启动更新服务放到此处进行
						if (!alreayUpdate) {
							startService(new Intent(TicketList.this,
									UpdateService.class));
							alreayUpdate = true;
						}
					}
					break;
				}
				}
			}
		};

		conn = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder binder) {
				serviceMessager = new Messenger(binder);
				Message msg = Message.obtain();
				msg.what = TicketDownloadingService.IN_BIND_MESSENGER;
				msg.obj = new Messenger(handler);
				try {
					serviceMessager.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			public void onServiceDisconnected(ComponentName className) {
				serviceMessager = null;
			}
		};
	}

	private void downloadApk(String apkUrl) {
		ApkDownloadService apkDownloadService = new ApkDownloadService();
		apkDownloadService.setApkUrl(apkUrl);
		FileNetWorker fileNetWorker = new FileNetWorker(this);
		fileNetWorker.request(apkDownloadService,
				apkDownloadService.getFileNetWorkerHandler());
	}

	private void alertUpdateDialog(String updateStr) {
		final String[] msg = updateStr.split("\\$");
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		String version = sp.getString("version", "");
		if (updateStr != null && !version.equals(msg[2])) {
			AlertDialog.Builder builder = new Builder(TicketList.this);
			builder.setCancelable(true);
			builder.setMessage(msg[0]);
			builder.setTitle(R.string.dialog_update);
			builder.setPositiveButton(R.string.dialog_update_confirm,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							downloadApk(msg[1]);
						}
					});
			builder.setNegativeButton(R.string.dialog_update_cancel,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SharedPreferences sp = PreferenceManager
									.getDefaultSharedPreferences(TicketList.this);
							sp.edit().putString("version", msg[2]).commit();
							dialog.dismiss();
						}
					});
			builder.show();
		}
		updateStr = null;
	}

	public boolean checkSDcard() {
		boolean res = FileUtil.isSDCardMounted();
		if (!res) {
			AlertDialog.Builder builder = new Builder(TicketList.this);
			builder.setCancelable(false);
			builder.setMessage(R.string.no_sd_error);
			builder.setTitle(R.string.dialog_title);
			builder.setPositiveButton(R.string.dialog_confirm,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							finish();
						}
					});
			builder.show();
		}
		return res;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (FileUtil.isSDCardMounted()) {
			Intent intent = new Intent(this, TicketDownloadingService.class);
			bindService(intent, conn, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (FileUtil.isSDCardMounted()) {
			unbindService(conn);
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
		if (FileUtil.isSDCardMounted()) {
			netWorker.cancelAll();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (FileUtil.isSDCardMounted()) {
			unregisterReceiver(receiver);
			dataSource.close();
			LogUtil.i(TicketList.class, "dataSource.close");
		}

		if (downloadReceiver != null) {
			this.unregisterReceiver(downloadReceiver);
		}
	}

	private void downloadTicket(String url) {
		Intent intent = new Intent(this, TicketDownloadingService.class);
		intent.putExtra("url", url);
		startService(intent);
		stopService(new Intent(this, UpdateService.class));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.clear_cache: {
			downloadTicket("http://192.168.1.247:8860/passbook/down/1260.pkpass");
			break;

		}
		case R.id.scan_qrCode:
			startActivityForResult((new Intent(this, CaptureActivity.class)),
					DOWNLOAD_TICKET);
			return true;
		case R.id.remote_test:
			startService(new Intent(this, UpdateService.class));
			break;
		case R.id.stopUpdateService:
			Intent intent = new Intent(this, UpdateService.class);
			intent.putExtra(UpdateService.FLAG, UpdateService.STOP);
			startService(intent);
			break;
		case R.id.startService:
			startService(new Intent(this, LocService.class));
			break;
		case R.id.stopService:
			stopService(new Intent(this, LocService.class));
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	/**
	 * 
	 * @author pengfan
	 * 
	 */
	private class RefreshReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				dataSource.open();
				dataList = dataSource.query(null);

				if (dataList.size() > 0) {
					listView.setVisibility(View.VISIBLE);
					emptyBox.setVisibility(View.GONE);
				} else {
					listView.setVisibility(View.GONE);
					emptyBox.setVisibility(View.VISIBLE);
				}

				adapter.notifyDataSetChanged();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				dataSource.close();
			}
			statusManager.hidden(false);

			// 检查是否有未进行的版本更新
			if (apkUpdateStr != null) {
				alertUpdateDialog(apkUpdateStr);
				apkUpdateStr = null;
			}
		}
	}

	private final class TicketAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return dataList.size();
		}

		@Override
		public Object getItem(int position) {
			return dataList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Ticket ticket = dataList.get(position);
			ListItem item = (ListItem) convertView;
			if (item == null) {
				item = new ListItem(TicketList.this);
			}
			item.setTicket(ticket);
			return item;
		}
	}

	/**
	 * 设置按钮
	 * 
	 * @param view
	 */
	public void settingButton(View view) {
		Intent intent = new Intent();
		intent.setClass(this, SettingActivity.class);
		startActivity(intent);
	}

	private final class DownloadNewTickets extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String url = intent.getExtras().getString(URL);
			downloadTicket(url);
		}
	}// end inner class

	/**
	 * 状态栏管理类
	 * 
	 * @author pengfan
	 * 
	 */
	private final class StatusBarMananger {
		private View statusBarLayout;

		private TextView statusView;
		private ImageButton btnYes;
		private ImageButton btnNo;
		private View statusColumn;

		private ColorProgress progressBar;
		private TextView progressBarStatusView;
		private ImageView progressValue;

		public StatusBarMananger(View statusBarLayout) {
			this.statusBarLayout = statusBarLayout;
			statusColumn = statusBarLayout.findViewById(R.id.status_column);
			statusView = (TextView) statusBarLayout
					.findViewById(R.id.status_prompt);
			btnYes = (ImageButton) statusBarLayout
					.findViewById(R.id.status_btn_yes);
			btnNo = (ImageButton) statusBarLayout
					.findViewById(R.id.status_btn_no);
			
			progressBar = (ColorProgress) statusBarLayout
					.findViewById(R.id.downloading_progressbar);
			progressBarStatusView = (TextView) statusBarLayout
					.findViewById(R.id.status_bar_prompt);
		}

		public void showStatus(final DownloadStatus status, boolean anim) {
			ViewUtil.showView(statusBarLayout, anim);
			ViewUtil.showView(statusColumn, false);
			statusView.setText(status.getPrompt());
			ViewUtil.hiddenView(progressBar, false);
			ViewUtil.hiddenView(progressBarStatusView, false);
			if (status.isNeedRedownload()) {
				// 居左对齐
				LayoutParams params = (RelativeLayout.LayoutParams) statusColumn
						.getLayoutParams();
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
						RelativeLayout.TRUE);
				params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
				statusColumn.setLayoutParams(params);
				ViewUtil.showView(btnYes, false);
				btnYes.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						downloadTicket(status.getTicketURL());
					}
				});
				ViewUtil.showView(btnNo, false);
				btnNo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// 通知service，清除错误状态
						Message msg = Message.obtain();
						msg.what = TicketDownloadingService.IN_CANCAL_REDOWNLOAD;
						try {
							serviceMessager.send(msg);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						hidden(false);
					}
				});
			} else {
				LayoutParams params = (RelativeLayout.LayoutParams) statusColumn
						.getLayoutParams();
				params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
				params.addRule(RelativeLayout.CENTER_IN_PARENT,
						RelativeLayout.TRUE);
				statusColumn.setLayoutParams(params);
				ViewUtil.hiddenView(btnYes, false);
				ViewUtil.hiddenView(btnNo, false);
			}
		}

		public void showProgressBar(int progress, DownloadStatus status,
				boolean anim) {
			ViewUtil.showView(statusBarLayout, anim);
			ViewUtil.hiddenView(statusColumn, false);
			ViewUtil.showView(progressBar, false);
			progressBar.setProgress(progress);
			ViewUtil.showView(progressBarStatusView, false);
			progressBarStatusView.setText(status.getPrompt());
		}

		public void showResult(DownloadStatus status) {
			showStatus(status, false);
			if (!status.isNeedRedownload()) {
				statusBarLayout.postDelayed(new Runnable() {
					@Override
					public void run() {
						hidden(true);
					}
				}, 1000);
			}
		}

		public void hidden(boolean anim) {
			ViewUtil.hiddenView(statusBarLayout, anim);
		}

		public boolean isShow() {
			return statusBarLayout.getVisibility() == View.VISIBLE;
		}
	}
}// end class
