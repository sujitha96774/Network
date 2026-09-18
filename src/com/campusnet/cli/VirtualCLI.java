package com.campusnet.cli;

import com.campusnet.model.*;
import com.campusnet.protocol.FirewallEngine;
import com.campusnet.protocol.NatEngine;
import com.campusnet.simulator.NetworkSimulator;
import com.campusnet.simulator.TrafficGenerator;

import java.util.HashSet;
import java.util.Set;

public class VirtualCLI {
    private final NetworkSimulator simulator;
    private final TrafficGenerator trafficGenerator;

    public VirtualCLI(NetworkSimulator simulator) {
        this.simulator = simulator;
        this.trafficGenerator = new TrafficGenerator(simulator);
    }

    public String executeCommand(Device device, String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return "";
        }

        String cmd = commandLine.trim();
        String[] tokens = cmd.split("\\s+");
        String base = tokens[0].toLowerCase();

        switch (base) {
            case "help":
            case "?":
                return getHelpText(device);

            case "ipconfig":
            case "ifconfig":
            case "show":
                if (tokens.length > 1 && ("ip".equalsIgnoreCase(tokens[1]) || "interface".equalsIgnoreCase(tokens[1]) || "interfaces".equalsIgnoreCase(tokens[1]))) {
                    if (tokens.length > 2 && "route".equalsIgnoreCase(tokens[2])) {
                        return handleShowRoute(device);
                    }
                    return handleShowInterfaces(device);
                } else if (tokens.length > 1 && ("mac".equalsIgnoreCase(tokens[1]) || "mac-table".equalsIgnoreCase(tokens[1]) || "mac-address-table".equalsIgnoreCase(tokens[1]))) {
                    return handleShowMac(device);
                } else if (tokens.length > 1 && "arp".equalsIgnoreCase(tokens[1])) {
                    return handleShowArp(device);
                } else if (tokens.length > 1 && "nat".equalsIgnoreCase(tokens[1])) {
                    return handleShowNat();
                } else if (tokens.length > 1 && "firewall".equalsIgnoreCase(tokens[1])) {
                    return handleShowFirewall();
                }
                return handleShowInterfaces(device);

            case "ping":
                if (tokens.length < 2) {
                    return "Usage: ping <destination_ip_or_device_name>";
                }
                return handlePing(device, tokens[1]);

            case "traceroute":
            case "tracert":
                if (tokens.length < 2) {
                    return "Usage: traceroute <destination_ip>";
                }
                return handleTraceroute(device, tokens[1]);

            case "curl":
            case "http":
                if (tokens.length < 2) {
                    return "Usage: curl <url_or_ip>";
                }
                return handleCurl(device, tokens[1]);

            case "nslookup":
                if (tokens.length < 2) {
                    return "Usage: nslookup <domain_name>";
                }
                return handleNslookup(tokens[1]);

            case "arp":
                return handleShowArp(device);

            case "route":
                return handleShowRoute(device);

            case "link":
                if (tokens.length < 3) {
                    return "Usage: link <link_id> <up|down>";
                }
                return handleLinkToggle(tokens[1], tokens[2]);

            case "clear":
            case "cls":
                return "__CLEAR__";

            default:
                return "Unknown command: '" + cmd + "'. Type 'help' for a list of available commands.";
        }
    }

    private String getHelpText(Device device) {
        StringBuilder sb = new StringBuilder();
        sb.append("===============================================================\n");
        sb.append("     CAMPUS NETWORK CLI - DEVICE: ").append(device.getName()).append("\n");
        sb.append("===============================================================\n");
        sb.append("Commands Available:\n");
        sb.append("  ping <ip|host>         - Send ICMP Echo Request packets\n");
        sb.append("  traceroute <ip>        - Trace path and hop-by-hop latency\n");
        sb.append("  ipconfig / ifconfig    - Display interface IP, MAC, Subnet Mask\n");
        sb.append("  show ip route          - Display device Routing Table\n");
        sb.append("  show mac-table         - Display Switch MAC Address Table\n");
        sb.append("  show arp               - Display ARP Cache table\n");
        sb.append("  curl <url|ip>          - Send HTTP GET request to web server\n");
        sb.append("  nslookup <domain>      - Resolve DNS domain name to IP\n");
        sb.append("  show nat               - Display active NAT translation sessions\n");
        sb.append("  show firewall          - Display active Firewall ACL rules\n");
        sb.append("  link <id> <up|down>    - Manually toggle link state for testing\n");
        sb.append("  clear                  - Clear terminal screen\n");
        sb.append("===============================================================\n");
        return sb.toString();
    }

    private String handleShowInterfaces(Device device) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Interface Configuration for %s (%s):\n", device.getName(), device.getDeviceType()));
        sb.append(String.format("%-12s %-16s %-16s %-20s %-8s\n", "Interface", "IP Address", "Subnet Mask", "MAC Address", "Status"));
        sb.append("--------------------------------------------------------------------------------\n");
        for (NetworkInterface iface : device.getInterfaces()) {
            sb.append(String.format("%-12s %-16s %-16s %-20s %-8s\n",
                    iface.getName(),
                    iface.getIpAddress() != null ? iface.getIpAddress() : "Unassigned",
                    iface.getSubnetMask() != null ? iface.getSubnetMask() : "N/A",
                    iface.getMacAddress(),
                    iface.isUp() ? "UP" : "DOWN"));
        }
        if (device instanceof HostDevice host) {
            sb.append("\nDefault Gateway : ").append(host.getDefaultGateway() != null ? host.getDefaultGateway() : "None").append("\n");
            sb.append("DNS Server      : ").append(host.getDnsServerIp() != null ? host.getDnsServerIp() : "None").append("\n");
        }
        return sb.toString();
    }

    private String handleShowRoute(Device device) {
        if (device instanceof RouterDevice router) {
            StringBuilder sb = new StringBuilder();
            sb.append("IP Routing Table for ").append(router.getName()).append(":\n");
            sb.append(String.format("%-18s %-18s %-16s %-10s %-8s %-8s\n",
                    "Network Destination", "Netmask", "Gateway/NextHop", "Interface", "Metric", "Protocol"));
            sb.append("------------------------------------------------------------------------------------\n");
            for (RouteEntry entry : router.getRoutingTable()) {
                sb.append(String.format("%-18s %-18s %-16s %-10s %-8d %-8s\n",
                        entry.getDestinationNetwork(), entry.getSubnetMask(), entry.getNextHopIp(),
                        entry.getOutgoingInterface(), entry.getMetric(), entry.getProtocol()));
            }
            return sb.toString();
        } else if (device instanceof HostDevice host) {
            return "Host Routing:\n" +
                    "Destination     Netmask         Gateway         Interface\n" +
                    "0.0.0.0         0.0.0.0         " + (host.getDefaultGateway() != null ? host.getDefaultGateway() : "None") + "      eth0\n";
        }
        return "Routing table is not applicable for L2 Switch.";
    }

    private String handleShowMac(Device device) {
        if (device instanceof SwitchDevice sw) {
            StringBuilder sb = new StringBuilder();
            sb.append("MAC Address Table for ").append(sw.getName()).append(":\n");
            sb.append(String.format("%-6s %-20s %-10s %-12s\n", "VLAN", "MAC Address", "Type", "Ports"));
            sb.append("--------------------------------------------------\n");
            for (MacTableEntry entry : sw.getMacTable().values()) {
                sb.append(String.format("%-6d %-20s %-10s %-12s\n",
                        entry.getVlanId(), entry.getMacAddress(), "DYNAMIC", entry.getPortName()));
            }
            if (sw.getMacTable().isEmpty()) {
                sb.append("(Table is currently empty - send traffic to populate)\n");
            }
            return sb.toString();
        }
        return "Device is not a Switch. Use 'show arp' on hosts/routers.";
    }

    private String handleShowArp(Device device) {
        StringBuilder sb = new StringBuilder();
        sb.append("ARP Cache Table for ").append(device.getName()).append(":\n");
        sb.append(String.format("%-18s %-20s %-12s %-10s\n", "Internet Address", "Physical Address", "Type", "Interface"));
        sb.append("---------------------------------------------------------------\n");
        for (ArpTableEntry entry : device.getArpTable().values()) {
            sb.append(String.format("%-18s %-20s %-12s %-10s\n",
                    entry.getIpAddress(), entry.getMacAddress(), entry.isStatic() ? "static" : "dynamic", entry.getInterfaceName()));
        }
        if (device.getArpTable().isEmpty()) {
            sb.append("(No ARP entries cached yet)\n");
        }
        return sb.toString();
    }

    private String handleShowNat() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active NAT Translation Sessions (Campus Border Gateway):\n");
        sb.append(String.format("%-25s %-25s %-25s\n", "Inside Local", "Inside Global (WAN)", "Outside Destination"));
        sb.append("--------------------------------------------------------------------------------\n");
        for (NatEngine.NatSession s : simulator.getNatEngine().getOutboundSessions().values()) {
            sb.append(String.format("%-25s %-25s %-25s\n",
                    s.getInsideLocalIp() + ":" + s.getInsideLocalPort(),
                    s.getInsideGlobalIp() + ":" + s.getInsideGlobalPort(),
                    s.getOutsideGlobalIp() + ":" + s.getOutsideGlobalPort()));
        }
        if (simulator.getNatEngine().getOutboundSessions().isEmpty()) {
            sb.append("(No active NAT sessions)\n");
        }
        return sb.toString();
    }

    private String handleShowFirewall() {
        StringBuilder sb = new StringBuilder();
        sb.append("Campus Border Firewall Access Control List (ACL):\n");
        sb.append(String.format("%-6s %-8s %-8s %-18s %-18s %-8s %-20s\n",
                "Rule#", "Action", "Proto", "Source Net", "Dest Net", "Port", "Description"));
        sb.append("------------------------------------------------------------------------------------------------\n");
        for (FirewallEngine.Rule r : simulator.getFirewallEngine().getRules()) {
            sb.append(String.format("%-6d %-8s %-8s %-18s %-18s %-8s %-20s\n",
                    r.getRuleNumber(), r.getAction(), r.getProtocol(),
                    r.getSrcNetwork(), r.getDstNetwork(),
                    r.getDstPort() > 0 ? String.valueOf(r.getDstPort()) : "any",
                    r.getDescription()));
        }
        return sb.toString();
    }

    private String handlePing(Device device, String target) {
        String targetIp = resolveTargetIp(target);
        String srcIp = device.getPrimaryIp();

        if ("Unassigned".equals(srcIp)) {
            return "Error: Source device has no IP address assigned.";
        }

        trafficGenerator.sendPing(srcIp, targetIp, 4);
        return String.format("Pinging %s [%s] from %s with 32 bytes of data...\n(Packets dispatched into simulator. Watch animated packet flow on canvas.)",
                target, targetIp, srcIp);
    }

    private String handleTraceroute(Device device, String target) {
        String targetIp = resolveTargetIp(target);
        StringBuilder sb = new StringBuilder();
        sb.append("Tracing route to ").append(target).append(" [").append(targetIp).append("] over maximum 30 hops:\n\n");

        Device current = device;
        int hop = 1;
        sb.append(String.format("%2d  %-20s  %-16s  %s\n", hop++, current.getName(), current.getPrimaryIp(), "0.0 ms [Local Start]"));

        // Simulate path discovery using routing tables
        Set<Device> visited = new HashSet<>();
        visited.add(current);

        while (current != null && !current.hasIp(targetIp) && hop < 15) {
            Device nextHop = null;
            double latency = 1.0;
            String linkType = "LAN";

            if (current instanceof RouterDevice rtr) {
                RouteEntry re = rtr.findBestRoute(targetIp);
                if (re != null) {
                    NetworkInterface outIface = rtr.getInterfaceByName(re.getOutgoingInterface());
                    if (outIface != null && outIface.getConnectedLink() != null && outIface.getConnectedLink().isUp()) {
                        NetworkLink link = outIface.getConnectedLink();
                        nextHop = link.getOtherDevice(rtr);
                        latency = link.getLatencyMs();
                        linkType = link.getAreaType().name();
                    }
                }
            } else if (current instanceof HostDevice host) {
                NetworkInterface iface = host.getPrimaryInterface();
                if (iface != null && iface.getConnectedLink() != null) {
                    nextHop = iface.getConnectedLink().getOtherDevice(host);
                }
            } else if (current instanceof SwitchDevice sw) {
                Device targetDev = simulator.findDeviceByIp(targetIp);
                if (targetDev != null && simulator.findLinkBetween(sw, targetDev) != null) {
                    nextHop = targetDev;
                } else {
                    for (NetworkInterface iface : sw.getInterfaces()) {
                        if (iface.getConnectedLink() != null && iface.getConnectedLink().getOtherDevice(sw) instanceof RouterDevice) {
                            nextHop = iface.getConnectedLink().getOtherDevice(sw);
                            break;
                        }
                    }
                }
            }

            if (nextHop == null || visited.contains(nextHop)) {
                sb.append(String.format("%2d  * * * Request timed out / Destination unreachable\n", hop));
                break;
            }

            visited.add(nextHop);
            sb.append(String.format("%2d  %-20s  %-16s  ~%.1f ms [%s]\n",
                    hop++, nextHop.getName(), nextHop.getPrimaryIp(), latency, linkType));
            current = nextHop;
        }

        if (current != null && current.hasIp(targetIp)) {
            sb.append("\nTrace complete. Destination reached successfully.\n");
        }
        return sb.toString();
    }

    private String handleCurl(Device device, String urlOrIp) {
        String targetIp = resolveTargetIp(urlOrIp);
        trafficGenerator.sendHttpRequest(device.getPrimaryIp(), targetIp, "/");
        return "HTTP GET request sent to " + targetIp + ":80...\n(Packet dispatched. HTTP 200 response will be returned by the server.)";
    }

    private String handleNslookup(String domain) {
        String ip = simulator.getDnsService().resolve(domain);
        if (ip != null) {
            return "Server:   Campus-DNS-Server (10.0.1.10)\nAddress:  10.0.1.10#53\n\nName:     " + domain + "\nAddress:  " + ip;
        }
        return "** server can't find " + domain + ": Non-existent domain";
    }

    private String handleLinkToggle(String linkId, String state) {
        for (NetworkLink link : simulator.getLinks()) {
            if (link.getId().equalsIgnoreCase(linkId)) {
                boolean up = "up".equalsIgnoreCase(state);
                link.setUp(up);
                simulator.recalculateRouting();
                return "Link " + link.getId() + " is now " + (up ? "UP" : "DOWN") + ". Dynamic routes recomputed.";
            }
        }
        return "Link with ID '" + linkId + "' not found.";
    }

    private String resolveTargetIp(String input) {
        if (input.startsWith("http://")) {
            input = input.substring(7);
        }
        if (input.contains("/")) {
            input = input.substring(0, input.indexOf("/"));
        }

        // Check if device name
        Device d = simulator.findDeviceByName(input);
        if (d != null) {
            return d.getPrimaryIp();
        }

        // Check if domain
        String dnsIp = simulator.getDnsService().resolve(input);
        if (dnsIp != null) {
            return dnsIp;
        }

        return input; // Assume direct IP
    }
}
