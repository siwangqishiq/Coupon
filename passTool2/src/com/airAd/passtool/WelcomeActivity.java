package com.airAd.passtool;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.airAd.passtool.util.ImageUtil;

/**
 * 欢迎界面
 * 
 * @author panyi
 * 
 */
public class WelcomeActivity extends Activity {
	protected Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Intent it = new Intent(WelcomeActivity.this,TicketList.class);
			WelcomeActivity.this.startActivity(it);
			WelcomeActivity.this.finish();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//				WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		handler.sendEmptyMessageDelayed(1, 2500);
	}
}
