package com.xiaoyu.kexueone.socks;

import java.util.List;

/**
 * SocksConfig配置
 *
 * @Author weibo
 * @Date 2024/2/29 9:14
 **/
public class SocksConfig {

    private DstAddr dstAddr;
    private Ws ws;

    public DstAddr getDstAddr() {
        return dstAddr;
    }

    public void setDstAddr(DstAddr dstAddr) {
        this.dstAddr = dstAddr;
    }

    public Ws getWs() {
        return ws;
    }

    public void setWs(Ws ws) {
        this.ws = ws;
    }

    public static class Ws {
        private String server;

        public String getServer() {
            return server;
        }

        public void setServer(String server) {
            this.server = server;
        }
    }

    public static class DstAddr {
        private List<String> dropList;

        public List<String> getDropList() {
            return dropList;
        }

        public void setDropList(List<String> dropList) {
            this.dropList = dropList;
        }
    }
}
