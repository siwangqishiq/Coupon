package com.airAd.passtool.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.airAd.passtool.R;

/**
 * View相关工具
 * @author pengfan
 *
 */
public class ViewUtil {

    public static void hiddenView(View view, boolean anim) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
            if (anim) {
                Animation showAnim = AnimationUtils.loadAnimation(view.getContext(), R.anim.zoom_exit);
                view.startAnimation(showAnim);
            }
        }
    }

    public static void showView(View view, boolean anim) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
            if (anim) {
                Animation showAnim = AnimationUtils.loadAnimation(view.getContext(), R.anim.zoom_enter);
                view.startAnimation(showAnim);
            }
        }
    }

    public static void invisableView(View view, boolean anim) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.INVISIBLE);
            if (anim) {
                Animation showAnim = AnimationUtils.loadAnimation(view.getContext(), R.anim.zoom_exit);
                view.startAnimation(showAnim);
            }
        }
    }

}
