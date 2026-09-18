package com.campusnet.protocol;

import com.campusnet.model.Packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NatEngine {
    public static class NatSession {
        private final String insideLocalIp;
        private final int insideLocalPort;
        private final String insideGlobalIp;
        private final int insideGlobalPort;
        private final String outsideGlobalIp;
        private final int outsideGlobalPort;
        private final long createdTime;

        public NatSession(String insideLocalIp, int insideLocalPort, String insideGlobalIp,
                          int insideGlobalPort, String outsideGlobalIp, int outsideGlobalPort) {
            this.insideLocalIp = insideLocalIp;
            this.insideLocalPort = insideLocalPort;
            this.insideGlobalIp = insideGlobalIp;
            this.insideGlobalPort = insideGlobalPort;
            this.outsideGlobalIp = outsideGlobalIp;
            this.outsideGlobalPort = outsideGlobalPort;
            this.createdTime = System.currentTimeMillis();
        }

        public String getInsideLocalIp() { return insideLocalIp; }
        public int getInsideLocalPort() { return insideLocalPort; }
        public String getInsideGlobalIp() { return insideGlobalIp; }
        public int getInsideGlobalPort() { return insideGlobalPort; }
        public String getOutsideGlobalIp() { return outsideGlobalIp; }
        public int getOutsideGlobalPort() { return outsideGlobalPort; }
        public long getCreatedTime() { return createdTime; }

        @Override
        public String toString() {
            return String.format("%s:%d <---> %s:%d (Target: %s:%d)",
                    insideLocalIp, insideLocalPort, insideGlobalIp, insideGlobalPort, outsideGlobalIp, outsideGlobalPort);
        }
    }

    private final Map<String, NatSession> outboundSessions = new ConcurrentHashMap<>(); // Key: insideLocalIp:port
    private final Map<Integer, NatSession> inboundSessions = new ConcurrentHashMap<>();  // Key: insideGlobalPort
    private final AtomicInteger nextPort = new AtomicInteger(40000);

    public boolean applyOutboundNat(Packet packet, String publicWanIp) {
        String key = packet.getSourceIp() + ":" + packet.getSourcePort();
        NatSession session = outboundSessions.get(key);

        if (session == null) {
            int allocatedPort = nextPort.getAndIncrement();
            if (nextPort.get() > 65000) nextPort.set(40000);

            session = new NatSession(
                    packet.getSourceIp(), packet.getSourcePort(),
                    publicWanIp, allocatedPort,
                    packet.getDestIp(), packet.getDestPort()
            );
            outboundSessions.put(key, session);
            inboundSessions.put(allocatedPort, session);
        }

        // Rewrite Source IP & Port
        packet.setSourceIp(session.getInsideGlobalIp());
        packet.setSourcePort(session.getInsideGlobalPort());
        return true;
    }

    public boolean applyInboundNat(Packet packet) {
        NatSession session = inboundSessions.get(packet.getDestPort());
        if (session != null) {
            // Rewrite Destination IP & Port back to private host
            packet.setDestIp(session.getInsideLocalIp());
            packet.setDestPort(session.getInsideLocalPort());
            return true;
        }
        return false;
    }

    public Map<String, NatSession> getOutboundSessions() {
        return outboundSessions;
    }

    public void clearSessions() {
        outboundSessions.clear();
        inboundSessions.clear();
    }
}
