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
package me.coding.panda.learning.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;


/**
 *
 */
public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel servChannel;

    private volatile boolean stop;

    /**
     * 初始化多路复用器、绑定监听端口
     * 
     * @param port
     */
    public MultiplexerTimeServer(int port) {
        try {
            // 初始化多路复用器
            selector = Selector.open();
            servChannel = ServerSocketChannel.open();
            // 设置非阻塞
            servChannel.configureBlocking(false);
            // 绑定端口 设置请求传入连接队列的最大长度1024
            servChannel.socket().bind(new InetSocketAddress(port), 1024);
            // 将channel 注册到多路复用器上 并且监听ACCEPT事件
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("时间服务器启动成功 Port : " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }


    @Override
    public void run() {
        while (!stop) {
            try {
                // 设置Timeout 事件1000ms
                selector.select(1000);
                // 无限轮询就绪的Key
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        // 处理selection key
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，
        // 所以不需要重复释放资源
        if (selector != null)
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void handleInput(SelectionKey key) throws IOException {

        if (key.isValid()) {

            // 如果Key 有效
            if (key.isAcceptable()) {
                // 接收Connection Channel
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                // 新的客户端接入 TCP的三次握手 建立物理链路
                SocketChannel sc = ssc.accept();
                // 设置客户端链路为非阻塞
                sc.configureBlocking(false);
                // 将新接入的客户端注册到Reactor线程的多路复用器上，监听读操作，
                // 读取客户端发送的消息
                sc.register(selector, SelectionKey.OP_READ);
            }

            if (key.isReadable()) {
                // key 出于可读状态
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // 读取客户端请求消息到缓冲区
                int readBytes = sc.read(readBuffer);

                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("时间服务器收到命令 : " + body);
                    String currentTime = "TIME".equalsIgnoreCase(body)
                            ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    // 返回当前时间
                    doWrite(sc, currentTime);
                    System.out.println("服务端返回当前时间: " + currentTime);
                } else if (readBytes < 0) {
                    // 对端链路关闭
                    key.cancel();
                    sc.close();
                } else
                    ; // 读到0字节，忽略
            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }
    }
}
