package com.zll.lib.link.core;

import java.io.Closeable;
import java.io.IOException;
public interface Receiver extends Closeable{
	
	void setReceiveListener(IoArgs.IoArgsEventProcessor processor);
	void postReceiveAsync() throws Exception;
	long getLastReadTime();
}
