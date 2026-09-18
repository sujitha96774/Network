(function(exports) {
    const TrafficGenerator = (typeof require !== 'undefined') ? require('../simulator/TrafficGenerator').TrafficGenerator : exports.TrafficGenerator;

    class VirtualCLI {
        constructor(simulator) {
            this.simulator = simulator;
            this.trafficGenerator = new TrafficGenerator(simulator);
        }

        executeCommand(device, commandLine) {
            if (!commandLine || !commandLine.trim()) return '';

            const cmd = commandLine.trim();
            const tokens = cmd.split(/\s+/);
            const base = tokens[0].toLowerCase();

            switch (base) {
                case 'help':
                case '?':
                    return this.getHelpText(device);

                case 'ipconfig':
                case 'ifconfig':
                case 'show':
                    if (tokens.length > 1 && (tokens[1].toLowerCase() === 'ip' || tokens[1].toLowerCase() === 'interface' || tokens[1].toLowerCase() === 'interfaces')) {
                        if (tokens.length > 2 && tokens[2].toLowerCase() === 'route') {
                            return this.handleShowRoute(device);
                        }
                        return this.handleShowInterfaces(device);
                    } else if (tokens.length > 1 && (tokens[1].toLowerCase() === 'mac' || tokens[1].toLowerCase() === 'mac-table' || tokens[1].toLowerCase() === 'mac-address-table')) {
                        return this.handleShowMac(device);
                    } else if (tokens.length > 1 && tokens[1].toLowerCase() === 'arp') {
                        return this.handleShowArp(device);
                    } else if (tokens.length > 1 && tokens[1].toLowerCase() === 'nat') {
                        return this.handleShowNat();
                    } else if (tokens.length > 1 && tokens[1].toLowerCase() === 'firewall') {
                        return this.handleShowFirewall();
                    }
                    return this.handleShowInterfaces(device);

                case 'ping':
                    if (tokens.length < 2) return 'Usage: ping <destination_ip_or_device_name>';
                    return this.handlePing(device, tokens[1]);

                case 'traceroute':
                case 'tracert':
                    if (tokens.length < 2) return 'Usage: traceroute <destination_ip>';
                    return this.handleTraceroute(device, tokens[1]);

                case 'curl':
                case 'http':
                    if (tokens.length < 2) return 'Usage: curl <url_or_ip>';
                    return this.handleCurl(device, tokens[1]);

                case 'nslookup':
                    if (tokens.length < 2) return 'Usage: nslookup <domain_name>';
                    return this.handleNslookup(tokens[1]);

                case 'arp':
                    return this.handleShowArp(device);

                case 'route':
                    return this.handleShowRoute(device);

                case 'link':
                    if (tokens.length < 3) return 'Usage: link <link_id> <up|down>';
                    return this.handleLinkToggle(tokens[1], tokens[2]);

                case 'clear':
                case 'cls':
                    return '__CLEAR__';

                default:
                    return `Unknown command: '${cmd}'. Type 'help' for a list of available commands.`;
            }
        }

        getHelpText(device) {
            return `===============================================================
     CAMPUS NETWORK CLI - DEVICE: ${device.name}
===============================================================
Commands Available:
  ping <ip|host>         - Send ICMP Echo Request packets
  traceroute <ip>        - Trace path and hop-by-hop latency
  ipconfig / ifconfig    - Display interface IP, MAC, Subnet Mask
  show ip route          - Display device Routing Table
  show mac-table         - Display Switch MAC Address Table
  show arp               - Display ARP Cache table
  curl <url|ip>          - Send HTTP GET request to web server
  nslookup <domain>      - Resolve DNS domain name to IP
  show nat               - Display active NAT translation sessions
  show firewall          - Display active Firewall ACL rules
  link <id> <up|down>    - Manually toggle link state for testing
  clear                  - Clear terminal screen
===============================================================`;
        }

        handleShowInterfaces(device) {
            let out = `Interface Configuration for ${device.name} (${device.getDeviceType()}):\n`;
            out += `Interface    IP Address       Subnet Mask      MAC Address          Status  \n`;
            out += `--------------------------------------------------------------------------------\n`;
            for (const iface of device.interfaces) {
                const ip = (iface.ipAddress || 'Unassigned').padEnd(16);
                const mask = (iface.subnetMask || 'N/A').padEnd(16);
                const mac = iface.macAddress.padEnd(20);
                const status = (iface.isUp ? 'UP' : 'DOWN').padEnd(8);
                out += `${iface.name.padEnd(12)} ${ip} ${mask} ${mac} ${status}\n`;
            }
            if (device.defaultGateway) {
                out += `\nDefault Gateway : ${device.defaultGateway}\n`;
                out += `DNS Server      : ${device.dnsServerIp || 'None'}\n`;
            }
            return out;
        }

        handleShowRoute(device) {
            if (device.getDeviceType() === 'L3 Router') {
                let out = `IP Routing Table for ${device.name}:\n`;
                out += `Network Destination Netmask            Gateway/NextHop  Interface  Metric   Protocol\n`;
                out += `------------------------------------------------------------------------------------\n`;
                for (const r of device.routingTable) {
                    out += `${r.destinationNetwork.padEnd(19)} ${r.subnetMask.padEnd(18)} ${r.nextHopIp.padEnd(16)} ${r.outgoingInterface.padEnd(10)} ${String(r.metric).padEnd(8)} ${r.protocol}\n`;
                }
                return out;
            } else if (device.defaultGateway) {
                return `Host Routing:\nDestination     Netmask         Gateway         Interface\n0.0.0.0         0.0.0.0         ${device.defaultGateway.padEnd(15)} eth0\n`;
            }
            return 'Routing table is not applicable for L2 Switch.';
        }

        handleShowMac(device) {
            if (device.getDeviceType() === 'L2 Switch') {
                let out = `MAC Address Table for ${device.name}:\n`;
                out += `VLAN   MAC Address          Type       Ports       \n`;
                out += `--------------------------------------------------\n`;
                for (const [mac, entry] of device.macTable.entries()) {
                    out += `${String(entry.vlanId).padEnd(6)} ${entry.mac.padEnd(20)} DYNAMIC    ${entry.portName}\n`;
                }
                if (device.macTable.size === 0) {
                    out += `(Table is currently empty - send traffic to populate)\n`;
                }
                return out;
            }
            return "Device is not a Switch. Use 'show arp' on hosts/routers.";
        }

        handleShowArp(device) {
            let out = `ARP Cache Table for ${device.name}:\n`;
            out += `Internet Address   Physical Address     Type         Interface \n`;
            out += `---------------------------------------------------------------\n`;
            for (const [ip, entry] of device.arpTable.entries()) {
                out += `${ip.padEnd(18)} ${entry.mac.padEnd(20)} dynamic      ${entry.interfaceName}\n`;
            }
            if (device.arpTable.size === 0) {
                out += `(No ARP entries cached yet)\n`;
            }
            return out;
        }

        handleShowNat() {
            let out = `Active NAT Translation Sessions (Campus Border Gateway):\n`;
            out += `Inside Local              Inside Global (WAN)       Outside Destination      \n`;
            out += `--------------------------------------------------------------------------------\n`;
            for (const s of this.simulator.natEngine.outboundSessions.values()) {
                const inLoc = `${s.insideLocalIp}:${s.insideLocalPort}`.padEnd(25);
                const inGlob = `${s.insideGlobalIp}:${s.insideGlobalPort}`.padEnd(25);
                const outDest = `${s.outsideGlobalIp}:${s.outsideGlobalPort}`.padEnd(25);
                out += `${inLoc} ${inGlob} ${outDest}\n`;
            }
            if (this.simulator.natEngine.outboundSessions.size === 0) {
                out += `(No active NAT sessions)\n`;
            }
            return out;
        }

        handleShowFirewall() {
            let out = `Campus Border Firewall Access Control List (ACL):\n`;
            out += `Rule#  Action   Proto    Source Net         Dest Net           Port     Description         \n`;
            out += `------------------------------------------------------------------------------------------------\n`;
            for (const r of this.simulator.firewallEngine.rules) {
                out += `${String(r.ruleNumber).padEnd(6)} ${r.action.padEnd(8)} ${r.protocol.padEnd(8)} ${r.srcNetwork.padEnd(18)} ${r.dstNetwork.padEnd(18)} ${String(r.dstPort || 'any').padEnd(8)} ${r.description}\n`;
            }
            return out;
        }

        handlePing(device, target) {
            const targetIp = this.resolveTargetIp(target);
            const srcIp = device.getPrimaryIp();
            if (srcIp === 'Unassigned') return 'Error: Source device has no IP address assigned.';

            this.trafficGenerator.sendPing(srcIp, targetIp, 4);
            return `Pinging ${target} [${targetIp}] from ${srcIp} with 32 bytes of data...\n(Packets dispatched into simulator. Watch animated packet flow on canvas.)`;
        }

        handleTraceroute(device, target) {
            const targetIp = this.resolveTargetIp(target);
            let out = `Tracing route to ${target} [${targetIp}] over maximum 30 hops:\n\n`;

            let current = device;
            let hop = 1;
            out += ` ${String(hop++).padStart(2)}  ${current.name.padEnd(20)}  ${current.getPrimaryIp().padEnd(16)}  0.0 ms [Local Start]\n`;

            const visited = new Set();
            visited.add(current);

            while (current && !current.hasIp(targetIp) && hop < 15) {
                let nextHop = null;
                let latency = 1.0;
                let linkType = 'LAN';

                if (current.getDeviceType() === 'L3 Router') {
                    const re = current.findBestRoute(targetIp);
                    if (re) {
                        const outIface = current.getInterfaceByName(re.outgoingInterface);
                        if (outIface && outIface.connectedLink && outIface.connectedLink.isUp) {
                            const link = outIface.connectedLink;
                            nextHop = link.getOtherDevice(current);
                            latency = link.latencyMs;
                            linkType = link.areaType ? link.areaType.id : 'LINK';
                        }
                    }
                } else if (current.getDeviceType() === 'L2 Switch') {
                    const targetDev = this.simulator.findDeviceByIp(targetIp);
                    const directLink = targetDev ? this.simulator.findLinkBetween(current, targetDev) : null;
                    if (directLink && directLink.isUp) {
                        nextHop = targetDev;
                        latency = directLink.latencyMs;
                        linkType = directLink.areaType ? directLink.areaType.id : 'LAN';
                    } else {
                        // Find connected router
                        for (const swIface of current.interfaces) {
                            const l = swIface.connectedLink;
                            if (l && l.isUp) {
                                const o = l.getOtherDevice(current);
                                if (o && o.getDeviceType() === 'L3 Router' && !visited.has(o)) {
                                    nextHop = o;
                                    latency = l.latencyMs;
                                    linkType = l.areaType ? l.areaType.id : 'LAN';
                                    break;
                                }
                            }
                        }
                    }
                } else if (current.getPrimaryInterface && current.getPrimaryInterface()) {
                    const iface = current.getPrimaryInterface();
                    if (iface && iface.connectedLink && iface.connectedLink.isUp) {
                        const link = iface.connectedLink;
                        nextHop = link.getOtherDevice(current);
                        latency = link.latencyMs;
                        linkType = link.areaType ? link.areaType.id : 'LAN';
                    }
                }

                if (!nextHop || visited.has(nextHop)) {
                    out += ` ${String(hop).padStart(2)}  * * * Request timed out / Destination unreachable\n`;
                    break;
                }

                visited.add(nextHop);
                out += ` ${String(hop++).padStart(2)}  ${nextHop.name.padEnd(20)}  ${nextHop.getPrimaryIp().padEnd(16)}  ~${latency.toFixed(1)} ms [${linkType}]\n`;
                current = nextHop;
            }

            if (current && current.hasIp(targetIp)) {
                out += `\nTrace complete. Destination reached successfully.\n`;
            }
            return out;
        }

        handleCurl(device, urlOrIp) {
            const targetIp = this.resolveTargetIp(urlOrIp);
            this.trafficGenerator.sendHttpRequest(device.getPrimaryIp(), targetIp, '/');
            return `HTTP GET request sent to ${targetIp}:80...\n(Packet dispatched. HTTP 200 response will be returned by server.)`;
        }

        handleNslookup(domain) {
            const ip = this.simulator.dnsService.resolve(domain);
            if (ip) {
                return `Server:   Campus-DNS-Server (10.0.1.10)\nAddress:  10.0.1.10#53\n\nName:     ${domain}\nAddress:  ${ip}`;
            }
            return `** server can't find ${domain}: Non-existent domain`;
        }

        handleLinkToggle(linkId, state) {
            const link = this.simulator.links.find(l => l.id.toLowerCase() === linkId.toLowerCase());
            if (link) {
                const up = (state.toLowerCase() === 'up');
                link.isUp = up;
                this.simulator.recalculateRouting();
                return `Link ${link.id} is now ${up ? 'UP' : 'DOWN'}. Dynamic routes recomputed.`;
            }
            return `Link with ID '${linkId}' not found.`;
        }

        resolveTargetIp(input) {
            let str = input;
            if (str.startsWith('http://')) str = str.substring(7);
            if (str.includes('/')) str = str.substring(0, str.indexOf('/'));

            const d = this.simulator.findDeviceByName(str);
            if (d) return d.getPrimaryIp();

            const dns = this.simulator.dnsService.resolve(str);
            if (dns) return dns;

            return str;
        }
    }

    exports.VirtualCLI = VirtualCLI;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
