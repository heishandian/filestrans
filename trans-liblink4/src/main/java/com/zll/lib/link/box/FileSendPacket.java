package com.zll.lib.link.box;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.zll.lib.link.core.SendPacket;

public class FileSendPacket extends SendPacket<FileInputStream> {

	private final File file;

	public FileSendPacket(File file) {
		this.file = file;
		this.length = file.length();
	}

	@Override
	protected FileInputStream createStream() {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public byte type() {
		// TODO Auto-generated method stub
		return TYPE_STREAM_FILE;
	}

}
