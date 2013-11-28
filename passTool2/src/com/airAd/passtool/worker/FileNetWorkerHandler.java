package com.airAd.passtool.worker;

import com.airAd.passtool.util.net.Response;

public interface FileNetWorkerHandler {
    void handleData(Response rsp);

    void progressUpdate(Object[] vals);

}
