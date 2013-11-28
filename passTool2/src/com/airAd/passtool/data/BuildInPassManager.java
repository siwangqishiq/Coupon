package com.airAd.passtool.data;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;

import com.airAd.passtool.service.TicketService;

/**
 * 内置卷的导入
 * 
 * @author pengfan
 * 
 */
public class BuildInPassManager {

	public static final String BUILD_PASS = "buildPass";

	private TicketDataSource ticketDatasource;
	private Context context;

	public BuildInPassManager(Context context) {
		this.context = context;
		ticketDatasource = new TicketDataSource(context);
	}

	public void addPassFromAssert() {
		AssetManager assetManager = context.getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		SharedPreferences sp = context.getSharedPreferences(BUILD_PASS, 0);
		Editor editor = sp.edit();
		for (String fileName : files) {
			if (fileName.endsWith(".pkpass") && sp.getBoolean(fileName, true)) {
				InputStream is = null;
				OutputStream os = null;
				try {
					is = assetManager.open(fileName);
					os = new FileOutputStream(TicketService.tempFiles);
					IOUtils.copy(is, os);
					TicketService.unZipPass(ticketDatasource,
							TicketService.tempFiles, false,new Date());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					editor.putBoolean(fileName, false);
					if (is != null) {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (os != null) {
						try {
							os.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}// end for
		editor.commit();
	}
}
