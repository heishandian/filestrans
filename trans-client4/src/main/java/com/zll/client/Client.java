package com.zll.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.zll.Common;
import com.zll.bean.ServerInfo;
import com.zll.foo.Command;
import com.zll.foo.handler.ConnectorCloseChain;
import com.zll.foo.handler.ConnectorHandler;
import com.zll.lib.link.box.FileSendPacket;
import com.zll.lib.link.core.Connector;
import com.zll.lib.link.core.IoContext;
import com.zll.lib.link.core.ScheduleJob;
import com.zll.lib.link.core.schedule.IdleTimeoutSchedule;
import com.zll.lib.link.impl.IoSelectorProvider;
import com.zll.lib.link.impl.IoStealingSelectorProvider;
import com.zll.lib.link.impl.async.SchedulerImpl;
import com.zll.lib.link.utils.CloseUtils;

public class Client {
	public static void main(String[] args) throws IOException {

		File cachePath = Common.getCacheDir("client");

		// 初始化上下文
		IoContext.setup().ioProvider(new IoStealingSelectorProvider(1)).scheduler(new SchedulerImpl(1)).start();

		ServerInfo info = UDPSearcher.searchServer(60000);
		System.out.println("Server:" + info);

		if (info != null) {
			TCPClient tcpClient = null;
			try {
				tcpClient = TCPClient.startWith(info, cachePath);
				if (tcpClient == null) {
					return;
				}
				
				tcpClient.getCloseChain().appendLast(new ConnectorCloseChain() {

					@Override
					protected boolean consume(ConnectorHandler clientHandler, Connector model) {
						CloseUtils.close(System.in);
						return true;
					}
				});

				ScheduleJob scheduleJob = new IdleTimeoutSchedule(50, TimeUnit.SECONDS, tcpClient);
				tcpClient.schedule(scheduleJob);
				write(tcpClient);
				
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (tcpClient != null) {
					tcpClient.exit();
				}
			}
		}
		IoContext.close();
	}

	private static void write(TCPClient tcpClient) throws IOException {
		// TODO Auto-generated method stub
		// 构建键盘输入流
		InputStream in = System.in;
		BufferedReader input = new BufferedReader(new InputStreamReader(in));

		do {
			// 键盘读取一行
			String str = input.readLine();
			if (str == null || Command.TRANS_EXIT_COMMAND.equalsIgnoreCase(str)) {
				break;
			}

			if (str.length() == 0) {
				continue;
			}
			// 发送文件 --f url
			if (str.startsWith("--f")) {
				String[] array = str.split(" ");
				if (array.length >= 2) {
					String filePath = array[1];
					File file = new File(filePath);
					if (file.exists() && file.isFile()) {
						FileSendPacket packet = new FileSendPacket(file);
						tcpClient.sendFilePacket(packet);
						continue;
					}
				}
			}
			// 上述不满足则发送字符串
			tcpClient.send(str);
		} while (true);
	}
}
