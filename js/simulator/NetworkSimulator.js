(function(exports) {
    const ProtocolType = (typeof require !== 'undefined') ? require('../models/ProtocolType').ProtocolType : exports.ProtocolType;
    const Packet = (typeof require !== 'undefined') ? require('../models/Packet').Packet : exports.Packet;
    const RoutingEngine = (typeof require !== 'undefined') ? require('../protocols/RoutingEngine').RoutingEngine : exports.RoutingEngine;
    const NatEngine = (typeof require !== 'undefined') ? require('../protocols/NatEngine').NatEngine : exports.NatEngine;
    const FirewallEngine = (typeof require !== 'undefined') ? require('../protocols/FirewallEngine').FirewallEngine : exports.FirewallEngine;
    const DnsService = (typeof require !== 'undefined') ? require('../protocols/DnsService').DnsService : exports.DnsService;
    const NetworkMetrics = (typeof require !== 'undefined') ? require('./NetworkMetrics').NetworkMetrics : exports.NetworkMetrics;

    class NetworkSimulator {
        constructor() {
            this.devices = [];
            this.links = [];
            this.activeInFlightPackets = [];
            this.packetHistory = [];
            this.listeners = [];

            this.metrics = new NetworkMetrics();
            this.natEngine = new NatEngine();
            this.firewallEngine = new FirewallEngine();
            this.dnsService = new DnsService();

            this.isRunning = true;
            this.isPaused = false;
            this.simulationSpeed = 1.0; // 0.2x to 5.0x

            this.startLoop();
        }

        addListener(listener) {
            this.listeners.push(listener);
        }

        removeListener(listener) {
            this.listeners = this.listeners.filter(l => l !== listener);
        }

        addDevice(device) {
            this.devices.push(device);
            this.notify('onTopologyChanged');
        }

        addLink(link) {
            this.links.push(link);
            this.notify('onTopologyChanged');
        }

        findDeviceByName(name) {
            if (!name) return null;
            return this.devices.find(d => d.name.toLowerCase() === name.toLowerCase()) || null;
        }

        findDeviceByIp(ip) {
            if (!ip) return null;
            return this.devices.find(d => d.hasIp(ip)) || null;
        }

        findLinkBetween(a, b) {
            for (const link of this.links) {
                const devA = link.interfaceA ? link.interfaceA.ownerDevice : null;
                const devB = link.interfaceB ? link.interfaceB.ownerDevice : null;
                if ((devA === a && devB === b) || (devA === b && devB === a)) {
                    return link;
                }
            }
            return null;
        }

        recalculateRouting() {
            RoutingEngine.computeAllRoutes(this.devices, this.links);
            this.notify('onTopologyChanged');
            this.log('Dynamic routing tables recomputed (Dijkstra SPF).');
        }

        toggleLinkState(link) {
            link.isUp = !link.isUp;
            this.log(`Link ${link.id} toggled: ${link.isUp ? 'UP' : 'DOWN'}`);
            this.recalculateRouting();
        }

        sendPacket(packet) {
            let srcDev = this.findDeviceByIp(packet.sourceIp);
            if (!srcDev) {
                srcDev = this.findDeviceByName(packet.sourceIp);
            }
            if (!srcDev) {
                this.dropPacket(packet, `Source device not found for IP: ${packet.sourceIp}`);
                return false;
            }

            packet.addHop(srcDev.name);
            packet.currentDevice = srcDev;
            packet.status = 'IN_TRANSIT';

            this.metrics.recordPacketSent(packet.protocol, packet.getPacketSize());
            this.packetHistory.push(packet);
            if (this.packetHistory.length > 2000) {
                this.packetHistory.shift();
            }

            this.notify('onPacketCreated', packet);
            this.log(`Packet ${packet.id} [${packet.protocol ? packet.protocol.code : 'DATA'}] injected at ${srcDev.name} (${packet.sourceIp} -> ${packet.destIp})`);

            this.routeFromDevice(srcDev, packet);
            return true;
        }

        routeFromDevice(currentDev, packet) {
            if (!currentDev.isOnline) {
                this.dropPacket(packet, `Device ${currentDev.name} is offline`);
                return;
            }

            // 1. Destination reached?
            if (currentDev.hasIp(packet.destIp)) {
                this.deliverPacket(currentDev, packet);
                return;
            }

            // 2. TTL Check
            if (packet.ttl <= 0) {
                this.dropPacket(packet, `TTL Expired in transit at ${currentDev.name}`);
                return;
            }

            // 3. Device specific forwarding
            const type = currentDev.getDeviceType();
            if (type === 'L2 Switch') {
                this.handleSwitchForward(currentDev, packet);
            } else if (type === 'L3 Router') {
                this.handleRouterForward(currentDev, packet);
            } else {
                this.handleHostForward(currentDev, packet);
            }
        }

        handleHostForward(host, packet) {
            const iface = host.getPrimaryInterface();
            if (!iface || !iface.isUp) {
                this.dropPacket(packet, `Host ${host.name} interface is DOWN`);
                return;
            }

            const link = iface.connectedLink;
            if (!link || !link.isUp) {
                this.dropPacket(packet, `No physical link on ${host.name}`);
                return;
            }

            const nextDev = link.getOtherDevice(host);
            if (!nextDev) {
                this.dropPacket(packet, `Cable unplugged at ${host.name}`);
                return;
            }

            if (!iface.isInSameSubnet(packet.destIp)) {
                if (!host.defaultGateway) {
                    this.dropPacket(packet, `No default gateway configured on ${host.name}`);
                    return;
                }
            }

            this.transmitOnLink(link, host, nextDev, packet);
        }

        handleSwitchForward(sw, packet) {
            const inLink = packet.currentLink;
            if (inLink) {
                const inIface = (inLink.interfaceA.ownerDevice === sw) ? inLink.interfaceA : inLink.interfaceB;
                if (inIface && packet.sourceMac) {
                    sw.learnMac(packet.sourceMac, inIface.name, packet.vlanId);
                }
            }

            const destDev = this.findDeviceByIp(packet.destIp);
            let targetLink = null;
            let targetNextHop = null;

            // 1. Check direct connection to switch
            if (destDev) {
                targetLink = this.findLinkBetween(sw, destDev);
                if (targetLink && targetLink.isUp) {
                    targetNextHop = destDev;
                }
            }

            // 2. Uplink to Router
            if (!targetLink) {
                for (const swIface of sw.interfaces) {
                    const link = swIface.connectedLink;
                    if (link && link.isUp && link !== inLink) {
                        const other = link.getOtherDevice(sw);
                        if (other && other.getDeviceType() === 'L3 Router') {
                            targetLink = link;
                            targetNextHop = other;
                            break;
                        }
                    }
                }
            }

            // 3. Flooding fallback
            if (!targetLink) {
                for (const swIface of sw.interfaces) {
                    const link = swIface.connectedLink;
                    if (link && link.isUp && link !== inLink) {
                        const other = link.getOtherDevice(sw);
                        if (other) {
                            targetLink = link;
                            targetNextHop = other;
                            break;
                        }
                    }
                }
            }

            if (!targetLink || !targetLink.isUp || !targetNextHop) {
                this.dropPacket(packet, `Switch ${sw.name} forwarding failed (no active port)`);
                return;
            }

            this.transmitOnLink(targetLink, sw, targetNextHop, packet);
        }

        handleRouterForward(router, packet) {
            packet.decrementTtl();

            // 1. Firewall ACL
            if (router.firewallEnabled) {
                const fwResult = this.firewallEngine.evaluate(packet);
                if (!fwResult.allowed) {
                    this.dropPacket(packet, `Firewall Rule #${fwResult.matchedRule ? fwResult.matchedRule.ruleNumber : 'Default'} BLOCKED packet at ${router.name}`);
                    return;
                }
            }

            // 2. Inbound NAT check (WAN to LAN response)
            if (router.natEnabled) {
                this.natEngine.applyInboundNat(packet);
            }

            // 3. Routing Table Lookup
            const route = router.findBestRoute(packet.destIp);
            if (!route) {
                this.dropPacket(packet, `No route to destination ${packet.destIp} at ${router.name}`);
                return;
            }

            const outIface = router.getInterfaceByName(route.outgoingInterface);
            if (!outIface || !outIface.isUp) {
                this.dropPacket(packet, `Outgoing interface ${route.outgoingInterface} is DOWN on ${router.name}`);
                return;
            }

            const link = outIface.connectedLink;
            if (!link || !link.isUp) {
                this.dropPacket(packet, `Outgoing link is DOWN on ${router.name}`);
                return;
            }

            // 4. Outbound NAT check (LAN to WAN translation)
            if (router.natEnabled && link.areaType && link.areaType.id === 'WAN') {
                this.natEngine.applyOutboundNat(packet, outIface.ipAddress);
            }

            const nextDev = link.getOtherDevice(router);
            if (!nextDev) {
                this.dropPacket(packet, `No neighbor device on interface ${outIface.name}`);
                return;
            }

            this.transmitOnLink(link, router, nextDev, packet);
        }

        transmitOnLink(link, fromDev, toDev, packet) {
            if (link.packetLossRate > 0 && Math.random() < link.packetLossRate) {
                this.dropPacket(packet, `Packet lost on link ${link.id} (loss rate ${(link.packetLossRate * 100).toFixed(1)}%)`);
                return;
            }

            packet.currentLink = link;
            packet.currentDevice = fromDev;
            packet.progressOnLink = 0.0;

            if (!this.activeInFlightPackets.includes(packet)) {
                this.activeInFlightPackets.push(packet);
            }

            this.log(`Link Transmission: ${fromDev.name} -> ${toDev.name} over ${link.id} [${link.areaType ? link.areaType.id : 'LINK'}, delay=${link.latencyMs}ms]`);
        }

        deliverPacket(destDev, packet) {
            packet.status = 'DELIVERED';
            packet.currentDevice = destDev;
            packet.currentLink = null;
            packet.progressOnLink = 1.0;
            this.activeInFlightPackets = this.activeInFlightPackets.filter(p => p !== packet);

            const latency = Date.now() - packet.creationTime;
            this.metrics.recordPacketDelivered(latency);
            packet.addHop(destDev.name);

            this.notify('onPacketDelivered', packet);
            this.log(`SUCCESS: Packet ${packet.id} DELIVERED at ${destDev.name} (${destDev.getPrimaryIp()}). Latency: ${latency} ms, Hops: ${packet.hopPath.join(' -> ')}`);

            this.processServerReply(destDev, packet);
        }

        dropPacket(packet, reason) {
            packet.dropReason = reason;
            packet.status = 'DROPPED';
            this.activeInFlightPackets = this.activeInFlightPackets.filter(p => p !== packet);
            this.metrics.recordPacketDropped();

            this.notify('onPacketDropped', packet, reason);
            this.log(`DROP: Packet ${packet.id} DROPPED. Reason: ${reason}`);
        }

        processServerReply(destDev, packet) {
            const protoCode = packet.protocol ? packet.protocol.code : '';
            // ICMP Ping Echo Reply
            if (protoCode === 'ICMP' && packet.payload.includes('Echo Request')) {
                const reply = Packet.createPingReply(packet.destIp, packet.sourceIp, packet.pingSequence);
                reply.sourceMac = destDev.getPrimaryMac();
                setTimeout(() => this.sendPacket(reply), 100);
            }
            // HTTP Web Server Response
            else if (protoCode === 'HTTP' && destDev.getDeviceType() === 'Server' && destDev.httpServiceRunning) {
                if (packet.httpMethod === 'GET') {
                    const page = destDev.getWebPage(packet.httpUrl || '/');
                    const httpReply = Packet.createHttpResponse(packet.destIp, packet.sourceIp, 200, page);
                    setTimeout(() => this.sendPacket(httpReply), 150);
                }
            }
            // DNS Server Response
            else if (protoCode === 'DNS' && destDev.getDeviceType() === 'Server' && destDev.dnsServiceRunning) {
                const domain = packet.dnsQuery;
                const resolved = this.dnsService.resolve(domain);
                if (resolved) {
                    const dnsReply = Packet.createDnsResponse(packet.destIp, packet.sourceIp, domain, resolved);
                    setTimeout(() => this.sendPacket(dnsReply), 80);
                }
            }
        }

        startLoop() {
            // Animation Tick Loop (every 30ms)
            this.simInterval = setInterval(() => {
                if (!this.isRunning || this.isPaused) return;
                this.tickPackets();
            }, 30);

            // Metrics Tick Loop (every 1000ms)
            this.metricsInterval = setInterval(() => {
                this.metrics.tickSample();
                this.notify('onStatsUpdated', this.metrics);
            }, 1000);
        }

        tickPackets() {
            for (const packet of [...this.activeInFlightPackets]) {
                const link = packet.currentLink;
                if (!link) continue;

                const speedFactor = this.simulationSpeed;
                const step = (0.05 * speedFactor) / Math.max(link.latencyMs / 10.0, 0.5);
                const newProgress = packet.progressOnLink + step;

                if (newProgress >= 1.0) {
                    const fromDev = packet.currentDevice;
                    const nextDev = link.getOtherDevice(fromDev);
                    packet.progressOnLink = 1.0;
                    this.activeInFlightPackets = this.activeInFlightPackets.filter(p => p !== packet);

                    if (nextDev) {
                        packet.addHop(nextDev.name);
                        packet.currentDevice = nextDev;
                        this.routeFromDevice(nextDev, packet);
                    } else {
                        this.dropPacket(packet, 'Link leads to nowhere');
                    }
                } else {
                    packet.progressOnLink = newProgress;
                    this.notify('onPacketMoved', packet);
                }
            }
        }

        play() {
            this.isPaused = false;
            this.log('Simulation resumed.');
        }

        pause() {
            this.isPaused = true;
            this.log('Simulation paused.');
        }

        step() {
            if (!this.isPaused) this.pause();
            this.tickPackets();
            this.log('Simulation single-step executed.');
        }

        setSimulationSpeed(speed) {
            this.simulationSpeed = Math.max(0.1, Math.min(10.0, speed));
            this.log(`Simulation speed set to ${this.simulationSpeed.toFixed(1)}x`);
        }

        log(msg) {
            this.notify('onLogMessage', msg);
        }

        notify(event, ...args) {
            for (const listener of this.listeners) {
                if (typeof listener[event] === 'function') {
                    listener[event](...args);
                }
            }
        }

        reset() {
            this.activeInFlightPackets = [];
            this.packetHistory = [];
            this.metrics.reset();
            for (const d of this.devices) {
                d.arpTable.clear();
                if (d.clearMacTable) d.clearMacTable();
            }
            this.recalculateRouting();
            this.log('Simulation reset.');
        }

        shutdown() {
            this.isRunning = false;
            if (this.simInterval) clearInterval(this.simInterval);
            if (this.metricsInterval) clearInterval(this.metricsInterval);
        }
    }

    exports.NetworkSimulator = NetworkSimulator;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
