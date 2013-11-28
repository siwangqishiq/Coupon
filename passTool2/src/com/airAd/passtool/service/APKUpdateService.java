package com.airAd.passtool.service;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.util.net.BasicService;
import com.airAd.passtool.util.net.Response;

/**
 * 下载票据的接口
 * @author pengfan
 *
 */
public class APKUpdateService extends BasicService {

    
    private int verisonCode = -1;

    public APKUpdateService(Context context) {
        super(TYPE_GET);
        try {
            verisonCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleResponse(HttpResponse httpResponse, Response rsp) {
        String oRsp = null;
        try {
            oRsp = EntityUtils.toString(httpResponse.getEntity());
            if ("".equals(oRsp.trim())) {
                return;
            }
            JSONObject jRsp = new JSONObject(oRsp);
            if (!jRsp.isNull("response")) {
                JSONObject rspObj = jRsp.getJSONObject("response");
                String changelog = rspObj.getString("changelog");
                String updateurl = rspObj.getString("updateurl");
                String version = rspObj.getString("versioncode");
                rsp.setData(changelog + "$" + updateurl + "$" + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂时返回一个
     */
    @Override
    public String getRemoteUrl() {
        return Config.UPDATE_HOST + "/passapi/apk/airpass/update/" + verisonCode;
    }
}
