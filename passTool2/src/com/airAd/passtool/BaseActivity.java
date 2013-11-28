package com.airAd.passtool;

import android.app.Activity;

import com.umeng.analytics.MobclickAgent;

/**
 * 所有界面的父类，主要用于增加数据分析项
 * @author pengfan
 *
 */
public class BaseActivity extends Activity {

    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
    
}//end class
