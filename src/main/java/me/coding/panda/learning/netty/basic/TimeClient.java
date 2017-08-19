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
package me.coding.panda.learning.netty.basic;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 客户端
 */
public class TimeClient {

    public void connect(int port, String host) throws Exception {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            // 客户端辅助启动类
            Bootstrap bootstrap = new Bootstrap();

            /**
             * TCP_NODELAY 设置禁用Nagle算法 nginx 里面也有这个配置项 Nagle算法试图减少TCP包的数量和结构性开销, 将多个较小的包组合成较大的包进行发送.
             * 关键是这个算法受TCP延迟确认影响, 会导致相继两次向连接发送请求包, 读数据时会有一个最多达500毫秒的延时.
             */

            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new TimeClientHandler());
                        }
                    });

            // 发起异步连接操作
            ChannelFuture future = bootstrap.connect(host, port).sync();

            // 当代客户端链路关闭
            future.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放NIO线程组
            group.shutdownGracefully();
        }
    }

    /**
     * 启动
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        new TimeClient().connect(port, "127.0.0.1");
    }
}
