package com.airAd.passtool;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.Coupon;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.ui.ticket.Appearance;
import com.airAd.passtool.ui.ticket.CouponAppearance;
import com.airAd.passtool.util.ConfigUtil;
import com.airAd.passtool.util.FileUtil;
import com.airAd.passtool.util.StringUtil;
import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;

/**
 * 票据盒子
 * 
 * @author pengfan
 * 
 */
public class TicketPanel extends BaseFragmentActivity {

	private ViewPager viewPager;
	public TicketDataSource dataSource;
	private List<Ticket> dataList = new ArrayList<Ticket>();
	private List<String> ticketIds;
	private TicketAdapter adapter;
	private TextView pagerIndicator;

	private PopupWindow popWindow;// 弹出框
	private View popContentView;// pop内容页
	private LayoutInflater mLayoutInflater;// 填充布局器
	private View shareBtn;// 分享按钮
	private Button closePopBtn;// 关闭pop窗口
	private View mask;// 遮罩层
	private View mWeixinShare;
	private View mMessageShare;// 短信分享
	private View mWeixinShareToFriend;//微信分享到朋友

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.ticket_panel);
		mLayoutInflater = LayoutInflater.from(this);

		viewPager = (ViewPager) findViewById(R.id.viewPager);
		pagerIndicator = (TextView) findViewById(R.id.pagerIndicator);
		shareBtn = findViewById(R.id.shareBtn);
		mask = findViewById(R.id.mask);
		String id_array_str = null;
		if (getIntent() != null) {
			if (getIntent().getDataString() != null) {
				// 外部调用通道
				id_array_str = getIntent().getDataString();
			} else {
				// 由通知栏点击进入通道
				id_array_str = getIntent().getStringExtra(Config.SERIAL);
			}
		}
		if (id_array_str != null) {
			ticketIds = new ArrayList<String>();
			for (String str : id_array_str.split(",")) {
				ticketIds.add(str);
			}
		} else {
			ticketIds = (List<String>) MyApplication.getCurrentApp().pop();
		}
		adapter = new TicketAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
		dataSource = new TicketDataSource(this);
		try {
			dataSource.open();
		} catch (SQLException e) {
			e.printStackTrace();
			finish();
		}
		dataList = dataSource.query(ticketIds);
		if (dataList.isEmpty() || dataList.size() == 1) {
			pagerIndicator.setVisibility(View.GONE);
		} else {
			pagerIndicator.setText("1 of " + dataList.size());
			viewPager.setOnPageChangeListener(new OnPageChangeListener() {

				@Override
				public void onPageSelected(int index) {
					pagerIndicator.setText((index + 1) + " of "
							+ dataList.size());
				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {

				}

				@Override
				public void onPageScrollStateChanged(int arg0) {

				}
			});
		}
		setPopWindow(viewPager, mLayoutInflater);
		shareBtn.setOnClickListener(new ShareBtnClick());
	}

	private void setPopWindow(View view, LayoutInflater inflater) {
		popContentView = inflater.inflate(R.layout.share_pop, null);
		popWindow = new PopupWindow(popContentView,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		popWindow.setFocusable(true);
		// popWindow.setBackgroundDrawable(new BitmapDrawable());
		popWindow.setAnimationStyle(R.style.PopupAnimation);
		closePopBtn = (Button) popContentView.findViewById(R.id.closePopWindow);
		mWeixinShare = popContentView.findViewById(R.id.share_weixin);
		mMessageShare = popContentView.findViewById(R.id.share_message);
		mWeixinShareToFriend = popContentView.findViewById(R.id.share_weixin_friend);
		closePopBtn.setOnClickListener(new ClosePop());
		mWeixinShare.setOnClickListener(new ShareWeixin());
		mWeixinShareToFriend.setOnClickListener(new ShareWeixinToFriend());
		mMessageShare.setOnClickListener(new ShareMessage());
	}

	/**
	 * 短信分享
	 * 
	 * @author Administrator
	 * 
	 */
	private final class ShareMessage implements OnClickListener {
		@Override
		public void onClick(View v) {
			Ticket ticket = dataList.get(viewPager.getCurrentItem());
			if (ticket == null) {
				return;
			}
			sendSMS(ticket.getShareUrl());
			closePopMask();
		}
	}// end inner class

	/**
	 * 发送短信
	 * 
	 * @param smsBody
	 */
	private void sendSMS(String smsBody) {
		String content = getString(R.string.message_content);
		content += smsBody;
		Uri smsToUri = Uri.parse("smsto:");
		Intent intent = new Intent(Intent.ACTION_SENDTO, smsToUri);
		intent.putExtra("sms_body", content);
		TicketPanel.this.startActivity(intent);
	}
	
	/**
	 * 分享微信给朋友
	 * 
	 * @author panyi
	 * 
	 */
	private final class ShareWeixinToFriend implements OnClickListener {
		@Override
		public void onClick(View v) {
			Ticket ticket = dataList.get(viewPager.getCurrentItem());
			if (ticket == null) {
				return;
			}
			if (StringUtil.isBlank(ticket.getShareUrl()))
				return;
			shareWeixin(ticket.getShareUrl(),true);
			closePopMask();
		}
	}// end inner class

	/**
	 * 分享微信
	 * 
	 * @author panyi
	 * 
	 */
	private final class ShareWeixin implements OnClickListener {
		@Override
		public void onClick(View v) {
			Ticket ticket = dataList.get(viewPager.getCurrentItem());
			if (ticket == null) {
				return;
			}
			if (StringUtil.isBlank(ticket.getShareUrl()))
				return;
			// Toast.makeText(TicketPanel.this,
			// getResources().getString(R.string.weixin_share_url) +
			// ticket.getShareUrl(),
			// Toast.LENGTH_SHORT).show();
			shareToWeixin(ticket.getShareUrl());
			closePopMask();
		}
	}// end inner class

	private void shareWeixin(String webUrl, boolean isToFriend) {
		WXWebpageObject webpage = new WXWebpageObject();
		IWXAPI api = WXAPIFactory.createWXAPI(this, Config.WENXIN_APP_ID);
		if (!api.isWXAppInstalled()) {
			Toast.makeText(TicketPanel.this, R.string.weinxin_nostall,
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (!api.isWXAppSupportAPI()) {
			Toast.makeText(TicketPanel.this, R.string.weixin_nosupport_friend,
					Toast.LENGTH_SHORT).show();
			return;
		}

		webpage.webpageUrl = webUrl;
		WXMediaMessage msg = new WXMediaMessage(webpage);
		msg.title = getString(R.string.message_content);
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("webpage");
		req.message = msg;
		if(isToFriend){
			req.scene = SendMessageToWX.Req.WXSceneSession;
		}else{
			req.scene = SendMessageToWX.Req.WXSceneTimeline;
		}
		if (!api.sendReq(req))
			Toast.makeText(TicketPanel.this, R.string.weinxin_share_fail,
					Toast.LENGTH_SHORT).show();
	}

	private void shareToWeixin(String webUrl) {
		WXWebpageObject webpage = new WXWebpageObject();
		IWXAPI api = WXAPIFactory.createWXAPI(this, Config.WENXIN_APP_ID);
		if (!api.isWXAppInstalled()) {
			Toast.makeText(TicketPanel.this, R.string.weinxin_nostall,
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (!api.isWXAppSupportAPI()) {
			Toast.makeText(TicketPanel.this, R.string.weixin_nosupport_friend,
					Toast.LENGTH_SHORT).show();
			return;
		}

		webpage.webpageUrl = webUrl;
		WXMediaMessage msg = new WXMediaMessage(webpage);
		msg.title = webUrl;
		// Bitmap thumb = BitmapFactory.decodeResource(getResources(),
		// R.drawable.icon);
		// msg.thumbData = FileUtil.bmpToByteArray(thumb, true);
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = buildTransaction("webpage");
		req.message = msg;
		req.scene = SendMessageToWX.Req.WXSceneTimeline;

		if (api.sendReq(req)) {
			// Toast.makeText(TicketPanel.this, R.string.weinxin_share_success,
			// Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(TicketPanel.this, R.string.weinxin_share_fail,
					Toast.LENGTH_SHORT).show();
		}
	}

	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis())
				: type + System.currentTimeMillis();
	}

	private final class ClosePop implements OnClickListener {
		@Override
		public void onClick(View v) {
			closePopMask();
		}
	}// end inner class

	/**
	 * 关闭遮罩层
	 */
	private void closePopMask() {
		if (popWindow.isShowing()) {
			popWindow.dismiss();
		}
		mask.setVisibility(View.GONE);
	}

	private final class ShareBtnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			popWindow.showAtLocation(viewPager, Gravity.BOTTOM, 0, 0);
			mask.setVisibility(View.VISIBLE);
		}
	}// end inner class

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (resultCode) {
		case TicketDetail.RES_REFRESH: {
			try {
				dataSource.open();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			dataList = dataSource.query(ticketIds);
			adapter.notifyDataSetChanged();
			setResult(TicketList.RES_REFRESH);
			dataSource.close();
			break;
		}
		case TicketDetail.RES_DELETE: {
			setResult(TicketList.RES_REFRESH);
			// Intent it=new Intent();
			// it.setAction(ConfigUtil.REFRESH_SERVICE_ACTION_NAME);
			// this.sendBroadcast(it);
			finish();
			break;
		}
		}
	}

	public void back(View v) {
		finish();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		try {
			dataSource.open();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		dataSource.close();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		dataSource.close();
		super.onDestroy();
	}

	private class TicketAdapter extends FragmentStatePagerAdapter {

		public TicketAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int pos) {
			Ticket ticket = dataList.get(pos);
			Appearance appearance = null;
			if (ticket instanceof Coupon) {
				CouponAppearance cApp = new CouponAppearance();
				cApp.setTicket(ticket);
				appearance = cApp;
			}
			return appearance;
		}

		public int getItemPosition(Object item) {
			Appearance appearance = (Appearance) item;
			int position = dataList.indexOf(appearance.getTicket());
			if (position >= 0) {
				return position;
			} else {
				return POSITION_NONE;
			}
		}

		@Override
		public int getCount() {
			return dataList.size();
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
}// end class
