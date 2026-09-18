package com.campusnet.simulator;

import com.campusnet.model.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TrafficGenerator {
    private final NetworkSimulator simulator;

    public TrafficGenerator(NetworkSimulator simulator) {
        this.simulator = simulator;
    }

    public void sendPing(String srcIp, String dstIp, int count) {
        for (int i = 1; i <= count; i++) {
            final int seq = i;
            CompletableFuture.delayedExecutor((long)(i - 1) * 800, TimeUnit.MILLISECONDS).execute(() -> {
                Packet p = Packet.createPing(srcIp, dstIp, seq);
                simulator.sendPacket(p);
            });
        }
    }

    public void sendHttpRequest(String srcIp, String dstIp, String url) {
        // Step 1: TCP SYN
        Packet syn = new Packet(srcIp, dstIp, ProtocolType.TCP_SYN, "TCP SYN [Port 80]");
        syn.setSourcePort((int)(Math.random() * 50000 + 1024));
        syn.setDestPort(80);
        syn.setSynFlag(true);
        simulator.sendPacket(syn);

        // Step 2: HTTP GET Request
        CompletableFuture.delayedExecutor(400, TimeUnit.MILLISECONDS).execute(() -> {
            Packet http = Packet.createHttpRequest(srcIp, dstIp, url);
            simulator.sendPacket(http);
        });
    }

    public void sendDnsQuery(String srcIp, String dnsServerIp, String domain) {
        Packet dns = Packet.createDnsQuery(srcIp, dnsServerIp, domain);
        simulator.sendPacket(dns);
    }

    public void startContinuousTraffic(String srcIp, String dstIp, ProtocolType protocol, int ratePerSec, int durationSec) {
        CompletableFuture.runAsync(() -> {
            int delayMs = Math.max(50, 1000 / ratePerSec);
            long endTime = System.currentTimeMillis() + (durationSec * 1000L);
            int seq = 1;
            while (System.currentTimeMillis() < endTime) {
                Packet p = new Packet(srcIp, dstIp, protocol, "Data Stream Payload #" + seq++);
                p.setSourcePort(12345);
                p.setDestPort(80);
                simulator.sendPacket(p);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }

    public void sendDdosBurst(String srcIpPrefix, String targetIp, int packetCount) {
        CompletableFuture.runAsync(() -> {
            for (int i = 1; i <= packetCount; i++) {
                String spoofedSrc = srcIpPrefix + (int)(Math.random() * 250 + 1);
                Packet floodPacket = new Packet(spoofedSrc, targetIp, ProtocolType.TCP_SYN, "SYN FLOOD Attack Frame #" + i);
                floodPacket.setSourcePort((int)(Math.random() * 60000 + 1024));
                floodPacket.setDestPort(80);
                simulator.sendPacket(floodPacket);
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {}
            }
        });
    }
}
