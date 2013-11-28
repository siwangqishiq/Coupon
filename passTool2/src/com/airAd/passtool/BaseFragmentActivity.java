package com.airAd.passtool;

import android.support.v4.app.FragmentActivity;

import com.umeng.analytics.MobclickAgent;

public class BaseFragmentActivity extends FragmentActivity {

    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
