package com.airAd.passtool;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingActivity extends PreferenceActivity {
	private SharedPreferences sp;
	private Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.settings);
		init();
	}
	
	private void init(){
		context=this;
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		sp.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener(){
			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				if("setLocService".equalsIgnoreCase(key)){
					boolean isStartService=sharedPreferences.getBoolean(key, false);
					if(isStartService){//开启服务
						startService(new Intent(context, LocService.class));
					}else{//关闭服务
						stopService(new Intent(context, LocService.class));
					}
				}
			}//end 
		});
	}
}
