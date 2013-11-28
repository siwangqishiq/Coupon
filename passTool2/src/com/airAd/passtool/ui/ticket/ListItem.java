package com.airAd.passtool.ui.ticket;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airAd.passtool.R;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.util.ConfigUtil;

/**
 * 条目项
 * 
 * @author pengfan
 * 
 */
public class ListItem extends LinearLayout {

	private RelativeLayout header_field;
	private TextView headerLogo;
	private TextView headerLabel;
	private TextView headerValue;
	private ImageView headerImg;
	private TextView headerTitle;

	private Ticket ticket;

	public ListItem(Context context) {
		super(context);
		initLayout(context);
	}

	public void initLayout(Context context) {
		LayoutInflater.from(context).inflate(R.layout.list_item, this);
		headerLogo = (TextView) findViewById(R.id.header_text);
		headerLabel = (TextView) findViewById(R.id.header_label);
		headerValue = (TextView) findViewById(R.id.header_value);
		headerTitle = (TextView) findViewById(R.id.header_text2);
		headerImg = (ImageView) findViewById(R.id.header_img);
		header_field = (RelativeLayout) findViewById(R.id.header_field);
	}

	public Ticket getTicket() {
		return ticket;
	}

	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
		fillData();
	}

	/**
	 * 填充数据，绘制图形
	 */
	private void fillData() {
		headerLogo.setText(ticket.getLogoText());
		if (ticket.getPrimaryField() != null
				&& ticket.getPrimaryField().size() > 0) {
			headerTitle.setText(ticket.getPrimaryField().get(0).getValue());
		}

		Bitmap logo = ConfigUtil.getLogo(ticket.getFolderName(), getContext());
		if (logo != null) {
			headerImg.setImageBitmap(logo);
		}
		// 清空原有数据 修改会保留之前余额数据的BUG ---add panyi
		headerLabel.setText("");
		headerValue.setText("");
		// end ----
		if (ticket.getHeadFields() != null && ticket.getHeadFields().size() > 0) {
			headerLabel.setText(ticket.getHeadFields().get(0).getLabel());
			headerValue.setText(ticket.getHeadFields().get(0).getValue());
		}

		// header_field.setBackgroundColor(ticket.getBackgroundColor());
		// int foregroundColor = ticket.getForegroundColor();
		// headerLogo.setTextColor(foregroundColor);
		// headerValue.setTextColor(foregroundColor);
		// headerLabel.setTextColor(ticket.getLabelColor());
	}
}
