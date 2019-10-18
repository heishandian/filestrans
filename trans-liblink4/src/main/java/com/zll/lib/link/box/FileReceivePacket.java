package com.zll.lib.link.box;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.zll.lib.link.core.ReceivePacket;

public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {

	private File file;

	public FileReceivePacket(long len, File file) {
		super(len);
		this.file = file;
	}

	@Override
	protected File buildEntity(FileOutputStream stream) {
		return file;
	}

	@Override
	protected FileOutputStream createStream() {
		try {
			return new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public byte type() {
		return TYPE_STREAM_FILE;
	}

}
