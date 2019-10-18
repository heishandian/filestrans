package com.zll.lib.link.core;

import java.io.IOException;
import java.io.InputStream;

public abstract class SendPacket<T extends InputStream> extends Packet<T> {

	// 包的发送状态
	private boolean isCanceled;

	public boolean isCanceled() {
		return isCanceled;
	}

	/*
	 * 设置取消发送标记
	 */
	public void cancel() {
		isCanceled = true;
	}
	
	/*
	 * 针对直流数据类型，获取当前可用数据大小
	 * PS:对于流的类型有限制，文件流一般可用正常获取，对于正在填充的流不一定有效
	 * 或得不到准确值
	 * <p>我们利用该方法不断得到直流传输的可发送数据量，从而不断生成frame
	 * <p>缺陷，对于流的数据量大于int的有效值范围外则得不到准确值 
	 * <p>一般情况下，发送数据包时不适用该方法，而使用总长度进行运算，对于直流传输则需要使用该方法，因为
	 * 对于直流而言没有最大长度 
	 */

	public int available() {
		InputStream inputStream = open();
		int available;
		try {
			available = inputStream.available();
			if (available < 0) {
				return 0;
			}
			return available;
		} catch (IOException e) {
			return 0;
		}
	}
}
