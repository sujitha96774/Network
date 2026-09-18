package com.campusnet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Packet {
    public enum Status {
        CREATED,
        IN_TRANSIT,
        DELIVERED,
        DROPPED
    }

    private final String id;
    private final long creationTime;
    
    // Layer 2 - Data Link
    private String sourceMac;
    private String destMac;
    private int vlanId = 1;

    // Layer 3 - Network
    private String sourceIp;
    private String destIp;
    private int ttl = 64;
    private ProtocolType protocol;

    // Layer 4 - Transport
    private int sourcePort = 0;
    private int destPort = 0;
    private long sequenceNumber = 0;
    private long ackNumber = 0;
    private boolean synFlag = false;
    private boolean ackFlag = false;
    private boolean finFlag = false;

    // Layer 7 - Application / Payload
    private String payload = "";
    private String httpMethod;
    private String httpUrl;
    private int httpStatusCode;
    private String dnsQuery;
    private String dnsResolvedIp;
    private int pingSequence = 0;

    // Trajectory & Diagnostics
    private final List<String> hopPath = new ArrayList<>();
    private Status status = Status.CREATED;
    private String dropReason = null;
    private NetworkLink currentLink = null;
    private Device currentDevice = null;
    private double progressOnLink = 0.0; // 0.0 to 1.0 for UI animation

    public Packet(String sourceIp, String destIp, ProtocolType protocol, String payload) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.creationTime = System.currentTimeMillis();
        this.sourceIp = sourceIp;
        this.destIp = destIp;
        this.protocol = protocol;
        this.payload = payload;
        this.status = Status.IN_TRANSIT;
    }

    public static Packet createPing(String srcIp, String dstIp, int seq) {
        Packet p = new Packet(srcIp, dstIp, ProtocolType.ICMP, "PING Echo Request seq=" + seq);
        p.setPingSequence(seq);
        return p;
    }

    public static Packet createPingReply(String srcIp, String dstIp, int seq) {
        Packet p = new Packet(srcIp, dstIp, ProtocolType.ICMP, "PING Echo Reply seq=" + seq);
        p.setPingSequence(seq);
        return p;
    }

    public static Packet createHttpRequest(String srcIp, String dstIp, String url) {
        Packet p = new Packet(srcIp, dstIp, ProtocolType.HTTP, "GET " + url + " HTTP/1.1");
        p.setSourcePort((int)(Math.random() * 50000 + 1024));
        p.setDestPort(80);
        p.setHttpMethod("GET");
        p.setHttpUrl(url);
        p.setSynFlag(false);
        return p;
    }

    public static Packet createHttpResponse(String srcIp, String dstIp, int statusCode, String body) {
        Packet p = new Packet(srcIp, dstIp, ProtocolType.HTTP, "HTTP/1.1 " + statusCode + " OK\n" + body);
        p.setSourcePort(80);
        p.setDestPort(8080);
        p.setHttpStatusCode(statusCode);
        return p;
    }

    public static Packet createDnsQuery(String srcIp, String dnsServerIp, String queryDomain) {
        Packet p = new Packet(srcIp, dnsServerIp, ProtocolType.DNS, "DNS Standard Query A " + queryDomain);
        p.setSourcePort((int)(Math.random() * 50000 + 1024));
        p.setDestPort(53);
        p.setDnsQuery(queryDomain);
        return p;
    }

    public static Packet createDnsResponse(String dnsServerIp, String clientIp, String domain, String resolvedIp) {
        Packet p = new Packet(dnsServerIp, clientIp, ProtocolType.DNS, "DNS Query Response: " + domain + " -> " + resolvedIp);
        p.setSourcePort(53);
        p.setDnsQuery(domain);
        p.setDnsResolvedIp(resolvedIp);
        return p;
    }

    public static Packet createArpRequest(String srcIp, String srcMac, String targetIp) {
        Packet p = new Packet(srcIp, targetIp, ProtocolType.ARP, "Who has " + targetIp + "? Tell " + srcIp);
        p.setSourceMac(srcMac);
        p.setDestMac("FF:FF:FF:FF:FF:FF");
        return p;
    }

    public static Packet createArpReply(String srcIp, String srcMac, String targetIp, String targetMac) {
        Packet p = new Packet(srcIp, targetIp, ProtocolType.ARP, targetIp + " is at " + srcMac);
        p.setSourceMac(srcMac);
        p.setDestMac(targetMac);
        return p;
    }

    // Getters and Setters
    public String getId() { return id; }
    public long getCreationTime() { return creationTime; }
    public String getSourceMac() { return sourceMac; }
    public void setSourceMac(String sourceMac) { this.sourceMac = sourceMac; }
    public String getDestMac() { return destMac; }
    public void setDestMac(String destMac) { this.destMac = destMac; }
    public int getVlanId() { return vlanId; }
    public void setVlanId(int vlanId) { this.vlanId = vlanId; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getDestIp() { return destIp; }
    public void setDestIp(String destIp) { this.destIp = destIp; }
    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }
    public int decrementTtl() { return --this.ttl; }
    public ProtocolType getProtocol() { return protocol; }
    public void setProtocol(ProtocolType protocol) { this.protocol = protocol; }
    public int getSourcePort() { return sourcePort; }
    public void setSourcePort(int sourcePort) { this.sourcePort = sourcePort; }
    public int getDestPort() { return destPort; }
    public void setDestPort(int destPort) { this.destPort = destPort; }
    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public long getAckNumber() { return ackNumber; }
    public void setAckNumber(long ackNumber) { this.ackNumber = ackNumber; }
    public boolean isSynFlag() { return synFlag; }
    public void setSynFlag(boolean synFlag) { this.synFlag = synFlag; }
    public boolean isAckFlag() { return ackFlag; }
    public void setAckFlag(boolean ackFlag) { this.ackFlag = ackFlag; }
    public boolean isFinFlag() { return finFlag; }
    public void setFinFlag(boolean finFlag) { this.finFlag = finFlag; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getHttpUrl() { return httpUrl; }
    public void setHttpUrl(String httpUrl) { this.httpUrl = httpUrl; }
    public int getHttpStatusCode() { return httpStatusCode; }
    public void setHttpStatusCode(int httpStatusCode) { this.httpStatusCode = httpStatusCode; }
    public String getDnsQuery() { return dnsQuery; }
    public void setDnsQuery(String dnsQuery) { this.dnsQuery = dnsQuery; }
    public String getDnsResolvedIp() { return dnsResolvedIp; }
    public void setDnsResolvedIp(String dnsResolvedIp) { this.dnsResolvedIp = dnsResolvedIp; }
    public int getPingSequence() { return pingSequence; }
    public void setPingSequence(int pingSequence) { this.pingSequence = pingSequence; }
    public List<String> getHopPath() { return hopPath; }
    public void addHop(String deviceName) { this.hopPath.add(deviceName); }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getDropReason() { return dropReason; }
    public void setDropReason(String dropReason) { this.dropReason = dropReason; this.status = Status.DROPPED; }
    public NetworkLink getCurrentLink() { return currentLink; }
    public void setCurrentLink(NetworkLink currentLink) { this.currentLink = currentLink; }
    public Device getCurrentDevice() { return currentDevice; }
    public void setCurrentDevice(Device currentDevice) { this.currentDevice = currentDevice; }
    public double getProgressOnLink() { return progressOnLink; }
    public void setProgressOnLink(double progressOnLink) { this.progressOnLink = progressOnLink; }

    public int getPacketSize() {
        return 64 + (payload != null ? payload.length() : 0);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s %s -> %s (%s)", id, protocol.getCode(), sourceIp, destIp, payload);
    }
}
