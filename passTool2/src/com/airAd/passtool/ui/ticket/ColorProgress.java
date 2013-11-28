package com.airAd.passtool.ui.ticket;

import com.airAd.passtool.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

public class ColorProgress extends View {
	private Bitmap mBitmap;
	private Rect srcRect;
	private RectF dstRect;
	private int bitWidth, bitHeight;
	private Path path;
	private Drawable draw;

	@TargetApi(11)
	public ColorProgress(Context context, AttributeSet attrs) {
		super(context, attrs);
		mBitmap = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.pass_loadingbar);
		bitWidth = mBitmap.getWidth();
		bitHeight = mBitmap.getHeight();
		srcRect = new Rect(0, 0, bitWidth, bitHeight);
		dstRect = new RectF();
		path = new Path();
		draw = context.getResources().getDrawable(R.drawable.pass_loadingbar);

		if (Build.VERSION.SDK_INT >= 11) {
			this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
	}

	public void setProgress(int value) {
		if (value < 0)
			value = 0;
		if (value > 100)
			value = 100;
		int originWidth = bitWidth * value / 100;
		int start_x = bitWidth - originWidth;
		srcRect.left = start_x;
		srcRect.right = srcRect.left + originWidth;
		int w = getWidth() * value / 100;
		path.reset();
		dstRect.set(0, 0, w, getHeight());
		path.addRoundRect(dstRect, 15f, 15f, Path.Direction.CCW);
		invalidate();
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		draw.setBounds(0, 0, getWidth(), getHeight());
		canvas.clipPath(path);
		draw.draw(canvas);
	}
}// end class
