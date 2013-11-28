package com.airAd.passtool.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.os.Environment;

import com.airAd.passtool.MyApplication;
import com.airAd.passtool.data.Config;
import com.airAd.passtool.data.TicketDataSource;
import com.airAd.passtool.util.FileUtil;
import com.airAd.passtool.util.LogUtil;
import com.airAd.passtool.util.StringUtil;
import com.airAd.passtool.util.net.FileService;
import com.airAd.passtool.util.net.Response;

/**
 * 下载票据的接口
 * 
 * @author pengfan
 * 
 */
public class TicketService extends FileService {
	public static final int RES_OK = 200;
	public static final String parentFolderPath = Environment
			.getExternalStorageDirectory().getAbsolutePath() + "/0ticket/";
	public final File tempFile = new File(parentFolderPath + "temp"
			+ (new Date().getTime()) + ".pass");
	public static final File tempFiles = new File(parentFolderPath + "temp.pass");
	private String URL;
	private static final String passFileName = "pass.json";
	private static final int IO_BUFFER_SIZE = 1024;
	private TicketDataSource datasource;
	protected boolean update = true;

	public TicketService(Context context) {
		this(context, TYPE_GET);
		addHeader("passtitle");
		addHeader("passcolor");
		setSize(IO_BUFFER_SIZE);
	}

	protected TicketService(Context context, int type) {
		super(type);
		datasource = new TicketDataSource(context);
		File temp = new File(parentFolderPath);
		if (!temp.exists()) {
			temp.mkdirs();
		}
	}

	public void setURL(String URL) {
		this.URL = URL;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	@Override
	public File getDownloadFile() {
		return tempFile;
	}

	@Override
	public void completeDownload(long size, Response rsp,Date lastModifyTime) {
		rsp.setData(TicketDataSource.FORMAT_FAIL);
		if (size > 0) {
			rsp.setData(unZipPass(datasource, tempFile, update,lastModifyTime));
			if (!StringUtil.isBlank(getExtern())) {
				MyApplication.getCurrentApp().getConfig()
						.removeUnUpdateItem(getExtern());
			}// end if
		}
	}

	public static Integer unZipPass(TicketDataSource datasource, File zipFile,
			boolean needRegister,Date lastModifyTime) {
		Integer res = null;
		try {
			String id = getId();
			unZip(zipFile, id);
			File passFile = null;
			if ((passFile = getPassFile(id)) != null) {
				datasource.open();
				res = datasource.insertOrUpdate(
						FileUtil.read(new FileInputStream(passFile)), id,
						needRegister,lastModifyTime);
			}
		} catch (Exception e) {
			LogUtil.w(TicketService.class, e.getMessage());
			e.printStackTrace();
		} finally {
			datasource.close();
		}
		return res;
	}

	@Override
	public String getRemoteUrl() {
		return URL;
	}

	private static File getPassFile(String id) {
		File passFile = new File(parentFolderPath + id, passFileName);
		if (passFile.exists()) {
			return passFile;
		}
		return null;
	}

	public static synchronized boolean unZip(File passFile, String folderName)
			throws IOException {
		try {
			ZipFile zipfile = new ZipFile(passFile);
			for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				unzipEntry(zipfile, entry, parentFolderPath + folderName);
			}
		} catch (Exception e) {
			LogUtil.e(TicketService.class,
					"Error while extracting file " + parentFolderPath
							+ folderName + ",error--->" + e.toString());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void unzipEntry(ZipFile zipFile, ZipEntry entry,
			String outputDir) throws IOException {

		if (entry.isDirectory()) {
			createDir(new File(outputDir, entry.getName()));
			return;
		}

		File outputFile = new File(outputDir, entry.getName());
		if (!outputFile.getParentFile().exists()) {
			createDir(outputFile.getParentFile());
		}

		LogUtil.i(TicketService.class, "Extracting: " + entry);
		BufferedInputStream inputStream = new BufferedInputStream(
				zipFile.getInputStream(entry));
		BufferedOutputStream outputStream = new BufferedOutputStream(
				new FileOutputStream(outputFile));

		try {
			IOUtils.copy(inputStream, outputStream);
		} finally {
			outputStream.close();
			inputStream.close();
		}
	}

	private static void createDir(File dir) {
		if (dir.exists()) {
			return;
		}
		LogUtil.i(TicketService.class, "Creating dir " + dir.getName());
		if (!dir.mkdirs()) {
			throw new RuntimeException("Can not create dir " + dir);
		}
	}

	private static String getId() {
		long name = new Date().getTime();
		LogUtil.i(TicketService.class, "线程--- >" + Thread.currentThread()
				+ "创建文件名称 " + name);
		return String.valueOf(name);
	}
}
