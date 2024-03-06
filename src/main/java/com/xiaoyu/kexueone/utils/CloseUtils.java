package com.xiaoyu.kexueone.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

public final class CloseUtils {

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch, boolean toWs) {
        if (ch.isActive()) {
            ch.writeAndFlush(toWs ? new CloseWebSocketFrame() : Unpooled.EMPTY_BUFFER)
                    .addListener(ChannelFutureListener.CLOSE);
        }
    }

    private CloseUtils() {
    }

    public static void closeCtx(ChannelHandlerContext ctx) {
        ctx.close().addListener(ChannelFutureListener.CLOSE);
    }
}
