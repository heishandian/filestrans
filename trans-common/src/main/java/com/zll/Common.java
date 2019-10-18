package com.zll;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class Common {
	private static final String CACHE_DIR = "cache";
	public static File getCacheDir(String dir){
		String path = System.getProperty("user.dir") + (File.separator+CACHE_DIR+File.separator +dir);
		File file = new File(path);
		if(!file.exists()){
			if(!file.mkdirs()){
				throw new RuntimeException("create path error:" + path);
			}
		}
		return file;
	}
	
	public static File createRandomTemp(File parent){
		String ranStr = UUID.randomUUID().toString()+".tmp";
		File file = new File(parent,ranStr);
		try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return file;
	}

}
