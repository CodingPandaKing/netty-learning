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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 时间服务器
 */
public class TimeServer {

    public void bind(int port) throws Exception {

        // 配置服务端的NIO线程组
        // 本质上其实是reactor线程组 ，一个用来接收请求 一个用来读写Channel数据
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // Netty 服务端启动类
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 这里类似于NIO的 设置Tcp参数 绑定处理事件
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024).childHandler(new ChildChannelHandler());

            // 绑定端口，sync同步等待绑定成功
            // ChannelFuture 类似月JDK的Future 用于通知回调
            ChannelFuture future = bootstrap.bind(port).sync();

            // 等待服务器链路关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            // 初始化SocketChannel 给你的SocketChannel 绑定一个处理事件
            socketChannel.pipeline().addLast(new TimeServerHandler());
        }

    }


    /**
     * 启动时间服务器
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        new TimeServer().bind(port);
    }
}
