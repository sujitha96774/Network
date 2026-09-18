package com.campusnet.model;

public class RouteEntry {
    private final String destinationNetwork; // e.g., "192.168.1.0" or "0.0.0.0"
    private final String subnetMask;         // e.g., "255.255.255.0" or "0.0.0.0"
    private final String nextHopIp;          // e.g., "10.0.1.1" or "Direct"
    private final String outgoingInterface;  // e.g., "gi0/0"
    private final int metric;                // e.g., 1, 10
    private final String protocol;           // "DIRECT", "STATIC", "OSPF", "RIP"

    public RouteEntry(String destinationNetwork, String subnetMask, String nextHopIp,
                      String outgoingInterface, int metric, String protocol) {
        this.destinationNetwork = destinationNetwork;
        this.subnetMask = subnetMask;
        this.nextHopIp = nextHopIp;
        this.outgoingInterface = outgoingInterface;
        this.metric = metric;
        this.protocol = protocol;
    }

    public String getDestinationNetwork() {
        return destinationNetwork;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public String getNextHopIp() {
        return nextHopIp;
    }

    public String getOutgoingInterface() {
        return outgoingInterface;
    }

    public int getMetric() {
        return metric;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean matches(String targetIp) {
        if ("0.0.0.0".equals(destinationNetwork) && "0.0.0.0".equals(subnetMask)) {
            return true; // Default route matches everything
        }
        try {
            long target = NetworkInterface.ipToLong(targetIp);
            long net = NetworkInterface.ipToLong(destinationNetwork);
            long mask = NetworkInterface.ipToLong(subnetMask);
            return (target & mask) == (net & mask);
        } catch (Exception e) {
            return false;
        }
    }

    public int getPrefixLength() {
        if ("0.0.0.0".equals(subnetMask)) return 0;
        try {
            long mask = NetworkInterface.ipToLong(subnetMask);
            return Long.bitCount(mask);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("%-15s %-15s %-15s %-10s %-6d [%s]",
                destinationNetwork, subnetMask, nextHopIp, outgoingInterface, metric, protocol);
    }
}
