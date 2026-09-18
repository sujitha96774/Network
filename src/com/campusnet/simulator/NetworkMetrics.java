package com.campusnet.simulator;

import com.campusnet.model.ProtocolType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkMetrics {
    private final AtomicLong totalPacketsSent = new AtomicLong(0);
    private final AtomicLong totalPacketsDelivered = new AtomicLong(0);
    private final AtomicLong totalPacketsDropped = new AtomicLong(0);
    private final AtomicLong totalBytesTransferred = new AtomicLong(0);

    private final Map<ProtocolType, AtomicLong> protocolCounts = new ConcurrentHashMap<>();
    private final List<Double> latencySamples = Collections.synchronizedList(new ArrayList<>());
    
    // Time-series history for chart rendering (last 60 samples)
    private final List<Long> throughputHistory = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> latencyHistory = Collections.synchronizedList(new ArrayList<>());
    private long lastThroughputBytes = 0;

    public NetworkMetrics() {
        for (ProtocolType pt : ProtocolType.values()) {
            protocolCounts.put(pt, new AtomicLong(0));
        }
        for (int i = 0; i < 30; i++) {
            throughputHistory.add(0L);
            latencyHistory.add(0.0);
        }
    }

    public void recordPacketSent(ProtocolType pt, int bytes) {
        totalPacketsSent.incrementAndGet();
        totalBytesTransferred.addAndGet(bytes);
        protocolCounts.computeIfAbsent(pt, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordPacketDelivered(long latencyMs) {
        totalPacketsDelivered.incrementAndGet();
        latencySamples.add((double) latencyMs);
        if (latencySamples.size() > 500) {
            latencySamples.remove(0);
        }
    }

    public void recordPacketDropped() {
        totalPacketsDropped.incrementAndGet();
    }

    public void tickSample() {
        long currentBytes = totalBytesTransferred.get();
        long diffBytes = currentBytes - lastThroughputBytes;
        lastThroughputBytes = currentBytes;
        long kbps = (diffBytes * 8) / 1000;

        synchronized (throughputHistory) {
            throughputHistory.add(kbps);
            if (throughputHistory.size() > 50) {
                throughputHistory.remove(0);
            }
        }

        double avgLat = getAverageLatency();
        synchronized (latencyHistory) {
            latencyHistory.add(avgLat);
            if (latencyHistory.size() > 50) {
                latencyHistory.remove(0);
            }
        }
    }

    public long getTotalPacketsSent() { return totalPacketsSent.get(); }
    public long getTotalPacketsDelivered() { return totalPacketsDelivered.get(); }
    public long getTotalPacketsDropped() { return totalPacketsDropped.get(); }
    public long getTotalBytesTransferred() { return totalBytesTransferred.get(); }

    public double getDeliveryRate() {
        long sent = totalPacketsSent.get();
        if (sent == 0) return 100.0;
        return (totalPacketsDelivered.get() * 100.0) / sent;
    }

    public double getAverageLatency() {
        synchronized (latencySamples) {
            if (latencySamples.isEmpty()) return 0.0;
            double sum = 0;
            for (Double d : latencySamples) sum += d;
            return sum / latencySamples.size();
        }
    }

    public Map<ProtocolType, Long> getProtocolDistribution() {
        Map<ProtocolType, Long> map = new HashMap<>();
        for (Map.Entry<ProtocolType, AtomicLong> entry : protocolCounts.entrySet()) {
            map.put(entry.getKey(), entry.getValue().get());
        }
        return map;
    }

    public List<Long> getThroughputHistory() {
        synchronized (throughputHistory) {
            return new ArrayList<>(throughputHistory);
        }
    }

    public List<Double> getLatencyHistory() {
        synchronized (latencyHistory) {
            return new ArrayList<>(latencyHistory);
        }
    }

    public void reset() {
        totalPacketsSent.set(0);
        totalPacketsDelivered.set(0);
        totalPacketsDropped.set(0);
        totalBytesTransferred.set(0);
        latencySamples.clear();
        for (ProtocolType pt : ProtocolType.values()) {
            protocolCounts.put(pt, new AtomicLong(0));
        }
    }
}
