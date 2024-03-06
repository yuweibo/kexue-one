package com.xiaoyu.kexueone.socks;

import com.xiaoyu.kexueone.utils.ConnectionCountHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;

public final class SocksServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline().addLast(
                new ConnectionCountHandler(),
                new SocksPortUnificationServerHandler(),
                SocksServerHandler.INSTANCE);
    }
}
