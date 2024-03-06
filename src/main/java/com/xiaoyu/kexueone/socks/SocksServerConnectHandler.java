package com.xiaoyu.kexueone.socks;

import com.xiaoyu.kexueone.socks.wsclient.WebSocketClientHandler;
import com.xiaoyu.kexueone.utils.CloseUtils;
import com.xiaoyu.kexueone.utils.ConfigUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

@ChannelHandler.Sharable
public final class SocksServerConnectHandler extends SimpleChannelInboundHandler<SocksMessage> {

    private static final Logger logger = LoggerFactory.getLogger(SocksServerConnectHandler.class);

    private static final SocksConfig config = ConfigUtil.getBeanConfig("socks", SocksConfig.class);

    private static EventLoopGroup wsClientWorkerGroup = new NioEventLoopGroup(1);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksMessage message) throws Exception {
        if (message instanceof Socks5CommandRequest) {
            Socks5CommandRequest request = (Socks5CommandRequest) message;
            //过滤网站
            for (String dropAddr : config.getDstAddr().getDropList()) {
                if (request.dstAddr().contains(dropAddr)) {
                    CloseUtils.closeCtx(ctx);
                    return;
                }
            }
            // connect to websocket
            URI uri = new URI(config.getWs().getServer());
            String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
            boolean ssl = "wss".equalsIgnoreCase(scheme);
            SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            } else {
                sslCtx = null;
            }
            new Bootstrap().group(wsClientWorkerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 0)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
                            }
                            p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192)
                                    , WebSocketClientCompressionHandler.INSTANCE
                                    , new WebSocketClientHandler(
                                            WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null
                                                    , true, new DefaultHttpHeaders(), Integer.MAX_VALUE), ctx, request));
                        }
                    }).connect(uri.getHost(), uri.getPort());
        } else {
            CloseUtils.closeCtx(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.getMessage(), cause);
        CloseUtils.closeCtx(ctx);
    }
}
