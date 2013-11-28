package com.airAd.passtool;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airAd.passtool.TicketDownloadingService.DownloadStatus;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.data.model.Field;
import com.airAd.passtool.data.model.Ticket;
import com.airAd.passtool.util.ImageUtil;
import com.airAd.passtool.util.ViewUtil;

/**
 * 票据盒子
 * @author peng
 *
 */
public class TicketDetail extends BaseActivity {

    private Ticket ticket;
    private TicketDataSource dataSource;

    private LinearLayout detailFieldLayout;
    private CheckBox autoUpdateCheck;
    private int res = RES_NORMAL;

    private Handler handler;
    private Messenger serviceMessager;
    private ServiceConnection conn;
    private boolean netWorkUnuseable;

    public static final int RES_REFRESH = 1001;
    public static final int RES_NORMAL = 1000;
    public static final int RES_DELETE = 1002;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.ticket_detail);

        dataSource = new TicketDataSource(this);
        ticket = (Ticket) MyApplication.getCurrentApp().pop();
        detailFieldLayout = (LinearLayout) findViewById(R.id.detail_field);
        autoUpdateCheck = (CheckBox) findViewById(R.id.autoUpdateView);
        autoUpdateCheck.setChecked(ticket.getTicketIdentifier().isAutoUpdate());
        initServiceConnection(findViewById(R.id.status_bar_t), findViewById(R.id.reload));
        try {
            dataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
            finish();
        }
        loadData();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK : {
                close();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void close() {
        setResult(res);
        //将设定的自动更新标志位持久化
        if (ticket.getTicketIdentifier().isAutoUpdate() != autoUpdateCheck.isChecked()) {
            ticket.getTicketIdentifier().setAutoUpdate(
                    autoUpdateCheck.isChecked() ? TicketDataSource.AUTO_UPDATE_ENABLED : TicketDataSource.AUTO_UPDATE_DISABLED);
            dataSource.setAutoUpdate(ticket.getTicketIdentifier().getSerialNumber(), autoUpdateCheck.isChecked());
        }
        finish();
    }

    public void back(View v) {
        close();
    }

    public void delete(View v) {
        new AlertDialog.Builder(TicketDetail.this).setTitle(R.string.dialog_delete_title)
                .setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (dataSource.delete(ticket.getTicketIdentifier().getSerialNumber())) {
                            setResult(RES_DELETE);
                            finish();
                            Toast.makeText(TicketDetail.this, R.string.delete_success, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(TicketDetail.this, R.string.delete_fail, Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                }).show();

    }

    public void refresh(View v) {
        if (!netWorkUnuseable) {
            Intent intent = new Intent(this, TicketDownloadingService.class);
            intent.putExtra("TicketIdentifier", ticket.getTicketIdentifier());
            startService(intent);
            stopService(new Intent(this, UpdateService.class));
        } else {
            Toast.makeText(getApplicationContext(), R.string.process_busy, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadData() {
        if(ImageUtil.detectByYUV(ticket.getBackgroundColor())==1){
            ((ImageButton)findViewById(R.id.reload)).setImageResource(R.drawable.reload_deep);
            ((TextView)findViewById(R.id.status_bar_prompt)).setTextColor(Color.BLACK);
       }
        detailFieldLayout.removeAllViews();
        List<Field> list = ticket.getBackFields();
        if (list != null && !list.isEmpty()) {
            for (int i = 0; i < list.size(); i++) {
                detailFieldLayout.addView(generateTextFieldItem(list.get(i)));
            }
        } else {
            detailFieldLayout.setVisibility(View.GONE);
        }
        int width = MyApplication.getCurrentApp().getMetrics(this).widthPixels;
        int height = MyApplication.getCurrentApp().getMetrics(this).heightPixels;
        findViewById(R.id.detail_parent).setBackgroundDrawable(ImageUtil.genBackColor(ticket.getBackgroundColor(), width, height));
    }

    /**
     * 生成text field项
     */
    private View generateTextFieldItem(Field field) {
        View v = getLayoutInflater().inflate(R.layout.back_text, null);
        TextView labelView = (TextView) v.findViewById(R.id.back_label);
        TextView valueView = (TextView) v.findViewById(R.id.back_value);
        labelView.setText(field.getLabel());
        String value = field.getValue();
        //替换所有超链接为a标签
        value = value.replaceAll("(?<=[^\\w@])(http://)?(([\\w-]+\\.)+(com|net|cn).*?)(?=\\s+|$)", "<a href='http://$2'>$2</a>");
        value = value.replaceAll("(?<=[^\\w-])([\\w-]+@\\w+\\.(com|net|cn))(?=\\s+|$)", "<a href='mailto:$1'>$1</a>");
        value = value.replaceAll("[\r]?\n", "<br/>");
        //替换所有电话为a标签
        value = value.replaceAll("((\\+\\d{2}-?)?(\\d{3}-)?\\d{7,})", "<a href='tel:$1'>$1</a>");
        value = value.replaceAll("(\\d{3}-\\d{3}-\\d{4})", "<a href='tel:$1'>$1</a>");
        valueView.setText(Html.fromHtml(value));
        valueView.setMovementMethod(LinkMovementMethod.getInstance());
        return v;
    }

    private void initServiceConnection(final View statusBarLayout, final View imageButton) {
        handler = new Handler() {
            private StatusBarMananger statusManager = new StatusBarMananger(statusBarLayout, imageButton);

            @Override
            public void handleMessage(Message msg) {
                int res = msg.arg1;
                boolean isInit = msg.arg2 == TicketDownloadingService.IN_STATUS_IS_INIT;
                //如果网络不可用则修改标志位。
                if (isInit && msg.what != TicketDownloadingService.OUT_SERVICE_NOT_RUNNING) {
                    netWorkUnuseable = true;
                }
                if (!netWorkUnuseable && !isFinishing()) {
                    switch (msg.what) {
                        case TicketDownloadingService.OUT_SERVICE_RUNNING_NORMAL :
                        case TicketDownloadingService.OUT_SERVICE_RUNNING_PROGRESS : {
                            DownloadStatus status = (DownloadStatus) msg.obj;
                            statusManager.showStatus(status.getPrompt(), false);
                            break;
                        }
                        case TicketDownloadingService.OUT_SERVICE_END : {
                            if (res == TicketDataSource.UPDATE_SUCCESS) {
                                List<String> list = new ArrayList<String>();
                                list.add(ticket.getTicketIdentifier().getSerialNumber());
                                ticket = dataSource.query(list).get(0);
                                loadData();
                                TicketDetail.this.res = RES_REFRESH;
                                DownloadStatus status = (DownloadStatus) msg.obj;
                                statusManager.hiddenDelay(status.getPrompt(), false);
                            }
                            else
                            {
                                statusManager.hiddenDelay(getResources().getString(R.string.update_error), false);
                            }
                            break;
                        }
                    }
                }
                if (netWorkUnuseable && msg.what == TicketDownloadingService.OUT_SERVICE_END) {
                    netWorkUnuseable = false;
                }
            }

        };

        conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                serviceMessager = new Messenger(binder);
                Message msg = Message.obtain();
                msg.what = TicketDownloadingService.IN_BIND_MESSENGER;
                msg.obj = new Messenger(handler);
                try {
                    serviceMessager.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                serviceMessager = null;
            }
        };
    }

    @Override
    protected void onResume() {
        try {
            dataSource.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, TicketDownloadingService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    protected void onPause() {
        dataSource.close();
        unbindService(conn);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * 状态栏管理类
     * @author pengfan
     *
     */
    private static class StatusBarMananger {
        private View statusBarLayout;
        private TextView statusView;
        private View imageButton;
        private ProgressBar progressBar;

        public StatusBarMananger(View statusBarLayout, View imageButton) {
            this.statusBarLayout = statusBarLayout;
            this.imageButton = imageButton;
            statusView = (TextView) statusBarLayout.findViewById(R.id.status_bar_prompt);
            progressBar = (ProgressBar) statusBarLayout.findViewById(R.id.progressBar);
        }

        public void showStatus(String status, boolean anim) {
            ViewUtil.invisableView(imageButton, false);
            ViewUtil.showView(progressBar, false);
            ViewUtil.showView(statusBarLayout, anim);
            statusView.setText(status);
        }

        public void hiddenDelay(String status, boolean anim) {
            showStatus(status, anim);
            ViewUtil.hiddenView(progressBar, false);
            statusBarLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hidden();
                }
            }, 1000);
        }

        public void hidden() {
            ViewUtil.invisableView(statusBarLayout, false);
            ViewUtil.showView(imageButton, false);
        }
    }

}
