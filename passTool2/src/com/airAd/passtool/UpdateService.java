package com.airAd.passtool;

import java.sql.SQLException;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.TicketIdentifier;
import com.airAd.passtool.service.TicketConfirmService;
import com.airAd.passtool.service.TicketRefreshListService;
import com.airAd.passtool.service.TicketRefreshService;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.net.RemoteService;
import com.airAd.passtool.util.net.Response;
import com.airAd.passtool.worker.FileNetWorker;
import com.airAd.passtool.worker.FileNetWorkerHandler;
import com.airAd.passtool.worker.NetWorker;
import com.airAd.passtool.worker.NetWorkerHandler;

/**
 * 定时更新请求
 * @author pengfan
 *
 */
public class UpdateService extends Service {

    public static final int REPEAT = 1000;
    public static final int STOP = 1001;
//    public static final int INTERVAL = 25 * 1000;
    public static final int INTERVAL = 10* 60 * 1000;//  间隔时间
//    public static final int INTERVAL=10*1000;
    public static final String FLAG = "flag";

    private TicketDataSource dataSource;
    private NetWorker netWorker;
    private FileNetWorker fileNetWorker;
    private int requestCount = 0;
    private int requestSum = 0;
    private boolean alarmSet;//是否已经设置过alarm

    @Override
    public void onCreate() {
        super.onCreate();
        dataSource = new TicketDataSource(this);
        netWorker = new NetWorker(this);
        fileNetWorker = new FileNetWorker(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        try {
            switch (intent.getIntExtra(FLAG, 0)) {
                case STOP : {
                    LogUtil.i(UpdateService.class, "stop");
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    PendingIntent sender =
                            PendingIntent.getService(this, 0, new Intent(this, UpdateService.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.cancel(sender);
                    stopSelf();
                    return;
                }
                case REPEAT : {
                    LogUtil.i(UpdateService.class, "repeat");
                    //TODO更新
//                    updateAll();
                    //注册
                    registerAll();
                    break;
                }
                default : {
                    LogUtil.i(UpdateService.class, "setUpAlarm");
                    setUpAlarm();
                    stopSelf();
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
            stopSelf();
        }
    }
    /**
     * 如果有未发出的注册请求，则进行发布。
     */
    public void registerAll()
    {
        try {
            dataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        List<TicketIdentifier> ticketIds = dataSource.queryUnRegisterTicketIdentifiers();
        LogUtil.i(UpdateService.class, "need register list"+ticketIds.size());
        print(ticketIds);
        dataSource.close();
        
        for (TicketIdentifier ti : ticketIds) {
            TicketConfirmService tcs = new TicketConfirmService(this);
            tcs.setTicketIdentifier(ti);
            netWorker.request(tcs, null);
        }
    }

    public void updateAll() {
        final NetWorkerHandler refreshListHandler = new NetWorkerHandler() {

            @Override
            public void handleData(Response data) {
                if (RemoteService.RES_OK == data.getStatus()) {
                    List<String> idList = (List<String>) data.getData();
                    LogUtil.i(UpdateService.class, "listSizess : " + idList.size());
                    print(idList);
                    try {
                        dataSource.open();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        stopSelf();
                    }
                    //dataSource.cleanPassDirectory();//清理原有临时数据
                    List<TicketIdentifier> ticketIds = dataSource.queryTicketIdentifiers(idList);
                    print(ticketIds);
                    dataSource.close();
                    requestSum = ticketIds.size();
                    //TicketRefreshService refreshService = new TicketRefreshService(UpdateService.this);
                    FileNetWorkerHandler refreshHandler = new FileNetWorkerHandler() {
                        @Override
                        public void handleData(Response data) {
                        	 LogUtil.e(UpdateService.class, "返回状态 : " + data.getStatus());
                            //所有请求发送完毕
                            if (++requestCount >= requestSum) {
                                LogUtil.i(UpdateService.class, "update ticket req : " + requestCount + "," + requestSum);
                                stopSelf();
                            } else {
                                LogUtil.i(UpdateService.class, "update ticket req else : " + requestCount + "," + requestSum);
                            }
                        }

                        @Override
                        public void progressUpdate(Object[] vals) {

                        }
                    };
                    
                    for (TicketIdentifier ti : ticketIds) {
                    	TicketRefreshService refreshServiceItem = new TicketRefreshService(UpdateService.this);
                    	refreshServiceItem.setExtern(ti.getSerialNumber());
                    	refreshServiceItem.setTicketIdentifier(ti);
//                        refreshService.setTicketIdentifier(ti);
                        LogUtil.i(UpdateService.class, "请求连接 : " + ti);
                        //fileNetWorker.request(refreshService, refreshHandler);
                        (new FileNetWorker(UpdateService.this)).request(refreshServiceItem, refreshHandler);
                    }//end for
                }
            }

            @Override
            public void progressUpdate(long current, long allLength) {
            	
            }
        };

        netWorker.request(new TicketRefreshListService(UpdateService.this), refreshListHandler);
    }

    private void print(List list) {
        for (int i = 0; i < list.size(); i++) {
            LogUtil.i(UpdateService.class, "list(" + i + "):" + list.get(i).toString());
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void setUpAlarm() {
        if (!alarmSet) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, UpdateService.class);
            intent.putExtra(FLAG, REPEAT);
            PendingIntent sender = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, INTERVAL, sender);
            alarmSet = true;
        }
    }
}
