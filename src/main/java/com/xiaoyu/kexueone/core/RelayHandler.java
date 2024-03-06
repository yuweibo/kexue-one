package com.xiaoyu.kexueone.core;

import com.xiaoyu.kexueone.utils.CloseUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RelayHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RelayHandler.class);
    private final Channel relayChannel;
    private ClientEnum downStream;
    private ClientEnum upStream;

    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    public RelayHandler setStream(ClientEnum downStream, ClientEnum upStream) {
        this.downStream = downStream;
        this.upStream = upStream;
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (relayChannel.isActive()) {
            //decode send to socks or outbound
            if (msg instanceof BinaryWebSocketFrame) {
                BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) msg;
                relayChannel.writeAndFlush(binaryWebSocketFrame.content());
            } else if (toWs()) {//encode
                relayChannel.writeAndFlush(new BinaryWebSocketFrame((ByteBuf) msg));
            } else if (msg instanceof CloseWebSocketFrame) {
                CloseUtils.closeCtx(ctx);
                CloseUtils.closeOnFlush(relayChannel,toWs());
                logger.info("RelayHandler ws closed,client:{},ch:{}", downStream.getName(), ctx.channel());
            } else {
                logger.warn("client:{} msgClass:{}", this.downStream.getName(), msg.getClass());
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private boolean toWs() {
        return ClientEnum.WS_CLIENT.equals(downStream) || ClientEnum.WS_SERVER.equals(downStream);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("RelayHandler channelInactive,upStream:{},downStream:{},ch:{}", upStream.getName(), downStream.getName(), ctx.channel());
        if (relayChannel.isActive()) {
            CloseUtils.closeOnFlush(relayChannel, toWs());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("RelayHandler channelInactive,upStream:{},downStream:{},msg:{}", upStream.getName(), downStream.getName(), cause.getMessage());
        CloseUtils.closeOnFlush(relayChannel, toWs());
        CloseUtils.closeCtx(ctx);
    }
}
