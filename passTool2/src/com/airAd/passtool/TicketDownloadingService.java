package com.airAd.passtool;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.TicketIdentifier;
import com.airAd.passtool.service.TicketRefreshService;
import com.airAd.passtool.service.TicketService;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.net.FileService;
import com.airAd.passtool.util.net.RemoteService;
import com.airAd.passtool.util.net.Response;
import com.airAd.passtool.worker.FileNetWorker;
import com.airAd.passtool.worker.FileNetWorkerHandler;

/**
 * 用于和前台交互的ticket下载服务，
 * @author pengfan
 *
 */
public class TicketDownloadingService extends Service {

    private static final String TAG = "TicketDownloadingService";

    public static final int IN_BIND_MESSENGER = 100;
    public static final int IN_DOWNLOAD_TICKET = 101;
    public static final int IN_CANCAL_REDOWNLOAD = 102;

    public static final int IN_STATUS_IS_INIT = 1;
    public static final int IN_STATUS_IS_NOT_INIT = 0;

    public static final int OUT_SERVICE_NOT_RUNNING = 200;
    public static final int OUT_SERVICE_ALEADY_RUNNING = 201;
    public static final int OUT_SERVICE_RUNNING_NORMAL = 202;
    public static final int OUT_SERVICE_RUNNING_PROGRESS = 203;
    public static final int OUT_SERVICE_END = 204;

    //用于接收来自activity的消息
    final Messenger inMessenger = new Messenger(new IncomingHandler());
    // 用于向activity发送消息
    private Messenger outMessenger;
    // 保存当前status的状态
    private int status = OUT_SERVICE_NOT_RUNNING;
    // 如果是进度条，保存当前进度。
    private int progress = 0;
    // 保存下载失败的url,以便重试
    private String downloadURL = "";
    // 当前状态提示文字
    private DownloadStatus downloadStatus = new DownloadStatus();
    private FileNetWorker fileNetWorker;

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.i(this.getClass(), "onBind");
        return inMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.i(this.getClass(), "onCreate");
        fileNetWorker = new FileNetWorker(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String url = intent.getStringExtra("url");
            TicketIdentifier ti = (TicketIdentifier) intent.getSerializableExtra("TicketIdentifier");
            LogUtil.i(getClass(), "url:" + url + ",ti:" + ti + ",status:" + status);
            if (status != OUT_SERVICE_NOT_RUNNING) {
                sendMsgToActivity(false, OUT_SERVICE_ALEADY_RUNNING);
                //全局提示正在下载
                Toast.makeText(getApplicationContext(), R.string.process_busy, Toast.LENGTH_SHORT).show();
                return super.onStartCommand(intent, flags, startId);
            }

            if (url != null) {
                TicketService ts = new TicketService(this);
                downloadURL = url;
                ts.setURL(url);
                downloadTicket(ts);
            } else if (ti != null) {
                TicketRefreshService ts = new TicketRefreshService(this);
                downloadURL = "";
                ts.setTicketIdentifier(ti);
                downloadTicket(ts);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    private class IncomingHandler extends Handler {
        //处理来自activity的消息
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case IN_BIND_MESSENGER : {
                    outMessenger = (Messenger) msg.obj;
                    //同时把当前状态发送
                    sendMsgToActivity(true);
                    LogUtil.i(TicketDownloadingService.class, "IN_BIND_MESSENGER:" + msg.obj + "," + status + "," + downloadStatus);
                    break;
                }
                case IN_CANCAL_REDOWNLOAD : {
                    downloadStatus = new DownloadStatus();
                    break;
                }
            }//end switch
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 将当前状态发给activity
     */
    private void sendMsgToActivity(boolean init) {
        sendMsgToActivity(init, status);
    }

    /**
     * 将当前状态发给activity
     */
    private void sendMsgToActivity(boolean init, int status) {
        if (outMessenger != null) {
            Message backMsg = Message.obtain();
            backMsg.what = status;
            if (init) {
                backMsg.arg2 = IN_STATUS_IS_INIT;
            } else {
                backMsg.arg2 = IN_STATUS_IS_NOT_INIT;
            }
            switch (status) {
                case OUT_SERVICE_RUNNING_NORMAL : {
                    backMsg.obj = downloadStatus;
                    break;
                }
                case OUT_SERVICE_RUNNING_PROGRESS : {
                    backMsg.arg1 = progress;
                    backMsg.obj = downloadStatus;
                    break;
                }
                case OUT_SERVICE_END : {
                    backMsg.arg1 = progress;
                    backMsg.obj = downloadStatus;
                    break;
                }
                case OUT_SERVICE_NOT_RUNNING : {
                    backMsg.obj = downloadStatus;
                    break;
                }
            }
            try {
                outMessenger.send(backMsg);
            } catch (android.os.RemoteException e1) {
                Log.e(getClass().getName(), "Exception sending message", e1);
            }
        }
    }

    private void downloadTicket(FileService service) {
        fillDownloadStatus(R.string.process_networker);
        status = OUT_SERVICE_RUNNING_NORMAL;
        sendMsgToActivity(false);
        fileNetWorker.request(service, new FileNetWorkerHandler() {

            @Override
            public void handleData(Response data) {
                status = OUT_SERVICE_END;
                if (data.getStatus() == RemoteService.RES_OK) {
                    int res = 0;
                    if (data.getData() != null) {
                        res = (Integer) data.getData();
                        progress = res;
                    }
                    switch (res) {
                        case TicketDataSource.INSERT_SUCCESS : {
                            fillDownloadStatus(R.string.insert_success);
                            break;
                        }
                        case TicketDataSource.UPDATE_SUCCESS : {
                            fillDownloadStatus(R.string.update_success);
                            break;
                        }
                        case TicketDataSource.PERSIST_FAIL : {
                            fillDownloadStatus(R.string.persist_error);
                            setRedownload();
                            break;
                        }
                        case TicketDataSource.REGISTER_FAIL : {
                            fillDownloadStatus(R.string.register_error);
                            setRedownload();
                            break;
                        }
                        case TicketDataSource.FORMAT_FAIL : {
                            fillDownloadStatus(R.string.format_error);
                            setRedownload();
                            break;
                        }
                    }
                } else if (data.getStatus() == TicketRefreshService.NoNeedToRefresh) {
                    fillDownloadStatus(R.string.update_noneed);
                } else {
                    fillDownloadStatus(R.string.network_error);
                    setRedownload();
                }
                sendMsgToActivity(false);
                status = OUT_SERVICE_NOT_RUNNING;
                progress = 0;
            }

            @Override
            public void progressUpdate(Object[] vals) {
                if (vals.length == 2) {
                    long current = (Long) vals[0];
                    long all = (Long) vals[1];
                    progress = (int) (current * 100 / all);
                    status = OUT_SERVICE_RUNNING_PROGRESS;
                    fillDownloadStatus(R.string.process_downloading);
                    sendMsgToActivity(false);
                }
            }
        });
    }

    private void fillDownloadStatus(int str_res) {
        downloadStatus = new DownloadStatus();
        downloadStatus.setPrompt(getResources().getString(str_res));
    }

    private void setRedownload() {
        //不是更新请求
        if (downloadURL != "") {
            downloadStatus.setNeedRedownload(true);
            downloadStatus.setTicketURL(downloadURL);
        }
    }
    public static class DownloadStatus {
        private String prompt;//提示文字
        private boolean needRedownload;//需要重新下载
        private String ticketURL;

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public boolean isNeedRedownload() {
            return needRedownload;
        }

        public void setNeedRedownload(boolean needRedownload) {
            this.needRedownload = needRedownload;
        }

        public String getTicketURL() {
            return ticketURL;
        }

        public void setTicketURL(String ticketURL) {
            this.ticketURL = ticketURL;
        }

        public String toString() {
            return prompt + "," + needRedownload + "," + ticketURL;
        }

    }
}
