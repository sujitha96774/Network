package com.campusnet.protocol;

import java.util.HashMap;
import java.util.Map;

public class DhcpService {
    private final String subnetPrefix; // e.g. "192.168.1."
    private final String subnetMask;
    private final String defaultGateway;
    private final String dnsServer;
    private int nextHostId = 10;
    private final Map<String, String> leasedIps = new HashMap<>(); // MAC -> IP

    public DhcpService(String subnetPrefix, String subnetMask, String defaultGateway, String dnsServer) {
        this.subnetPrefix = subnetPrefix;
        this.subnetMask = subnetMask;
        this.defaultGateway = defaultGateway;
        this.dnsServer = dnsServer;
    }

    public synchronized LeaseResult leaseIp(String mac) {
        if (leasedIps.containsKey(mac)) {
            return new LeaseResult(leasedIps.get(mac), subnetMask, defaultGateway, dnsServer);
        }
        String allocatedIp = subnetPrefix + (nextHostId++);
        leasedIps.put(mac, allocatedIp);
        return new LeaseResult(allocatedIp, subnetMask, defaultGateway, dnsServer);
    }

    public static class LeaseResult {
        public final String ip;
        public final String mask;
        public final String gateway;
        public final String dns;

        public LeaseResult(String ip, String mask, String gateway, String dns) {
            this.ip = ip;
            this.mask = mask;
            this.gateway = gateway;
            this.dns = dns;
        }
    }
}
