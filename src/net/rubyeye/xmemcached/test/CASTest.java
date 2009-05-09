/**
 *Copyright [2009-2010] [dennis zhuang(killme2008@gmail.com)]
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *             http://www.apache.org/licenses/LICENSE-2.0
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *either express or implied. See the License for the specific language governing permissions and limitations under the License
 */
package net.rubyeye.xmemcached.test;

import java.util.concurrent.CountDownLatch;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.utils.AddrUtil;

/**
 * 测试CAS
 * 
 * @author dennis
 */
class CASThread extends Thread {
	/**
	 * 递增操作类，尝试在现有值上+1
	 * 
	 * @author dennis
	 * 
	 */
	static final class IncrmentOperation implements CASOperation<Integer> {
		@Override
		public int getMaxTries() {
			return Integer.MAX_VALUE; // 最大重试次数
		}

		@Override
		public Integer getNewValue(long currentCAS, Integer currentValue) {
			return currentValue + 1; // 当前值+1
		}
	}

	private XMemcachedClient mc;
	private CountDownLatch cd;

	public CASThread(XMemcachedClient mc, CountDownLatch cdl) {
		super();
		this.mc = mc;
		this.cd = cdl;

	}

	public void run() {
		try {
			if (mc.cas("a", 0, new IncrmentOperation()))
				this.cd.countDown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

public class CASTest {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage:java CASTest [threadNum] [server]");
		}
		int NUM = Integer.parseInt(args[0]);
		XMemcachedClient mc = new XMemcachedClient(AddrUtil.getAddress(args[1]));
		// 设置初始值为0
		mc.set("a", 0, 0);
		CountDownLatch cdl = new CountDownLatch(NUM);
		long start = System.currentTimeMillis();
		// 开NUM个线程递增变量a
		for (int i = 0; i < NUM; i++)
			new CASThread(mc, cdl).start();

		cdl.await();
		System.out.println("test cas,timed:"
				+ (System.currentTimeMillis() - start));
		// 打印结果,最后结果应该为NUM
		System.out.println("result=" + mc.get("a"));
		mc.shutdown();
	}
}
