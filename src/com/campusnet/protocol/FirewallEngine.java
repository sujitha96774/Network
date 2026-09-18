package com.campusnet.protocol;

import com.campusnet.model.NetworkInterface;
import com.campusnet.model.Packet;
import com.campusnet.model.ProtocolType;

import java.util.ArrayList;
import java.util.List;

public class FirewallEngine {
    public enum Action {
        PERMIT,
        DENY
    }

    public static class Rule {
        private final int ruleNumber;
        private final Action action;
        private final String srcNetwork; // "any" or "192.168.1.0/24"
        private final String dstNetwork; // "any" or "10.0.1.0/24"
        private final String protocol;   // "ANY", "ICMP", "TCP", "HTTP", "DNS"
        private final int dstPort;       // 0 for any
        private final String description;

        public Rule(int ruleNumber, Action action, String srcNetwork, String dstNetwork,
                    String protocol, int dstPort, String description) {
            this.ruleNumber = ruleNumber;
            this.action = action;
            this.srcNetwork = srcNetwork;
            this.dstNetwork = dstNetwork;
            this.protocol = protocol;
            this.dstPort = dstPort;
            this.description = description;
        }

        public boolean matches(Packet packet) {
            // Check protocol
            if (!"ANY".equalsIgnoreCase(protocol)) {
                if (packet.getProtocol() == ProtocolType.ICMP && !"ICMP".equalsIgnoreCase(protocol)) return false;
                if ((packet.getProtocol() == ProtocolType.HTTP || packet.getProtocol() == ProtocolType.HTTPS)
                        && !"HTTP".equalsIgnoreCase(protocol) && !"TCP".equalsIgnoreCase(protocol)) return false;
                if (packet.getProtocol() == ProtocolType.DNS && !"DNS".equalsIgnoreCase(protocol) && !"UDP".equalsIgnoreCase(protocol)) return false;
            }

            // Check Dest Port if specified
            if (dstPort > 0 && packet.getDestPort() != 0 && packet.getDestPort() != dstPort) {
                return false;
            }

            // Check Source Network
            if (!"any".equalsIgnoreCase(srcNetwork)) {
                if (!matchesNetwork(packet.getSourceIp(), srcNetwork)) {
                    return false;
                }
            }

            // Check Dest Network
            if (!"any".equalsIgnoreCase(dstNetwork)) {
                if (!matchesNetwork(packet.getDestIp(), dstNetwork)) {
                    return false;
                }
            }

            return true;
        }

        private boolean matchesNetwork(String ip, String cidrOrNet) {
            if (ip == null) return false;
            try {
                if (cidrOrNet.contains("/")) {
                    String[] parts = cidrOrNet.split("/");
                    String net = parts[0];
                    int prefix = Integer.parseInt(parts[1]);
                    long mask = (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
                    long ipVal = NetworkInterface.ipToLong(ip);
                    long netVal = NetworkInterface.ipToLong(net);
                    return (ipVal & mask) == (netVal & mask);
                } else {
                    return ip.equals(cidrOrNet);
                }
            } catch (Exception e) {
                return false;
            }
        }

        public int getRuleNumber() { return ruleNumber; }
        public Action getAction() { return action; }
        public String getSrcNetwork() { return srcNetwork; }
        public String getDstNetwork() { return dstNetwork; }
        public String getProtocol() { return protocol; }
        public int getDstPort() { return dstPort; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("%d: %s %s from %s to %s port %s - %s",
                    ruleNumber, action, protocol, srcNetwork, dstNetwork,
                    dstPort > 0 ? String.valueOf(dstPort) : "any", description);
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private Action defaultAction = Action.PERMIT;

    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void clearRules() {
        rules.clear();
    }

    public Action getDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(Action defaultAction) {
        this.defaultAction = defaultAction;
    }

    public FirewallResult evaluate(Packet packet) {
        for (Rule rule : rules) {
            if (rule.matches(packet)) {
                return new FirewallResult(rule.getAction() == Action.PERMIT, rule);
            }
        }
        return new FirewallResult(defaultAction == Action.PERMIT, null);
    }

    public static class FirewallResult {
        private final boolean allowed;
        private final Rule matchedRule;

        public FirewallResult(boolean allowed, Rule matchedRule) {
            this.allowed = allowed;
            this.matchedRule = matchedRule;
        }

        public boolean isAllowed() { return allowed; }
        public Rule getMatchedRule() { return matchedRule; }
    }
}
