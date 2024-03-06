package com.xiaoyu.kexueone.core;

/**
 * 服务端响应
 *
 * @Author weibo
 * @Date 2024/2/28 10:48
 **/
public class Ws2ConnectResponse {
    private byte commandStatus;
    private byte dstAddrType;
    private String dstAddr;
    private int dstPort;

    public Ws2ConnectResponse(byte commandStatus, byte dstAddrType, String dstAddr, int dstPort) {
        this.commandStatus = commandStatus;
        this.dstAddrType = dstAddrType;
        this.dstAddr = dstAddr;
        this.dstPort = dstPort;
    }

    public byte getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(byte commandStatus) {
        this.commandStatus = commandStatus;
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
