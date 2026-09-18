package com.campusnet.simulator;

import com.campusnet.model.*;
import com.campusnet.protocol.*;

import java.util.*;
import java.util.concurrent.*;

public class NetworkSimulator {
    private final List<Device> devices = new CopyOnWriteArrayList<>();
    private final List<NetworkLink> links = new CopyOnWriteArrayList<>();
    private final List<Packet> activeInFlightPackets = new CopyOnWriteArrayList<>();
    private final List<Packet> packetHistory = new CopyOnWriteArrayList<>();
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();

    private final NetworkMetrics metrics = new NetworkMetrics();
    private final NatEngine natEngine = new NatEngine();
    private final FirewallEngine firewallEngine = new FirewallEngine();
    private final DnsService dnsService = new DnsService();

    private volatile boolean isRunning = true;
    private volatile boolean isPaused = false;
    private volatile double simulationSpeed = 1.0; // 0.2x to 10.0x
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> simulationTask;
    private ScheduledFuture<?> metricsTask;

    public NetworkSimulator() {
        startSimulationLoop();
    }

    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }

    public void addDevice(Device device) {
        devices.add(device);
        notifyTopologyChanged();
    }

    public void addLink(NetworkLink link) {
        links.add(link);
        notifyTopologyChanged();
    }

    public List<Device> getDevices() {
        return devices;
    }

    public List<NetworkLink> getLinks() {
        return links;
    }

    public List<Packet> getActiveInFlightPackets() {
        return activeInFlightPackets;
    }

    public List<Packet> getPacketHistory() {
        return packetHistory;
    }

    public NetworkMetrics getMetrics() {
        return metrics;
    }

    public NatEngine getNatEngine() {
        return natEngine;
    }

    public FirewallEngine getFirewallEngine() {
        return firewallEngine;
    }

    public DnsService getDnsService() {
        return dnsService;
    }

    public Device findDeviceByName(String name) {
        for (Device d : devices) {
            if (d.getName().equalsIgnoreCase(name)) {
                return d;
            }
        }
        return null;
    }

    public Device findDeviceByIp(String ip) {
        for (Device d : devices) {
            if (d.hasIp(ip)) {
                return d;
            }
        }
        return null;
    }

    public NetworkLink findLinkBetween(Device a, Device b) {
        for (NetworkLink link : links) {
            Device devA = link.getInterfaceA() != null ? link.getInterfaceA().getOwnerDevice() : null;
            Device devB = link.getInterfaceB() != null ? link.getInterfaceB().getOwnerDevice() : null;
            if ((devA == a && devB == b) || (devA == b && devB == a)) {
                return link;
            }
        }
        return null;
    }

    public void recalculateRouting() {
        RoutingEngine.computeAllRoutes(devices, links);
        notifyTopologyChanged();
        log("Dynamic routing tables recomputed (Dijkstra SPF).");
    }

    public void toggleLinkState(NetworkLink link) {
        link.setUp(!link.isUp());
        log("Link " + link.getId() + " toggled: " + (link.isUp() ? "UP" : "DOWN"));
        recalculateRouting();
    }

    /**
     * Injects a packet into the network starting from source device
     */
    public boolean sendPacket(Packet packet) {
        Device srcDev = findDeviceByIp(packet.getSourceIp());
        if (srcDev == null) {
            srcDev = findDeviceByName(packet.getSourceIp());
        }
        if (srcDev == null) {
            dropPacket(packet, "Source device not found for IP: " + packet.getSourceIp());
            return false;
        }

        packet.addHop(srcDev.getName());
        packet.setCurrentDevice(srcDev);
        packet.setStatus(Packet.Status.IN_TRANSIT);

        metrics.recordPacketSent(packet.getProtocol(), packet.getPacketSize());
        packetHistory.add(packet);
        if (packetHistory.size() > 2000) {
            packetHistory.remove(0);
        }

        for (SimulationListener listener : listeners) {
            listener.onPacketCreated(packet);
        }

        log(String.format("Packet %s [%s] injected at %s (%s -> %s)",
                packet.getId(), packet.getProtocol().getCode(), srcDev.getName(), packet.getSourceIp(), packet.getDestIp()));

        // Forward from initial source
        routeFromDevice(srcDev, packet);
        return true;
    }

    private void routeFromDevice(Device currentDev, Packet packet) {
        if (!currentDev.isOnline()) {
            dropPacket(packet, "Device " + currentDev.getName() + " is offline");
            return;
        }

        // 1. Check if packet arrived at destination
        if (currentDev.hasIp(packet.getDestIp())) {
            deliverPacket(currentDev, packet);
            return;
        }

        // 2. Check TTL
        if (packet.getTtl() <= 0) {
            dropPacket(packet, "TTL Expired in transit at " + currentDev.getName());
            return;
        }

        // 3. Device specific routing / forwarding
        if (currentDev instanceof SwitchDevice sw) {
            handleSwitchForward(sw, packet);
        } else if (currentDev instanceof RouterDevice router) {
            handleRouterForward(router, packet);
        } else if (currentDev instanceof HostDevice host) {
            handleHostForward(host, packet);
        }
    }

    private void handleHostForward(HostDevice host, Packet packet) {
        NetworkInterface iface = host.getPrimaryInterface();
        if (iface == null || !iface.isUp()) {
            dropPacket(packet, "Host " + host.getName() + " interface is DOWN");
            return;
        }

        NetworkLink link = iface.getConnectedLink();
        if (link == null || !link.isUp()) {
            dropPacket(packet, "No physical link on " + host.getName());
            return;
        }

        Device nextDev = link.getOtherDevice(host);
        if (nextDev == null) {
            dropPacket(packet, "Cable unplugged at " + host.getName());
            return;
        }

        // Learn ARP if direct subnet
        if (iface.isInSameSubnet(packet.getDestIp())) {
            packet.setDestMac("FF:FF:FF:FF:FF:00"); // Direct LAN frame
        } else {
            // Need default gateway
            if (host.getDefaultGateway() == null) {
                dropPacket(packet, "No default gateway configured on " + host.getName());
                return;
            }
        }

        transmitOnLink(link, host, nextDev, packet);
    }

    private void handleSwitchForward(SwitchDevice sw, Packet packet) {
        // Learn source MAC on incoming link/port
        NetworkLink inLink = packet.getCurrentLink();
        if (inLink != null) {
            NetworkInterface inIface = (inLink.getInterfaceA().getOwnerDevice() == sw) ?
                    inLink.getInterfaceA() : inLink.getInterfaceB();
            if (inIface != null && packet.getSourceMac() != null) {
                sw.learnMac(packet.getSourceMac(), inIface.getName(), packet.getVlanId());
            }
        }

        // Find destination host / router link
        Device destDev = findDeviceByIp(packet.getDestIp());
        NetworkLink targetLink = null;
        Device targetNextHop = null;

        // 1. Check direct connection to switch
        if (destDev != null) {
            targetLink = findLinkBetween(sw, destDev);
            if (targetLink != null && targetLink.isUp()) {
                targetNextHop = destDev;
            }
        }

        // 2. If not directly attached, find uplink Router connected to switch
        if (targetLink == null) {
            for (NetworkInterface swIface : sw.getInterfaces()) {
                NetworkLink link = swIface.getConnectedLink();
                if (link != null && link.isUp() && link != inLink) {
                    Device other = link.getOtherDevice(sw);
                    if (other instanceof RouterDevice) {
                        targetLink = link;
                        targetNextHop = other;
                        break;
                    }
                }
            }
        }

        // 3. Fallback: flood out of another port if not inLink
        if (targetLink == null) {
            for (NetworkInterface swIface : sw.getInterfaces()) {
                NetworkLink link = swIface.getConnectedLink();
                if (link != null && link.isUp() && link != inLink) {
                    Device other = link.getOtherDevice(sw);
                    if (other != null) {
                        targetLink = link;
                        targetNextHop = other;
                        break;
                    }
                }
            }
        }

        if (targetLink == null || !targetLink.isUp() || targetNextHop == null) {
            dropPacket(packet, "Switch " + sw.getName() + " forwarding failed (no active port)");
            return;
        }

        transmitOnLink(targetLink, sw, targetNextHop, packet);
    }

    private void handleRouterForward(RouterDevice router, Packet packet) {
        packet.decrementTtl();

        // 1. Firewall ACL evaluation
        if (router.isFirewallEnabled()) {
            FirewallEngine.FirewallResult fwResult = firewallEngine.evaluate(packet);
            if (!fwResult.isAllowed()) {
                dropPacket(packet, "Firewall Rule #" +
                        (fwResult.getMatchedRule() != null ? fwResult.getMatchedRule().getRuleNumber() : "Default") +
                        " BLOCKED packet at " + router.getName());
                return;
            }
        }

        // 2. Inbound NAT check (WAN to LAN response)
        if (router.isNatEnabled()) {
            natEngine.applyInboundNat(packet);
        }

        // 3. Routing Table Lookup
        RouteEntry route = router.findBestRoute(packet.getDestIp());
        if (route == null) {
            dropPacket(packet, "No route to destination " + packet.getDestIp() + " at " + router.getName());
            return;
        }

        NetworkInterface outIface = router.getInterfaceByName(route.getOutgoingInterface());
        if (outIface == null || !outIface.isUp()) {
            dropPacket(packet, "Outgoing interface " + route.getOutgoingInterface() + " is DOWN on " + router.getName());
            return;
        }

        NetworkLink link = outIface.getConnectedLink();
        if (link == null || !link.isUp()) {
            dropPacket(packet, "Outgoing link is DOWN on " + router.getName());
            return;
        }

        // 4. Outbound NAT check (LAN to WAN translation)
        if (router.isNatEnabled() && link.getAreaType() == AreaType.WAN) {
            natEngine.applyOutboundNat(packet, outIface.getIpAddress());
        }

        Device nextDev = link.getOtherDevice(router);
        if (nextDev == null) {
            dropPacket(packet, "No neighbor device on interface " + outIface.getName());
            return;
        }

        transmitOnLink(link, router, nextDev, packet);
    }

    private void transmitOnLink(NetworkLink link, Device fromDev, Device toDev, Packet packet) {
        // Check link packet loss simulation
        if (link.getPacketLossRate() > 0 && Math.random() < link.getPacketLossRate()) {
            dropPacket(packet, String.format("Packet lost on link %s (loss rate %.1f%%)", link.getId(), link.getPacketLossRate() * 100));
            return;
        }

        packet.setCurrentLink(link);
        packet.setCurrentDevice(fromDev);
        packet.setProgressOnLink(0.0);

        if (!activeInFlightPackets.contains(packet)) {
            activeInFlightPackets.add(packet);
        }

        log(String.format("Link Transmission: %s -> %s over %s [%s, latency=%.1fms]",
                fromDev.getName(), toDev.getName(), link.getId(), link.getAreaType().name(), link.getLatencyMs()));
    }

    private void deliverPacket(Device destDev, Packet packet) {
        packet.setStatus(Packet.Status.DELIVERED);
        packet.setCurrentDevice(destDev);
        packet.setCurrentLink(null);
        packet.setProgressOnLink(1.0);
        activeInFlightPackets.remove(packet);

        long latency = System.currentTimeMillis() - packet.getCreationTime();
        metrics.recordPacketDelivered(latency);

        packet.addHop(destDev.getName());

        for (SimulationListener listener : listeners) {
            listener.onPacketDelivered(packet);
        }

        log(String.format("SUCCESS: Packet %s DELIVERED at %s (%s). End-to-End Latency: %d ms, Hops: %s",
                packet.getId(), destDev.getName(), destDev.getPrimaryIp(), latency, String.join(" -> ", packet.getHopPath())));

        // Handle Application Layer Automated Replies
        processServerReply(destDev, packet);
    }

    private void dropPacket(Packet packet, String reason) {
        packet.setDropReason(reason);
        packet.setStatus(Packet.Status.DROPPED);
        activeInFlightPackets.remove(packet);
        metrics.recordPacketDropped();

        for (SimulationListener listener : listeners) {
            listener.onPacketDropped(packet, reason);
        }

        log(String.format("DROP: Packet %s DROPPED. Reason: %s", packet.getId(), reason));
    }

    private void processServerReply(Device destDev, Packet packet) {
        // Automatic ICMP Ping Echo Reply
        if (packet.getProtocol() == ProtocolType.ICMP && packet.getPayload().contains("Echo Request")) {
            Packet reply = Packet.createPingReply(packet.getDestIp(), packet.getSourceIp(), packet.getPingSequence());
            reply.setSourceMac(destDev.getPrimaryMac());
            // Delay slightly for realism
            CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> sendPacket(reply));
        }
        // Automatic HTTP Server Reply
        else if (packet.getProtocol() == ProtocolType.HTTP && destDev instanceof ServerDevice server && server.isHttpServiceRunning()) {
            if ("GET".equalsIgnoreCase(packet.getHttpMethod())) {
                String page = server.getWebPage(packet.getHttpUrl() != null ? packet.getHttpUrl() : "/");
                Packet httpReply = Packet.createHttpResponse(packet.getDestIp(), packet.getSourceIp(), 200, page);
                CompletableFuture.delayedExecutor(150, TimeUnit.MILLISECONDS).execute(() -> sendPacket(httpReply));
            }
        }
        // Automatic DNS Server Reply
        else if (packet.getProtocol() == ProtocolType.DNS && destDev instanceof ServerDevice server && server.isDnsServiceRunning()) {
            String domain = packet.getDnsQuery();
            String resolved = dnsService.resolve(domain);
            if (resolved != null) {
                Packet dnsReply = Packet.createDnsResponse(packet.getDestIp(), packet.getSourceIp(), domain, resolved);
                CompletableFuture.delayedExecutor(80, TimeUnit.MILLISECONDS).execute(() -> sendPacket(dnsReply));
            }
        }
    }

    private void startSimulationLoop() {
        executorService = Executors.newScheduledThreadPool(2);

        // Animation and packet advancement tick (every 30ms)
        simulationTask = executorService.scheduleAtFixedRate(() -> {
            if (!isRunning || isPaused) return;
            try {
                tickPackets();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 30, TimeUnit.MILLISECONDS);

        // Metrics sampler tick (every 1000ms)
        metricsTask = executorService.scheduleAtFixedRate(() -> {
            try {
                metrics.tickSample();
                for (SimulationListener listener : listeners) {
                    listener.onStatsUpdated(metrics);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void tickPackets() {
        for (Packet packet : activeInFlightPackets) {
            NetworkLink link = packet.getCurrentLink();
            if (link == null) continue;

            double speedFactor = simulationSpeed;
            // Advance progress based on link latency (higher latency = slower packet movement)
            double step = (0.05 * speedFactor) / Math.max(link.getLatencyMs() / 10.0, 0.5);
            double newProgress = packet.getProgressOnLink() + step;

            if (newProgress >= 1.0) {
                // Packet arrived at end of link
                Device fromDev = packet.getCurrentDevice();
                Device nextDev = link.getOtherDevice(fromDev);
                packet.setProgressOnLink(1.0);
                activeInFlightPackets.remove(packet);

                if (nextDev != null) {
                    packet.addHop(nextDev.getName());
                    packet.setCurrentDevice(nextDev);
                    routeFromDevice(nextDev, packet);
                } else {
                    dropPacket(packet, "Link leads to nowhere");
                }
            } else {
                packet.setProgressOnLink(newProgress);
                for (SimulationListener listener : listeners) {
                    listener.onPacketMoved(packet, packet.getCurrentDevice() != null ? packet.getCurrentDevice().getName() : "?",
                            link.getOtherDevice(packet.getCurrentDevice()) != null ? link.getOtherDevice(packet.getCurrentDevice()).getName() : "?",
                            newProgress);
                }
            }
        }
    }

    public void play() {
        this.isPaused = false;
        log("Simulation resumed.");
    }

    public void pause() {
        this.isPaused = true;
        log("Simulation paused.");
    }

    public void step() {
        if (!isPaused) pause();
        tickPackets();
        log("Simulation single-step executed.");
    }

    public void setSimulationSpeed(double speed) {
        this.simulationSpeed = Math.max(0.1, Math.min(10.0, speed));
        log(String.format("Simulation speed set to %.1fx", this.simulationSpeed));
    }

    public double getSimulationSpeed() {
        return simulationSpeed;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void log(String message) {
        for (SimulationListener listener : listeners) {
            listener.onLogMessage(message);
        }
    }

    private void notifyTopologyChanged() {
        for (SimulationListener listener : listeners) {
            listener.onTopologyChanged();
        }
    }

    public void reset() {
        activeInFlightPackets.clear();
        packetHistory.clear();
        metrics.reset();
        for (Device d : devices) {
            d.getArpTable().clear();
            if (d instanceof SwitchDevice sw) {
                sw.clearMacTable();
            }
        }
        recalculateRouting();
        log("Simulation state reset.");
    }

    public void shutdown() {
        isRunning = false;
        if (simulationTask != null) simulationTask.cancel(true);
        if (metricsTask != null) metricsTask.cancel(true);
        if (executorService != null) executorService.shutdown();
    }
}
