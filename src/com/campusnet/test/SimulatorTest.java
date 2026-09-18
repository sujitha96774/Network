package com.campusnet.test;

import com.campusnet.model.*;
import com.campusnet.protocol.FirewallEngine;
import com.campusnet.protocol.RoutingEngine;
import com.campusnet.simulator.NetworkSimulator;
import com.campusnet.simulator.ScenarioManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimulatorTest {
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println(" Running Multi-Area Campus Network Simulator Test Suite ");
        System.out.println("=========================================================");

        int passed = 0;
        int total = 6;

        if (testTopologyInitialization()) passed++;
        if (testRoutingTableCalculation()) passed++;
        if (testIntraLanPacketDelivery()) passed++;
        if (testInterBuildingManDelivery()) passed++;
        if (testDnsAndNatWanFlow()) passed++;
        if (testDynamicLinkFailover()) passed++;

        System.out.println("=========================================================");
        System.out.printf(" Test Suite Finished: %d / %d Tests PASSED!\n", passed, total);
        System.out.println("=========================================================");

        if (passed != total) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static boolean testTopologyInitialization() {
        System.out.print("[TEST 1] Testing Topology & Device Initialization... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        if (sim.getDevices().size() >= 10 && sim.getLinks().size() >= 10) {
            System.out.println("PASSED (" + sim.getDevices().size() + " devices, " + sim.getLinks().size() + " links)");
            sim.shutdown();
            return true;
        }
        System.out.println("FAILED: Insufficient devices/links");
        sim.shutdown();
        return false;
    }

    private static boolean testRoutingTableCalculation() {
        System.out.print("[TEST 2] Testing Dijkstra SPF Routing Table Computation... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        RouterDevice rtrA = (RouterDevice) sim.findDeviceByName("RTR-Building-A");
        if (rtrA == null || rtrA.getRoutingTable().isEmpty()) {
            System.out.println("FAILED: RTR-Building-A routing table is empty");
            sim.shutdown();
            return false;
        }

        // Test route match for Data Center Subnet 10.0.1.50
        RouteEntry route = rtrA.findBestRoute("10.0.1.50");
        if (route != null) {
            System.out.println("PASSED (Route found to 10.0.1.50 via " + route.getOutgoingInterface() + " metric=" + route.getMetric() + ")");
            sim.shutdown();
            return true;
        }
        System.out.println("FAILED: No route to 10.0.1.50");
        sim.shutdown();
        return false;
    }

    private static boolean testIntraLanPacketDelivery() {
        System.out.print("[TEST 3] Testing Intra-Department LAN Packet Delivery... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean delivered = new AtomicBoolean(false);

        sim.addListener(new com.campusnet.simulator.SimulationListener() {
            public void onPacketCreated(Packet packet) {}
            public void onPacketMoved(Packet packet, String from, String to, double p) {}
            public void onPacketDelivered(Packet packet) {
                if ("192.168.1.11".equals(packet.getDestIp())) {
                    delivered.set(true);
                    latch.countDown();
                }
            }
            public void onPacketDropped(Packet packet, String reason) {
                latch.countDown();
            }
            public void onTopologyChanged() {}
            public void onStatsUpdated(com.campusnet.simulator.NetworkMetrics metrics) {}
            public void onLogMessage(String message) {}
        });

        Packet ping = Packet.createPing("192.168.1.10", "192.168.1.11", 1);
        sim.sendPacket(ping);

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        sim.shutdown();
        if (delivered.get()) {
            System.out.println("PASSED");
            return true;
        }
        System.out.println("FAILED");
        return false;
    }

    private static boolean testInterBuildingManDelivery() {
        System.out.print("[TEST 4] Testing Inter-Building MAN Packet Delivery (Bldg A -> Bldg B DC)... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean delivered = new AtomicBoolean(false);

        sim.addListener(new com.campusnet.simulator.SimulationListener() {
            public void onPacketCreated(Packet packet) {}
            public void onPacketMoved(Packet packet, String from, String to, double p) {}
            public void onPacketDelivered(Packet packet) {
                if ("10.0.1.50".equals(packet.getDestIp())) {
                    delivered.set(true);
                    latch.countDown();
                }
            }
            public void onPacketDropped(Packet packet, String reason) {
                latch.countDown();
            }
            public void onTopologyChanged() {}
            public void onStatsUpdated(com.campusnet.simulator.NetworkMetrics metrics) {}
            public void onLogMessage(String message) {}
        });

        Packet httpReq = Packet.createHttpRequest("192.168.1.10", "10.0.1.50", "/");
        sim.sendPacket(httpReq);

        try {
            latch.await(6, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        sim.shutdown();
        if (delivered.get()) {
            System.out.println("PASSED");
            return true;
        }
        System.out.println("FAILED");
        return false;
    }

    private static boolean testDnsAndNatWanFlow() {
        System.out.print("[TEST 5] Testing DNS Resolution & WAN Border NAT Translation... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        String ip = sim.getDnsService().resolve("portal.campus.edu");
        if (!"10.0.1.50".equals(ip)) {
            System.out.println("FAILED: DNS did not resolve");
            sim.shutdown();
            return false;
        }

        Packet wanPacket = Packet.createHttpRequest("192.168.2.10", "198.51.100.10", "/");
        sim.getNatEngine().applyOutboundNat(wanPacket, "203.0.113.2");

        if ("203.0.113.2".equals(wanPacket.getSourceIp()) && wanPacket.getSourcePort() >= 40000) {
            System.out.println("PASSED (Source IP translated to Public WAN IP " + wanPacket.getSourceIp() + ":" + wanPacket.getSourcePort() + ")");
            sim.shutdown();
            return true;
        }

        System.out.println("FAILED: NAT did not rewrite source IP");
        sim.shutdown();
        return false;
    }

    private static boolean testDynamicLinkFailover() {
        System.out.print("[TEST 6] Testing Dynamic Link Failover & Rerouting... ");
        NetworkSimulator sim = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(sim);

        NetworkLink primary = null;
        for (NetworkLink l : sim.getLinks()) {
            if ("MAN_FIBER_PRIMARY".equals(l.getId())) {
                primary = l;
                break;
            }
        }

        if (primary != null) {
            primary.setUp(false); // Sever primary link
            sim.recalculateRouting();

            RouterDevice rtrA = (RouterDevice) sim.findDeviceByName("RTR-Building-A");
            RouteEntry rerouted = rtrA.findBestRoute("10.0.1.50");

            if (rerouted != null && "fiber0/2".equals(rerouted.getOutgoingInterface())) {
                System.out.println("PASSED (Rerouted through backup fiber0/2 via Campus Gateway ring)");
                sim.shutdown();
                return true;
            }
        }

        System.out.println("FAILED: Reroute failed");
        sim.shutdown();
        return false;
    }
}
