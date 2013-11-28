package com.airAd.passtool;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class NoificationDialog extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String warn = intent.getStringExtra("warn");
        setContentView(R.layout.custom_dialog_activity);

        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(warn);
    }
}
