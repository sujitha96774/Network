package com.campusnet.protocol;

import com.campusnet.model.*;

import java.util.*;

public class RoutingEngine {

    public static void computeAllRoutes(List<Device> devices, List<NetworkLink> links) {
        for (Device dev : devices) {
            if (dev instanceof RouterDevice router) {
                computeRoutesForRouter(router, devices, links);
            }
        }
    }

    public static void computeRoutesForRouter(RouterDevice router, List<Device> allDevices, List<NetworkLink> allLinks) {
        router.clearRoutes();

        // 1. Add Directly Connected Networks
        for (NetworkInterface iface : router.getInterfaces()) {
            if (iface.isConfigured() && iface.isUp()) {
                String network = getNetworkAddress(iface.getIpAddress(), iface.getSubnetMask());
                router.addRoute(new RouteEntry(network, iface.getSubnetMask(), "Direct", iface.getName(), 0, "DIRECT"));
            }
        }

        // 2. Dijkstra Shortest Path Algorithm across the network graph
        Map<Device, Double> distances = new HashMap<>();
        Map<Device, Device> previousHop = new HashMap<>();
        Map<Device, NetworkInterface> firstInterface = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a.distance));

        for (Device d : allDevices) {
            distances.put(d, Double.POSITIVE_INFINITY);
        }

        distances.put(router, 0.0);
        pq.add(new NodeDistance(router, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            Device u = current.device;

            if (current.distance > distances.get(u)) continue;

            // Explore neighbors
            for (NetworkInterface uIface : u.getInterfaces()) {
                if (!uIface.isUp()) continue;
                NetworkLink link = uIface.getConnectedLink();
                if (link == null || !link.isUp()) continue;

                Device v = link.getOtherDevice(u);
                if (v == null || !v.isOnline()) continue;

                double linkCost = link.getRoutingCost();
                double newDist = distances.get(u) + linkCost;

                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    previousHop.put(v, u);

                    if (u == router) {
                        firstInterface.put(v, uIface);
                    } else {
                        firstInterface.put(v, firstInterface.get(u));
                    }

                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        // 3. For all reachable devices and their subnets, populate the routing table
        for (Device target : allDevices) {
            if (target == router || distances.get(target) == Double.POSITIVE_INFINITY) continue;

            NetworkInterface outIface = firstInterface.get(target);
            if (outIface == null) continue;

            int metricCost = (int) Math.round(distances.get(target));

            // Find next hop IP on outIface
            String nextHopIp = "Direct";
            NetworkLink link = outIface.getConnectedLink();
            if (link != null) {
                NetworkInterface otherIface = link.getOtherInterface(outIface);
                if (otherIface != null && otherIface.getIpAddress() != null) {
                    nextHopIp = otherIface.getIpAddress();
                }
            }

            for (NetworkInterface targetIface : target.getInterfaces()) {
                if (targetIface.isConfigured()) {
                    String targetNet = getNetworkAddress(targetIface.getIpAddress(), targetIface.getSubnetMask());
                    // Check if already in routing table
                    boolean exists = false;
                    for (RouteEntry re : router.getRoutingTable()) {
                        if (re.getDestinationNetwork().equals(targetNet) && re.getSubnetMask().equals(targetIface.getSubnetMask())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        router.addRoute(new RouteEntry(
                                targetNet,
                                targetIface.getSubnetMask(),
                                nextHopIp,
                                outIface.getName(),
                                metricCost,
                                "OSPF"
                        ));
                    }
                }
            }
        }
    }

    public static String getNetworkAddress(String ip, String mask) {
        if (ip == null || mask == null) return "0.0.0.0";
        try {
            long ipVal = NetworkInterface.ipToLong(ip);
            long maskVal = NetworkInterface.ipToLong(mask);
            long netVal = ipVal & maskVal;
            return NetworkInterface.longToIp(netVal);
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }

    private static class NodeDistance {
        final Device device;
        final double distance;

        NodeDistance(Device device, double distance) {
            this.device = device;
            this.distance = distance;
        }
    }
}
