package com.airAd.passtool.ui.ticket;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.R;
import com.airAd.passtool.TicketDetail;
import com.airAd.passtool.TicketDownloadingService;
import com.airAd.passtool.UpdateService;
import com.airAd.passtool.TicketDownloadingService.DownloadStatus;
import com.airAd.passtool.animation.Rotate3dAnimation;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.Coupon;
import com.airAd.passtool.data.model.Field;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.util.ConfigUtil;
import com.airAd.passtool.util.ImageUtil;
import com.airAd.passtool.util.ViewUtil;

/**
 * 事件类型（活动门票）
 * 
 * @author
 * 
 */

public class CouponAppearance extends Appearance {

	private ViewGroup mContainer;

	private RelativeLayout allParent;// 正面

	private TextView headerLogo;
	private TextView headerLabel;
	private TextView headerValue;
	private ImageView headerImg;

	private ImageView primaryImgView;
	private TextView primaryLabel;
	private TextView primaryValue;

	private LinearLayout subLayout;
	private LabelValueLayoutFactory factory;

	private RelativeLayout barCode_field;
	private ImageView barCode;
	private TextView barCodeAlt;
	private Button couponDetailBtn;

	// ----
	private RelativeLayout backParent;// 反面
	private TicketDataSource dataSource;
	private LinearLayout detailFieldLayout;
	private CheckBox autoUpdateCheck;
	private int res = RES_NORMAL;
	private Handler handler;
	private Messenger serviceMessager;
	private ServiceConnection conn;
	private boolean netWorkUnuseable;
	public static final int RES_REFRESH = 1001;
	public static final int RES_NORMAL = 1000;
	public static final int RES_DELETE = 1002;
	private Button deleteBtn;
	private ImageButton reloadBtn;
	private Button reverseBtn;
	private ScrollView mainScroll;
	private LinearLayout loadStatus;
	private ImageView loadbg;
	private ImageView loadArrow;
	private TextView refreshText;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.coupon, container, false);
		mContainer = (ViewGroup) v.findViewById(R.id.container);

		headerLogo = (TextView) v.findViewById(R.id.header_text);
		headerLabel = (TextView) v.findViewById(R.id.header_label);
		headerValue = (TextView) v.findViewById(R.id.header_value);
		headerImg = (ImageView) v.findViewById(R.id.header_img);

		primaryImgView = (ImageView) v.findViewById(R.id.primary_img);
		primaryLabel = (TextView) v.findViewById(R.id.primary_label);
		primaryValue = (TextView) v.findViewById(R.id.primary_value);

		subLayout = (LinearLayout) v.findViewById(R.id.sub_field);
		factory = new LabelValueLayoutFactory(getActivity());

		barCode = (ImageView) v.findViewById(R.id.barcode);
		barCodeAlt = (TextView) v.findViewById(R.id.barcode_alt);
		barCode_field = (RelativeLayout) v.findViewById(R.id.barcode_field);

		allParent = (RelativeLayout) v.findViewById(R.id.all_parent);
		ImageView info = (ImageView) v.findViewById(R.id.infoBtn);
		couponDetailBtn = (Button) v.findViewById(R.id.coupon_detail_btn);

		dataSource = new TicketDataSource(getActivity());
		try {
			dataSource.open();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// 背面
		detailFieldLayout = (LinearLayout) v.findViewById(R.id.detail_field);
		autoUpdateCheck = (CheckBox) v.findViewById(R.id.autoUpdateView);
		backParent = (RelativeLayout) v.findViewById(R.id.detail_parent);
		deleteBtn = (Button) v.findViewById(R.id.delete_btn);
		reloadBtn = (ImageButton) v.findViewById(R.id.reload);
		reverseBtn = (Button) v.findViewById(R.id.reverseBtn);
		mainScroll = (ScrollView) v.findViewById(R.id.main_scroll);
		loadStatus = (LinearLayout) v.findViewById(R.id.load_status);
		loadArrow = (ImageView) v.findViewById(R.id.update_arrow);
		refreshText = (TextView) v.findViewById(R.id.refresh_status);
		loadbg = (ImageView) v.findViewById(R.id.update_bg);

		initServiceConnection();
		switchInfoImageByColor(info);
		setInfoOnClickListener(info);
		return v;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		loadData();
	}

	private void loadData() {
		setForgroundColor(getCoupon().getForegroundColor());
		setLabelColor(getCoupon().getLabelColor());

		headerLogo.setText(getCoupon().getLogoText());
		Bitmap logo = ConfigUtil.getLogo(getCoupon().getFolderName(),
				getActivity());
		if (logo != null) {
			headerImg.setImageBitmap(logo);
		}

		Bitmap strip = ConfigUtil.getStrip(getCoupon().getFolderName(),
				getActivity());
		if (strip != null) {
			primaryImgView.setImageBitmap(strip);
		}

		Bitmap qrCode = ConfigUtil.getQRCode(getCoupon().getFolderName());
		if (qrCode != null && ticket.getBarcode() != null) {
			barCode.setImageBitmap(qrCode);
			String text = ticket.getBarcode().getAltText();
			// 为空的时候也不显示
			if (text != null && !"".equals(text)) {
				barCodeAlt.setText(ticket.getBarcode().getAltText());
			} else {
				barCodeAlt.setVisibility(View.GONE);
			}
		} else {
			barCode_field.setVisibility(View.GONE);
		}

		if (getCoupon().getHeadFields() != null
				&& getCoupon().getHeadFields().size() > 0) {
			headerLabel.setText(getCoupon().getHeadFields().get(0).getLabel());
			headerValue.setText(getCoupon().getHeadFields().get(0).getValue());
		}

		/**
		 * 在有背景图的情况下不显示label
		 */
		if (getCoupon().getPrimaryField() != null
				&& getCoupon().getPrimaryField().size() > 0) {
			// 如果包含横幅则用白色
			if (strip != null) {
				primaryLabel.setTextColor(getResources().getColor(
						R.color.primary_color));
				primaryValue.setTextColor(getResources().getColor(
						R.color.primary_color));
			}
			primaryLabel.setText(getCoupon().getPrimaryField().get(0)
					.getLabel());
			primaryValue.setText(getCoupon().getPrimaryField().get(0)
					.getValue());
		}
		List<Field> fieldList = new ArrayList<Field>();
		if (getCoupon().getSecondaryFields() != null) {
			fieldList.addAll(getCoupon().getSecondaryFields());
		}
		if (getCoupon().getAuxiliaryFields() != null) {
			fieldList.addAll(getCoupon().getAuxiliaryFields());
		}
		// 最大数量为4
		int maxSize = fieldList.size() > 4 ? 4 : fieldList.size();
		for (int i = 0; i < maxSize; i++) {
			Field field = fieldList.get(i);
			boolean isRight = false;
			if (i + 1 == maxSize && i > 0) {
				isRight = true;
			}
			View v = factory.getInstance(field, isRight, getCoupon()
					.getForegroundColor(), getCoupon().getLabelColor());
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
					LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
			v.setLayoutParams(params);
			subLayout.addView(v);
		}

		// 设置正面背景色
		allParent.setBackgroundColor(getCoupon().getBackgroundColor());

		// 反面
		Coupon coupon = getCoupon();
		backParent.setBackgroundColor(coupon.getBackgroundColor());
		autoUpdateCheck.setChecked(coupon.getTicketIdentifier().isAutoUpdate());

		if (ImageUtil.detectByYUV(coupon.getBackgroundColor()) == 1) {
			reloadBtn.setImageResource(R.drawable.reload_deep_bg);
			reverseBtn.setTextColor(Color.BLACK);

			loadArrow.setImageResource(R.drawable.update_arrow);
			refreshText.setTextColor(Color.BLACK);
			loadbg.setImageResource(R.drawable.update_bg_deep);
		}

		detailFieldLayout.removeAllViews();
		List<Field> list = coupon.getBackFields();
		if (list != null && !list.isEmpty()) {
			for (int i = 0; i < list.size(); i++) {
				detailFieldLayout.addView(generateTextFieldItem(list.get(i)));
			}
		} else {
			detailFieldLayout.setVisibility(View.GONE);
		}
	}

	private void initServiceConnection() {
		handler = new Handler() {
			private StatusBarMananger statusManager = new StatusBarMananger();

			@Override
			public void handleMessage(Message msg) {
				int res = msg.arg1;
				boolean isInit = msg.arg2 == TicketDownloadingService.IN_STATUS_IS_INIT;
				// 如果网络不可用则修改标志位。
				if (isInit
						&& msg.what != TicketDownloadingService.OUT_SERVICE_NOT_RUNNING) {
					netWorkUnuseable = true;
				}
				if (!netWorkUnuseable && !getActivity().isFinishing()) {
					switch (msg.what) {
					case TicketDownloadingService.OUT_SERVICE_RUNNING_NORMAL:
					case TicketDownloadingService.OUT_SERVICE_RUNNING_PROGRESS: {
						DownloadStatus status = (DownloadStatus) msg.obj;
						statusManager.showStatus(status.getPrompt());
						break;
					}
					case TicketDownloadingService.OUT_SERVICE_END: {
						if (res == TicketDataSource.UPDATE_SUCCESS) {
							List<String> list = new ArrayList<String>();
							list.add(ticket.getTicketIdentifier()
									.getSerialNumber());
							ticket = dataSource.query(list).get(0);
							loadData();
							// TicketDetail.this.res = RES_REFRESH;
							DownloadStatus status = (DownloadStatus) msg.obj;
							statusManager.hiddenDelay(status.getPrompt());
						} else {
							statusManager.hiddenDelay(getResources().getString(
									R.string.update_error));
						}
						break;
					}
					}
				}
				if (netWorkUnuseable
						&& msg.what == TicketDownloadingService.OUT_SERVICE_END) {
					netWorkUnuseable = false;
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

	/**
	 * 根据背景色的亮度值切换图片
	 */
	private void switchInfoImageByColor(ImageView infoView) {
		// if (ImageUtil.detectByYUV(getCoupon().getBackgroundColor()) == 1) {
		// infoView.setImageResource(R.drawable.i_deep);
		// }

	}

	@Override
	public void onDestroy() {
		dataSource.close();
		super.onDestroy();
	}

	/**
	 * 增加info的点击
	 * 
	 * @param v
	 */
	private void setInfoOnClickListener(View v) {
		v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MyApplication.getCurrentApp().push(getCoupon());
				startActivityForResult(new Intent(getActivity(),
						TicketDetail.class), 1);
			}
		});

		// 设置是否自动更新
		autoUpdateCheck
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						dataSource.setAutoUpdate(getCoupon()
								.getTicketIdentifier().getSerialNumber(),
								autoUpdateCheck.isChecked());
						// System.out.println("自动更新-->"
						// + autoUpdateCheck.isChecked());
					}
				});

		deleteBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.dialog_delete_title)
						.setPositiveButton(R.string.dialog_confirm,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										if (dataSource.delete(ticket
												.getTicketIdentifier()
												.getSerialNumber())) {
											getActivity().setResult(RES_DELETE);
											getActivity().finish();
											Toast.makeText(getActivity(),
													R.string.delete_success,
													Toast.LENGTH_LONG).show();
										} else {
											Toast.makeText(getActivity(),
													R.string.delete_fail,
													Toast.LENGTH_LONG).show();
										}
									}
								})
						.setNegativeButton(R.string.dialog_cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										dialog.dismiss();
									}
								}).show();
			}
		});

		reloadBtn.setOnClickListener(new RefreshClick());
		couponDetailBtn.setOnClickListener(new RotateToBack());
		reverseBtn.setOnClickListener(new RotateToFore());
	}

	/**
	 * 点击刷新按钮
	 * 
	 * @author Administrator
	 * 
	 */
	private final class RefreshClick implements OnClickListener {
		@Override
		public void onClick(View view) {

			if (!netWorkUnuseable) {
				TranslateAnimation moveToDown = new TranslateAnimation(0, 0, 0,
						loadStatus.getHeight());
				moveToDown.setFillEnabled(true);
				moveToDown.setFillAfter(true);
				moveToDown.setDuration(600);
				moveToDown.setAnimationListener(new AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {
					}

					@Override
					public void onAnimationRepeat(Animation animation) {
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						Animation rotate = AnimationUtils.loadAnimation(
								getActivity(), R.anim.rotate_animation);
						loadArrow.setAnimation(rotate);
					}
				});
				mainScroll.scrollTo(0, 1);
				mainScroll.setAnimation(moveToDown);
				Intent intent = new Intent(getActivity(),
						TicketDownloadingService.class);
				intent.putExtra("TicketIdentifier",
						ticket.getTicketIdentifier());
				getActivity().startService(intent);
			} else {
				Toast.makeText(getActivity(), R.string.process_busy,
						Toast.LENGTH_SHORT).show();
			}

		}
	}// end inner class

	private final class RotateToBack implements OnClickListener {
		@Override
		public void onClick(View v) {
			applyRotation(0, 90f, R.id.all_parent);
		}
	}// end inner class

	private final class RotateToFore implements OnClickListener {
		@Override
		public void onClick(View v) {
			applyRotation(0, 90f, R.id.detail_parent);
		}
	}// end inner class

	private void applyRotation(float start, float end, final int viewId) {
		final float centerX = mContainer.getWidth() / 2.0f;
		final float centerY = mContainer.getHeight() / 2.0f;
		Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX,
				centerY, 200f, true);
		rotation.setDuration(500);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation arg0) {
				mContainer.post(new Runnable() {
					@Override
					public void run() {
						if (viewId == R.id.all_parent) {
							allParent.setVisibility(View.GONE);
							backParent.setVisibility(View.VISIBLE);
						} else if (viewId == R.id.detail_parent) {
							backParent.setVisibility(View.GONE);
							allParent.setVisibility(View.VISIBLE);
						}
						Rotate3dAnimation rotatiomAnimation = new Rotate3dAnimation(
								-90, 0, centerX, centerY, 200.0f, false);
						rotatiomAnimation.setDuration(500);
						rotatiomAnimation
								.setInterpolator(new DecelerateInterpolator());
						mContainer.startAnimation(rotatiomAnimation);
					}
				});

			}

			@Override
			public void onAnimationRepeat(Animation arg0) {
			}

			@Override
			public void onAnimationStart(Animation arg0) {
			}
		});
		mContainer.startAnimation(rotation);
	}

	/**
	 * 生成text field项
	 */
	private View generateTextFieldItem(Field field) {
		View v = getActivity().getLayoutInflater().inflate(R.layout.back_text,
				null);
		TextView labelView = (TextView) v.findViewById(R.id.back_label);
		TextView valueView = (TextView) v.findViewById(R.id.back_value);
		labelView.setText(field.getLabel());
		String value = field.getValue();
		if (field.getLabel().contains("微博地址")) {
			value = value
					.replaceAll(
							"(?<=[^\\w@])(http://)?(([\\w-]+\\.)+(com|net|cn).*?)(?=\\s+|$)",
							"<a href='http://$2'>$2</a>");
			valueView.setText(Html.fromHtml(value));
			valueView.setMovementMethod(LinkMovementMethod.getInstance());
			return v;
		}else if(field.getLabel().contains("更多优惠券")){
			getCoupon().setShareUrl(value);
		}
		// 替换所有超链接为a标签
		value = value
				.replaceAll(
						"(?<=[^\\w@])(http://)?(([\\w-]+\\.)+(com|net|cn).*?)(?=\\s+|$)",
						"<a href='http://$2'>$2</a>");
		value = value.replaceAll("[\r]?\n", "<br/>");
		// 替换所有电话为a标签
		value = value.replaceAll("((\\+\\d{2}-?)?(\\d{3}-)?\\d{7,})",
				"<a href='tel:$1'>$1</a>");
		value = value.replaceAll("(\\d{3}-\\d{3}-\\d{4})",
				"<a href='tel:$1'>$1</a>");

		valueView.setText(Html.fromHtml(value));
		valueView.setMovementMethod(LinkMovementMethod.getInstance());
		return v;
	}

	@Override
	public void setForgroundColor(int color) {
		headerLogo.setTextColor(color);
		headerValue.setTextColor(color);
		primaryValue.setTextColor(color);
		primaryLabel.setTextColor(color);

	}

	@Override
	public void setLabelColor(int color) {
		headerLabel.setTextColor(color);
	}

	protected Coupon getCoupon() {
		return (Coupon) ticket;
	}

	@Override
	public void onResume() {
		Intent intent = new Intent(getActivity(),
				TicketDownloadingService.class);
		getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);
		super.onResume();
	}

	@Override
	public void onPause() {
		getActivity().unbindService(conn);
		super.onPause();
	}

	private final class StatusBarMananger {
		public void showStatus(String status) {
			refreshText.setText(status);
		}

		/**
		 * 两秒之后 向上收起
		 * 
		 * @param status
		 */
		public void hiddenDelay(String status) {
			refreshText.setText(status);
			loadArrow.clearAnimation();
			TranslateAnimation moveToUp = new TranslateAnimation(0, 0,
					loadStatus.getHeight(), 0);
			moveToUp.setDuration(600);
			moveToUp.setStartOffset(1500);
			mainScroll.setAnimation(moveToUp);
		}
	}// end inner class

}
