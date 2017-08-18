/*
 * Copyright 2013-2018 Lilinfeng.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package me.coding.panda.learning.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 *
 */
public class AcceptCompletionHandler
        implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler> {

    @Override
    public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
    	// 已经接收成功了  为什么这里还要accept 一次
		// 调用AsynchronousSocketChannel的accept 方法后，如有有新的客户端接入 ，我们还需要继续处理新接入
		// 的请求 ， 所以在调用一次accept ， accept中又含有 completed回调 ，形成循环 ，相当于每接入
		// 一个客户端，再异步的处理其他的客户端
        attachment.asynchronousServerSocketChannel.accept(attachment, this);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        //ByteBuffer 缓冲区 用于异步的读取Chanel的数据包
		// ReadCompletionHandler 读取处理程序
        result.read(buffer, buffer, new ReadCompletionHandler(result));
    }

    @Override
    public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
    	// 接收失败
        exc.printStackTrace();
        attachment.latch.countDown();
    }

}
