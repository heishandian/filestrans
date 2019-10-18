package com.zll.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Utils {
	public static void main(String[] args) throws IOException {
		
		String code_src = getCode("E:\\learn2016\\project.zip");
		String code_dest = getCode("E:\\codespace\\space\\socket_space\\filestrans\\trans-server\\cache\\server\\0ec7fe88-505b-4ed6-8b01-4588a3e27f26.tmp");
		System.out.println(code_src.equals(code_dest));
	}

	static String getCode(String path) throws IOException{
		
		File file = new File(path);
		FileInputStream in = new FileInputStream(file);
		String code = DigestUtils.md5Hex(in);
		return code;
	}
}
