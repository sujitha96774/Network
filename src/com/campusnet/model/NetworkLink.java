package com.campusnet.model;

public class NetworkLink {
    private final String id;
    private final NetworkInterface interfaceA;
    private final NetworkInterface interfaceB;
    private final AreaType areaType;
    private double bandwidthMbps; // Bandwidth in Mbps (e.g., 100 for LAN, 10000 for MAN, 50 for WAN)
    private double latencyMs;     // Latency in ms (e.g., 1ms for LAN, 3ms for MAN, 45ms for WAN)
    private double packetLossRate;// 0.0 to 1.0
    private boolean isUp;

    public NetworkLink(String id, NetworkInterface interfaceA, NetworkInterface interfaceB,
                       AreaType areaType, double bandwidthMbps, double latencyMs, double packetLossRate) {
        this.id = id;
        this.interfaceA = interfaceA;
        this.interfaceB = interfaceB;
        this.areaType = areaType;
        this.bandwidthMbps = bandwidthMbps;
        this.latencyMs = latencyMs;
        this.packetLossRate = packetLossRate;
        this.isUp = true;

        if (interfaceA != null) {
            interfaceA.setConnectedLink(this);
        }
        if (interfaceB != null) {
            interfaceB.setConnectedLink(this);
        }
    }

    public NetworkLink(String id, NetworkInterface interfaceA, NetworkInterface interfaceB, AreaType areaType) {
        this(id, interfaceA, interfaceB, areaType,
                areaType == AreaType.LAN ? 1000.0 : (areaType == AreaType.MAN ? 10000.0 : 100.0),
                areaType == AreaType.LAN ? 1.0 : (areaType == AreaType.MAN ? 3.0 : 45.0),
                0.0);
    }

    public String getId() {
        return id;
    }

    public NetworkInterface getInterfaceA() {
        return interfaceA;
    }

    public NetworkInterface getInterfaceB() {
        return interfaceB;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public double getBandwidthMbps() {
        return bandwidthMbps;
    }

    public void setBandwidthMbps(double bandwidthMbps) {
        this.bandwidthMbps = bandwidthMbps;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public double getPacketLossRate() {
        return packetLossRate;
    }

    public void setPacketLossRate(double packetLossRate) {
        this.packetLossRate = packetLossRate;
    }

    public boolean isUp() {
        return isUp;
    }

    public void setUp(boolean up) {
        isUp = up;
    }

    public NetworkInterface getOtherInterface(NetworkInterface iface) {
        if (iface == interfaceA) return interfaceB;
        if (iface == interfaceB) return interfaceA;
        return null;
    }

    public Device getOtherDevice(Device dev) {
        if (interfaceA != null && interfaceA.getOwnerDevice() == dev) {
            return interfaceB != null ? interfaceB.getOwnerDevice() : null;
        }
        if (interfaceB != null && interfaceB.getOwnerDevice() == dev) {
            return interfaceA != null ? interfaceA.getOwnerDevice() : null;
        }
        return null;
    }

    public double getRoutingCost() {
        // Cost formula: base delay + bandwidth inverse (e.g. 10000/bandwidth)
        // Similar to OSPF reference bandwidth cost
        if (!isUp) return Double.POSITIVE_INFINITY;
        return latencyMs + (1000.0 / Math.max(bandwidthMbps, 1.0));
    }

    @Override
    public String toString() {
        return String.format("%s [%s] (%s <-> %s, %.1f ms, %.0f Mbps, %s)",
                id, areaType.name(),
                interfaceA != null && interfaceA.getOwnerDevice() != null ? interfaceA.getOwnerDevice().getName() : "?",
                interfaceB != null && interfaceB.getOwnerDevice() != null ? interfaceB.getOwnerDevice().getName() : "?",
                latencyMs, bandwidthMbps, isUp ? "UP" : "DOWN");
    }
}
