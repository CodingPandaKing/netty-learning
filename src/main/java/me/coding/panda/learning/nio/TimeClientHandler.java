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
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Administrator
 * @date 2014年2月16日
 * @version 1.0
 */
public class TimeClientHandler implements Runnable {

    private String host;
    private int port;

    private Selector selector;
    private SocketChannel socketChannel;

    private volatile boolean stop;

    public TimeClientHandler(String host, int port) {

        this.host = host;
        this.port = port;

        try {
            // 初始化Socket Channel
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            doConnect();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // 轮询多路复用器
        while (!stop) {
            try {
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectedKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        // 处理SelectionKey
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
        if (selector != null)
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    private void handleInput(SelectionKey key) throws IOException {

        if (key.isValid()) {
            SocketChannel sc = (SocketChannel) key.channel();
            if (key.isConnectable()) {
                // 判断是否连接成功 返回TRUE 说明服务端已经返回ACK应答消息
                if (sc.finishConnect()) {
                    // 设置操作位为READ
                    sc.register(selector, SelectionKey.OP_READ);
                    // 发送查询时间命令
                    doWrite(sc);
                } else {
                    // 连接失败，进程退出
                    System.exit(1);
                }
            }

            if (key.isReadable()) {
                // 如果Key有数据可读
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("当前时间是 : " + body);
                    this.stop = true;
                } else if (readBytes < 0) {
                    // 对端链路关闭
                    key.cancel();
                    sc.close();
                } else
                    ; // 读到0字节，忽略
            }
        }

    }

    private void doConnect() throws IOException {
        if (socketChannel.connect(new InetSocketAddress(host, port))) {
            // 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
            socketChannel.register(selector, SelectionKey.OP_READ);
            // 发送查询时间命令
            doWrite(socketChannel);
        } else {
            // false 没有连接成功(异步链接，客户端已经发送sync包,但是服务端没返回ack)
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel sc) throws IOException {
        byte[] req = "TIME".getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        sc.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            System.out.println("发送查询时间命令成功.");
        }

    }

}
