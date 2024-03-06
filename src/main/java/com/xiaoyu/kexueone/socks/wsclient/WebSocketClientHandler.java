package com.xiaoyu.kexueone.socks.wsclient;

import cn.hutool.json.JSONUtil;
import com.xiaoyu.kexueone.core.ClientEnum;
import com.xiaoyu.kexueone.core.Connect2WsRequest;
import com.xiaoyu.kexueone.core.RelayHandler;
import com.xiaoyu.kexueone.core.Ws2ConnectResponse;
import com.xiaoyu.kexueone.utils.CloseUtils;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final WebSocketClientHandshaker handshaker;
    private ChannelHandlerContext socksCtx;
    private Socks5CommandRequest socks5CommandRequest;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, ChannelHandlerContext socksCtx
            , Socks5CommandRequest socks5CommandRequest) {
        this.handshaker = handshaker;
        this.socksCtx = socksCtx;
        this.socks5CommandRequest = socks5CommandRequest;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (!channelFuture.isSuccess()) {
                            CloseUtils.closeCtx(ctx);
                            CloseUtils.closeCtx(socksCtx);
                            logger.error("handshake failed:{}", channelFuture.cause().getMessage());
                        }
                    }
                });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("WebSocket Client disconnected!");
        CloseUtils.closeCtx(socksCtx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext wsClientCtx, Object msg) throws Exception {
        Channel ch = wsClientCtx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                wsClientCtx.channel()
                        .writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(new Connect2WsRequest(socks5CommandRequest.dstAddrType().byteValue(), socks5CommandRequest.dstAddr(), socks5CommandRequest.dstPort()))));
            } catch (WebSocketHandshakeException e) {
                logger.error("WebSocket Client failed to connect ch:{},msg:{}", ch, e.getMessage());
                CloseUtils.closeCtx(socksCtx);
                CloseUtils.closeCtx(wsClientCtx);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            if (JSONUtil.isTypeJSON(textFrame.text())) {
                Ws2ConnectResponse ws2Connect = JSONUtil.toBean(textFrame.text(), Ws2ConnectResponse.class);
                Socks5CommandStatus status = Socks5CommandStatus.valueOf(ws2Connect.getCommandStatus());
                if (Socks5CommandStatus.SUCCESS == status) {
                    socksCtx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                                    Socks5CommandStatus.SUCCESS,
                                    Socks5AddressType.valueOf(ws2Connect.getDstAddrType()),
                                    ws2Connect.getDstAddr(),
                                    ws2Connect.getDstPort()))
                            .addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture channelFuture) {
                                    Channel outboundChannel = socksCtx.channel();
                                    outboundChannel.pipeline()
                                            .addLast(new RelayHandler(wsClientCtx.channel()).setStream(ClientEnum.WS_CLIENT, ClientEnum.SOCKS_5));
                                    wsClientCtx.pipeline().remove(WebSocketClientHandler.class);
                                    wsClientCtx.pipeline().addLast(new RelayHandler(outboundChannel).setStream(ClientEnum.SOCKS_5, ClientEnum.WS_CLIENT));
                                }
                            });
                } else {
                    socksCtx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                            Socks5CommandStatus.FAILURE, Socks5AddressType.valueOf(ws2Connect.getDstAddrType())));
                    CloseUtils.closeCtx(socksCtx);
                }
            }
        } else if (frame instanceof BinaryWebSocketFrame) {
            logger.warn("BinaryWebSocketFrame");
        } else if (frame instanceof PongWebSocketFrame) {
            logger.info("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            logger.info("WebSocket Client received closing,dstAddrType:{},dstAddr:{},dstPort:{}", socks5CommandRequest.dstAddrType(), socks5CommandRequest.dstAddr(), socks5CommandRequest.dstPort());
            socksCtx.channel().writeAndFlush(new DefaultSocks5CommandResponse(
                    Socks5CommandStatus.FAILURE, socks5CommandRequest.dstAddrType()));
            CloseUtils.closeCtx(socksCtx);
            CloseUtils.closeCtx(wsClientCtx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error(cause.getMessage(), cause);
        CloseUtils.closeCtx(ctx);
        CloseUtils.closeCtx(socksCtx);
    }
}
