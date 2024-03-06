package com.xiaoyu.kexueone.core;

/**
 * netty channel 客户端枚举
 *
 * @Author weibo
 * @Date 2024/2/29 11:11
 **/
public enum ClientEnum {
    SOCKS_5("socks5连接"), WS_CLIENT("websocket客户端连接")
    , WS_SERVER("websocket服务端连接"), TARGET("目标请求连接");

    private String name;

    ClientEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
