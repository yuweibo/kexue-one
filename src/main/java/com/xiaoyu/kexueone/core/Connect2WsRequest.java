package com.xiaoyu.kexueone.core;

/**
 * socks connect cmd to websocket msg
 *
 * @Author weibo
 * @Date 2024/2/28 10:17
 **/
public class Connect2WsRequest {

    private byte dstAddrType;
    private String dstAddr;
    private int dstPort;

    public Connect2WsRequest(byte dstAddrType, String dstAddr, int dstPort) {
        this.dstAddrType = dstAddrType;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

    public byte getDstAddrType() {
        return dstAddrType;
    }

    public void setDstAddrType(byte dstAddrType) {
        this.dstAddrType = dstAddrType;
    }

    public String getDstAddr() {
        return dstAddr;
    }

    public void setDstAddr(String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }
}
