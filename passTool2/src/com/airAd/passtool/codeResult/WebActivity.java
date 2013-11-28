package com.airAd.passtool.codeResult;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.airAd.passtool.R;
import com.airAd.passtool.TicketList;
import com.airAd.passtool.service.ApkDownloadService;
import com.airAd.passtool.worker.FileNetWorker;
import com.google.zxing.client.android.CaptureActivity;

public class WebActivity extends Activity {

	private WebView webView;
	public static final int RESULT = 10;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.web);
		webView = (WebView) findViewById(R.id.webView);
		initWebView();

		if (getIntent() != null
				&& getIntent().getStringExtra(CaptureActivity.FLAG) != null) {
			String url = getIntent().getStringExtra(CaptureActivity.FLAG);
			if (url.contains("?")) {
				url += "&from=airPass";
				// url += "?from=airPass";
			} else {
				url += "?from=airPass";
			}
			webView.loadUrl(url);
		} else {
			finish();
		}
	}

	public void initWebView() {
		webView.setHorizontalScrollBarEnabled(true);
		webView.setVerticalScrollBarEnabled(false);

		WebSettings webSettings = webView.getSettings();
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		webSettings.setSupportMultipleWindows(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setAllowFileAccess(true);

		webView.setClickable(true);
		webView.setLongClickable(true);
		ChromeClient client = new ChromeClient(
				(ProgressBar) findViewById(R.id.progressBar));
		webView.setWebChromeClient(client);
		webView.setWebViewClient(new MyWebViewClient());

	}

	public void back(View view) {
		back();
	}

	private void back() {
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				back();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private class MyWebViewClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.contains(".pkpass")) {
				// Intent data = new Intent();
				// data.putExtra(CaptureActivity.FLAG, url);
				// setResult(RESULT_OK, data);

				// 发送广播 通知列表页面下载URL;
				Intent data = new Intent();
				data.setAction(TicketList.DOWNLOAD_TICKETS_BROADCAST);
				data.putExtra(TicketList.URL, url);
				WebActivity.this.sendBroadcast(data);

				finish();
				return true;
			}
			if (url.contains("/app/")) {
				ApkDownloadService apkDownloadService = new ApkDownloadService();
				apkDownloadService.setApkUrl(url);
				FileNetWorker fileNetWorker = new FileNetWorker(
						WebActivity.this);
				fileNetWorker.request(apkDownloadService,
						apkDownloadService.getFileNetWorkerHandler());
				return true;
			} else {
				return super.shouldOverrideUrlLoading(view, url);
			}
		}
	}
}
