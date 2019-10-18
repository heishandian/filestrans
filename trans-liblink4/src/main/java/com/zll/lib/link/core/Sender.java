package com.zll.lib.link.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable  {
	
	void setSendListener(IoArgs.IoArgsEventProcessor processor);
	void postSendAsync() throws Exception;
	long getLastWriteTime();
}
