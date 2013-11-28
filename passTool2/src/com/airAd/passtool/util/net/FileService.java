package com.airAd.passtool.util.net;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;

import com.airAd.passtool.worker.AsyncTask;

public abstract class FileService extends BasicService {
	private String extern;
	private File downloadFile;
    private int size = 1024;
    private AsyncTask task;
    private Long range;
    private List<String> headers = new ArrayList<String>();
    
    public String getExtern() {
		return extern;
	}

	public void setExtern(String extern) {
		this.extern = extern;
	}


    public FileService(int type) {
        super(type);
    }

    public String getHeader(int index) {
        return headers.get(index);
    }

    public int getHeaderSize() {
        return headers.size();
    }

    public void addHeader(String header) {
        headers.add(header);
    }

    public Long getRange() {
        return range;
    }

    public void setRange(Long range) {
        this.range = range;
        addHeader("RANGE", "bytes=" + range + "-");
    }

    public void setTask(AsyncTask task) {
        this.task = task;
    }

    public AsyncTask getTask() {
        return task;
    }

    public File getDownloadFile() {
        return downloadFile;
    }

    public void setDownloadFile(File downloadFile) {
        this.downloadFile = downloadFile;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public abstract String getRemoteUrl();

    public abstract void completeDownload(long size, Response rsp,Date latsModifyTime);

    @Override
    public void handleResponse(HttpResponse httpResponse, Response rsp) {

    }

}
