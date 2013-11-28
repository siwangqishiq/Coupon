/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.airAd.passtool.R;
import com.airAd.passtool.TicketList;
import com.airAd.passtool.codeResult.WebActivity;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.util.LogUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.result.ParsedResultType;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
@TargetApi(8)
public final class CaptureActivity extends Activity implements
		SurfaceHolder.Callback {

	public static final String FLAG = "scanRes";

	private static final int SHARE_ID = Menu.FIRST;
	private static final int HISTORY_ID = Menu.FIRST + 1;
	private static final int SETTINGS_ID = Menu.FIRST + 2;
	private static final int HELP_ID = Menu.FIRST + 3;
	private static final int ABOUT_ID = Menu.FIRST + 4;

	private static final long INTENT_RESULT_DURATION = 1L;
	private static final long BULK_MODE_SCAN_DELAY_MS = 1L;

	private static final String PACKAGE_NAME = "com.laundrylocker.barcodescanner";
	private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
	private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
	private static final String ZXING_URL = "http://zxing.appspot.com/scan";
	private static final String RETURN_CODE_PLACEHOLDER = "{CODE}";
	private static final String RETURN_URL_PARAM = "ret";

	private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;

	private View backBtn;
	private View webBtn;
	protected Context mContext;

	static {
		DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
		DISPLAYABLE_METADATA_TYPES
				.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
		DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
	}

	private enum Source {
		NATIVE_APP_INTENT, PRODUCT_SEARCH_LINK, ZXING_LINK, NONE
	}

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private View resultView;
	private Result lastResult;
	private boolean hasSurface;
	private boolean copyToClipboard;
	private Source source;
	private String sourceUrl;
	private String returnUrlTemplate;
	private Collection<BarcodeFormat> decodeFormats;
	private String characterSet;
	private String versionName;
	private InactivityTimer inactivityTimer;
	private BeepManager beepManager;

	private final DialogInterface.OnClickListener aboutListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialogInterface, int i) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(getString(R.string.zxing_url)));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
		}
	};

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mContext = this;
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);

		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		resultView = findViewById(R.id.result_view);
		handler = null;
		lastResult = null;
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);

		backBtn = findViewById(R.id.back_btn);
		backBtn.setOnClickListener(new BackOnClick());
		webBtn = findViewById(R.id.go_web);
		webBtn.setOnClickListener(new GoWebOnClick());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	private final class GoWebOnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(CaptureActivity.this, WebActivity.class);
			intent.putExtra(CaptureActivity.FLAG, Config.HOME_NAV_URL);
			startActivityForResult(intent, TicketList.DOWNLOAD_TICKET);
			CaptureActivity.this.finish();
		}
	}// end inner class

	private final class BackOnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			CaptureActivity.this.finish();
			System.gc();
		}
	}// end inner class

	@Override
	protected void onResume() {
		super.onResume();
		resetStatusView();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		String dataString = intent == null ? null : intent.getDataString();
		if (intent != null && action != null) {
			if (action.equals(Intents.Scan.ACTION)) {
				// Scan the formats the intent requested, and return the result
				// to the calling activity.
				source = Source.NATIVE_APP_INTENT;
				decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
				if (intent.hasExtra(Intents.Scan.WIDTH)
						&& intent.hasExtra(Intents.Scan.HEIGHT)) {
					int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
					int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
					if (width > 0 && height > 0) {
						CameraManager.get().setManualFramingRect(height, width);
					}
				}

			} else if (dataString != null
					&& dataString.contains(PRODUCT_SEARCH_URL_PREFIX)
					&& dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {
				// Scan only products and send the result to mobile Product
				// Search.
				source = Source.PRODUCT_SEARCH_LINK;
				sourceUrl = dataString;
				decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;
			} else if (dataString != null && dataString.startsWith(ZXING_URL)) {
				// Scan formats requested in query string (all formats if none
				// specified).
				// If a return URL is specified, send the results there.
				// Otherwise, handle it ourselves.
				source = Source.ZXING_LINK;
				sourceUrl = dataString;
				Uri inputUri = Uri.parse(sourceUrl);
				returnUrlTemplate = inputUri
						.getQueryParameter(RETURN_URL_PARAM);
				decodeFormats = DecodeFormatManager
						.parseDecodeFormats(inputUri);
			} else {
				// Scan all formats and handle the results ourselves (launched
				// from Home).
				source = Source.NONE;
				decodeFormats = null;
			}
			characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
		} else {
			source = Source.NONE;
			decodeFormats = null;
			characterSet = null;
		}
		beepManager.updatePrefs();
		inactivityTimer.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (source == Source.NATIVE_APP_INTENT) {
				setResult(RESULT_CANCELED);
				finish();
				return true;
			} else if ((source == Source.NONE || source == Source.ZXING_LINK)
					&& lastResult != null) {
				resetStatusView();
				if (handler != null) {
					handler.sendEmptyMessage(R.id.restart_preview);
				}
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_FOCUS
				|| keyCode == KeyEvent.KEYCODE_CAMERA) {
			// Handle these events so they don't launch the Camera app
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult, Bitmap barcode) {
		inactivityTimer.onActivity();
		lastResult = rawResult;
		ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
				this, rawResult);
		if (barcode == null) {
			finish();
		} else {
			beepManager.playBeepSoundAndVibrate();
			drawResultPoints(barcode, rawResult);
			if (resultHandler.getResult().getType() == ParsedResultType.URI) {
				String url = resultHandler.getResult().getDisplayResult();
				Intent data = new Intent();
				data.putExtra(FLAG, url);
				if (url.endsWith(".pkpass")) {
					setResult(RESULT_OK, data);
				} else {
					setResult(TicketList.RES_OPENURL, data);
				}
				finish();
			} else {

			}
		}
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	private void drawResultPoints(Bitmap barcode, Result rawResult) {
		ResultPoint[] points = rawResult.getResultPoints();
		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_image_border));
			paint.setStrokeWidth(3.0f);
			paint.setStyle(Paint.Style.STROKE);
			Rect border = new Rect(2, 2, barcode.getWidth() - 2,
					barcode.getHeight() - 2);
			canvas.drawRect(border, paint);

			paint.setColor(getResources().getColor(R.color.result_points));
			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				drawLine(canvas, paint, points[0], points[1]);
			} else if (points.length == 4
					&& (rawResult.getBarcodeFormat()
							.equals(BarcodeFormat.UPC_A) || rawResult
							.getBarcodeFormat().equals(BarcodeFormat.EAN_13))) {
				// Hacky special case -- draw two lines, for the barcode and
				// metadata
				drawLine(canvas, paint, points[0], points[1]);
				drawLine(canvas, paint, points[2], points[3]);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					canvas.drawPoint(point.getX(), point.getY(), paint);
				}
			}
		}
	}

	private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
			ResultPoint b) {
		canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
	}

	// Briefly show the contents of the barcode, then handle the result outside
	// Barcode Scanner.
	private void handleDecodeExternally(Result rawResult,
			ResultHandler resultHandler, Bitmap barcode) {
		if (source != Source.NATIVE_APP_INTENT) {
			viewfinderView.drawResultBitmap(barcode);

			// Since this message will only be shown for a second, just tell the
			// user what kind of
			// barcode was found (e.g. contact info) rather than the full
			// contents, which they won't
			// have time to read.
		}

		if (copyToClipboard && !resultHandler.areContentsSecure()) {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(resultHandler.getDisplayContents());
		}

		if (source == Source.NATIVE_APP_INTENT) {
			// Hand back whatever action they requested - this can be changed to
			// Intents.Scan.ACTION when
			// the deprecated intent is retired.
			Intent intent = new Intent(getIntent().getAction());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
			intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult
					.getBarcodeFormat().toString());
			byte[] rawBytes = rawResult.getRawBytes();
			if (rawBytes != null && rawBytes.length > 0) {
				intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
			}
			Message message = Message.obtain(handler, R.id.return_scan_result);
			message.obj = intent;
			handler.sendMessage(message);
		} else if (source == Source.PRODUCT_SEARCH_LINK) {
			// Reformulate the URL which triggered us into a query, so that the
			// request goes to the same
			// TLD as the scan URL.
			Message message = Message
					.obtain(handler, R.id.launch_product_query);
			int end = sourceUrl.lastIndexOf("/scan");
			message.obj = sourceUrl.substring(0, end) + "?q="
					+ resultHandler.getDisplayContents().toString()
					+ "&source=zxing";
			handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		} else if (source == Source.ZXING_LINK) {
			// Replace each occurrence of RETURN_CODE_PLACEHOLDER in the
			// returnUrlTemplate
			// with the scanned code. This allows both queries and REST-style
			// URLs to work.
			Message message = Message
					.obtain(handler, R.id.launch_product_query);
			message.obj = returnUrlTemplate.replace(RETURN_CODE_PLACEHOLDER,
					resultHandler.getDisplayContents().toString());
			handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats,
						characterSet);
			}
		} catch (IOException ioe) {
//			LogUtil.w(CaptureActivity.class, ioe.getMessage());
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			LogUtil.e(CaptureActivity.class,
					"Unexpected error initializating camera  " + e.toString());

			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	private void resetStatusView() {
		resultView.setVisibility(View.GONE);
		viewfinderView.setVisibility(View.VISIBLE);
		lastResult = null;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}
}
