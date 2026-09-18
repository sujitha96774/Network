(function(exports) {
    const NetworkInterface = (typeof require !== 'undefined') ? require('../models/NetworkInterface').NetworkInterface : exports.NetworkInterface;

    class FirewallRule {
        constructor(ruleNumber, action, srcNetwork, dstNetwork, protocol = 'ANY', dstPort = 0, description = '') {
            this.ruleNumber = ruleNumber;
            this.action = action; // 'PERMIT' or 'DENY'
            this.srcNetwork = srcNetwork; // 'any' or '192.168.1.0/24'
            this.dstNetwork = dstNetwork;
            this.protocol = protocol;
            this.dstPort = dstPort;
            this.description = description;
        }

        matches(packet) {
            // Check protocol
            if (this.protocol !== 'ANY') {
                const protoCode = packet.protocol ? packet.protocol.code : '';
                if (protoCode === 'ICMP' && this.protocol !== 'ICMP') return false;
                if ((protoCode === 'HTTP' || protoCode === 'HTTPS') && this.protocol !== 'HTTP' && this.protocol !== 'TCP') return false;
                if (protoCode === 'DNS' && this.protocol !== 'DNS' && this.protocol !== 'UDP') return false;
            }

            // Check Dest Port
            if (this.dstPort > 0 && packet.destPort !== 0 && packet.destPort !== this.dstPort) {
                return false;
            }

            // Check Source Net
            if (this.srcNetwork !== 'any') {
                if (!this.matchesNetwork(packet.sourceIp, this.srcNetwork)) return false;
            }

            // Check Dest Net
            if (this.dstNetwork !== 'any') {
                if (!this.matchesNetwork(packet.destIp, this.dstNetwork)) return false;
            }

            return true;
        }

        matchesNetwork(ip, cidrOrNet) {
            if (!ip) return false;
            try {
                if (cidrOrNet.includes('/')) {
                    const [net, prefixStr] = cidrOrNet.split('/');
                    const prefix = parseInt(prefixStr, 10);
                    const mask = ((0xFFFFFFFF << (32 - prefix)) >>> 0);
                    const ipNum = NetworkInterface.ipToLong(ip);
                    const netNum = NetworkInterface.ipToLong(net);
                    return (ipNum & mask) === (netNum & mask);
                } else {
                    return ip === cidrOrNet;
                }
            } catch (e) {
                return false;
            }
        }
    }

    class FirewallEngine {
        constructor() {
            this.rules = [];
            this.defaultAction = 'PERMIT';
        }

        addRule(rule) {
            this.rules.push(rule);
        }

        clearRules() {
            this.rules = [];
        }

        evaluate(packet) {
            for (const rule of this.rules) {
                if (rule.matches(packet)) {
                    return { allowed: rule.action === 'PERMIT', matchedRule: rule };
                }
            }
            return { allowed: this.defaultAction === 'PERMIT', matchedRule: null };
        }
    }

    exports.FirewallRule = FirewallRule;
    exports.FirewallEngine = FirewallEngine;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
