package com.xiaoyu.kexueone.ws;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xiaoyu.kexueone.core.ClientEnum;
import com.xiaoyu.kexueone.core.Connect2WsRequest;
import com.xiaoyu.kexueone.core.RelayHandler;
import com.xiaoyu.kexueone.core.Ws2ConnectResponse;
import com.xiaoyu.kexueone.utils.CloseUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.util.Locale;

/**
 * Echoes uppercase content of text frames.
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final Bootstrap b = new Bootstrap();

    @Override
    protected void channelRead0(ChannelHandlerContext wsServerCtx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled

        if (frame instanceof TextWebSocketFrame) {
            // Send the uppercase string back.
            String request = ((TextWebSocketFrame) frame).text();
            if (!JSONUtil.isTypeJSON(request)) {
                wsServerCtx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));
            } else {
                //json转换为Connect2Ws
                Connect2WsRequest connect2Ws = JSONUtil.toBean(request, Connect2WsRequest.class);
                //判断内容是否正确
                if (StrUtil.isBlank(connect2Ws.getDstAddr())) {
                    Ws2ConnectResponse ws2Connect = new Ws2ConnectResponse(Socks5CommandStatus.FAILURE.byteValue(), (byte) -1, null, -1);
                    String response = JSONUtil.toJsonStr(ws2Connect);
                    wsServerCtx.channel()
                            .writeAndFlush(new TextWebSocketFrame(response));
                    return;
                }
                //连接服务器成功后发送给websocket客户端内容
                Promise<Channel> promise = wsServerCtx.executor().newPromise();
                promise.addListener(
                        new FutureListener<Channel>() {
                            @Override
                            public void operationComplete(final Future<Channel> future) throws Exception {
                                final Channel outboundChannel = future.getNow();
                                if (future.isSuccess()) {
                                    String ws2ConnectStr = JSONUtil.toJsonStr(new Ws2ConnectResponse(Socks5CommandStatus.SUCCESS.byteValue(), connect2Ws.getDstAddrType(), connect2Ws.getDstAddr(), connect2Ws.getDstPort()));
                                    wsServerCtx.channel()
                                            .writeAndFlush(new TextWebSocketFrame(ws2ConnectStr))
                                            .addListener(new ChannelFutureListener() {
                                                @Override
                                                public void operationComplete(ChannelFuture channelFuture) {
                                                    //发送给客户端成功后建立发送应答通道
                                                    if (channelFuture.isSuccess()) {
                                                        outboundChannel.pipeline().addLast(new RelayHandler(wsServerCtx.channel()).setStream(ClientEnum.WS_SERVER, ClientEnum.TARGET));
                                                        wsServerCtx.pipeline().remove(WebSocketFrameHandler.class);
                                                        wsServerCtx.pipeline().addLast(new RelayHandler(outboundChannel).setStream(ClientEnum.TARGET, ClientEnum.WS_SERVER));
                                                    } else {
                                                        //发送失败，关闭websocket连接和目标服务器连接
                                                        CloseUtils.closeOnFlush(outboundChannel, false);
                                                        CloseUtils.closeCtx(wsServerCtx);
                                                    }
                                                }
                                            });
                                } else {
                                    Ws2ConnectResponse ws2Connect = new Ws2ConnectResponse(Socks5CommandStatus.FAILURE.byteValue(), connect2Ws.getDstAddrType(), connect2Ws.getDstAddr(), connect2Ws.getDstPort());
                                    String ws2ConnectStr = JSONUtil.toJsonStr(ws2Connect);
                                    wsServerCtx.channel().writeAndFlush(ws2ConnectStr);
                                }
                            }
                        });
                Channel inboundChannel = wsServerCtx.channel();
                b.group(inboundChannel.eventLoop())
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new DirectClientHandler(promise));

                b.connect(connect2Ws.getDstAddr(), connect2Ws.getDstPort()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            // Connection established use handler provided results
                        } else {
                            // Close the connection if the connection attempt has failed.
                            String response = JSONUtil.toJsonStr(new Ws2ConnectResponse(Socks5CommandStatus.FAILURE.byteValue(), connect2Ws.getDstAddrType(), connect2Ws.getDstAddr(), connect2Ws.getDstPort()));
                            wsServerCtx.channel()
                                    .writeAndFlush(new TextWebSocketFrame(response));
                            CloseUtils.closeOnFlush(wsServerCtx.channel(), true);
                        }
                    }
                });
            }
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }
}
