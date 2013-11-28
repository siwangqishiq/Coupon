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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.airAd.passtool.R;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192, 128, 64 };
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;

    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int frameColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    private Paint linePaint;
    public final int LINE_LENGTH=30;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint();
        linePaint = new Paint();
        linePaint.setColor(Color.rgb(17, 170, 204));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        frameColor = resources.getColor(R.color.viewfinder_frame);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        resultPointColor = resources.getColor(R.color.possible_result_points);
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<ResultPoint>(5);
        lastPossibleResultPoints = null;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = CameraManager.get().getFramingRect();
        if (frame == null) {
            return;
        }
        int width = this.getWidth();
        int height = this.getHeight();
        
        int cube_width = width/4;
        int cube_height = height/4;
        
        int mid = getHeight()/2;
        int pad = cube_width;
        
        int point1_x = cube_width,point1_y = mid-pad;
        int point2_x = width - cube_width,point2_y = mid-pad;
        int point3_x = cube_width,point3_y = mid+pad;
        int point4_x = width-cube_width,point4_y = point3_y;
        
        canvas.drawLine(point1_x, point1_y, point1_x+LINE_LENGTH, point1_y, linePaint);// 画线  
        canvas.drawLine(point1_x, point1_y, point1_x, point1_y+LINE_LENGTH, linePaint);// 画线
        
        canvas.drawLine(point2_x, point2_y, point2_x-LINE_LENGTH, point2_y, linePaint);// 画线  
        canvas.drawLine(point2_x, point2_y, point2_x, point2_y+LINE_LENGTH, linePaint);// 画线
        
        canvas.drawLine(point3_x, point3_y, point3_x, point3_y-LINE_LENGTH, linePaint);// 画线  
        canvas.drawLine(point3_x, point3_y, point3_x+LINE_LENGTH, point3_y, linePaint);// 画线
        
        canvas.drawLine(point4_x, point4_y, point4_x, point4_y-LINE_LENGTH, linePaint);// 画线  
        canvas.drawLine(point4_x, point4_y, point4_x-LINE_LENGTH, point4_y, linePaint);// 画线  
        
        
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {

            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.setColor(laserColor);
            paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//            int middle = frame.height() / 2 + frame.top;
            int middle = getHeight()/2;
            canvas.drawRect(cube_width + 2, middle - 1, width-cube_width - 1, middle + 2, paint);

            Rect previewFrame = CameraManager.get().getFramingRectInPreview();
            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<ResultPoint>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frame.left + (int) (point.getX() * scaleX), frame.top + (int) (point.getY() * scaleY), 6.0f,
                                paint);
                    }
                }
            }
            if (currentLast != null) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                synchronized (currentLast) {
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frame.left + (int) (point.getX() * scaleX), frame.top + (int) (point.getY() * scaleY), 3.0f,
                                paint);
                    }
                }
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    public void drawViewfinder() {
        resultBitmap = null;
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (point) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
