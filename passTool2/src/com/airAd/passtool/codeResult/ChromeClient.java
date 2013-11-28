package com.airAd.passtool.codeResult;

import android.view.View;
import android.webkit.GeolocationPermissions.Callback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

/*
 * Android 2.2+  
 */
public class ChromeClient extends WebChromeClient {

    private ProgressBar progressBar;

    public ChromeClient(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (progressBar != null) {
            if (progressBar.getVisibility() != View.VISIBLE && newProgress != 100) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setProgress(newProgress);
            }

        }
    }
}
