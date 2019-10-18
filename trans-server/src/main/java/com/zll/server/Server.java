
package com.zll.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.zll.Common;
import com.zll.constants.TCPConstants;
import com.zll.foo.Command;
import com.zll.lib.link.core.IoContext;
import com.zll.lib.link.impl.IoSelectorProvider;
import com.zll.lib.link.impl.IoStealingSelectorProvider;
import com.zll.lib.link.impl.async.SchedulerImpl;

public class Server {
	public static void main(String[] args) throws IOException {

		// 初始化上下文
		IoContext.setup().ioProvider(new IoStealingSelectorProvider(3)).scheduler(new SchedulerImpl(1)).start();
		// 服务端缓存文件路径
		File cachePath = Common.getCacheDir("server");

		// tcp server start
		TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
		boolean isSucceed = tcpServer.start();
		if (!isSucceed) {
			System.out.println("Start TCP server failed!");
			return;
		}

		// udp server start
		UDPProvider.start(TCPConstants.PORT_SERVER);

		// 服务器输出
		InputStreamReader ir = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(ir);

		String mg;
		do {
			mg = br.readLine();
			if (mg == null ||Command.TRANS_EXIT_COMMAND.equalsIgnoreCase(mg)) {
				break;
			}
			if(mg.length() ==0){
				continue;
			}
			// 发送字符串
			tcpServer.broadcast(mg);
		} while (true);

		// tcp udp stop
		UDPProvider.stop();
		tcpServer.stop();

		IoContext.close();
	}
}
