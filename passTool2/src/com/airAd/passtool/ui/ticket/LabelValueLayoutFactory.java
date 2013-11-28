package com.airAd.passtool.ui.ticket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airAd.passtool.R;
import com.airAd.passtool.data.model.Field;

/**
 * 中间文字的Layout
 * @author pengfan
 *
 */
public class LabelValueLayoutFactory {

    private Context context;

    /**
     * @param context
     */
    public LabelValueLayoutFactory(Context context) {
        this.context = context;
    }

    /**
     * @param field
     * @param isRight
     * @return
     */
    /**
     * @param field
     * @param isRight
     * @return
     */
    /**
     * @param field
     * @param isRight
     * @param foregroundColor
     * @param labelColor
     * @return
     */
    /**
     * @param field
     * @param isRight
     * @param foregroundColor
     * @param labelColor
     * @return
     */
    public View getInstance(Field field, boolean isRight, int foregroundColor, int labelColor) {
        View v = LayoutInflater.from(context).inflate(R.layout.label_value_layout, null);
        LinearLayout baseLayout = (LinearLayout) v.findViewById(R.id.baseLayout);
        TextView labelView = (TextView) v.findViewById(R.id.labelView);
        TextView valueView = (TextView) v.findViewById(R.id.valueView);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) baseLayout.getLayoutParams();
        if (isRight) {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            layoutParams.rightMargin = layoutParams.leftMargin;
            layoutParams.leftMargin = 0;
        } else {
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }
        baseLayout.setLayoutParams(layoutParams);
        labelView.setText(field.getLabel());
        valueView.setText(field.getValue());
        labelView.setTextColor(labelColor);
        valueView.setTextColor(foregroundColor);
        return v;
    }
}
