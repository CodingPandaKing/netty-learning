package me.coding.panda.learning.netty.basic;

import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author lilinfeng
 * @date 2014年2月14日
 * @version 1.0
 */
public class TimeServerHandler extends ChannelHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 读取Channel 收到的消息
        ByteBuf buf = (ByteBuf) msg;
        byte[] req = new byte[buf.readableBytes()];
        buf.readBytes(req);
        String body = new String(req, "UTF-8");
        System.out.println("服务器端收到命令 : " + body);
        String currentTime = "TIME".equalsIgnoreCase(body)
                ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
        System.out.println("服务器端返回时间 : " + currentTime);
        ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
        // 回写响应消息 此时并没写入socketchannel
        ctx.write(resp);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        // 调用write先把发送的消息 写到缓冲池中
        // 然后调用flush 进行发送 写入chanle
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
