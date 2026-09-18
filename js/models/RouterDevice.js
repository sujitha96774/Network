(function(exports) {
    const Device = (typeof require !== 'undefined') ? require('./Device').Device : exports.Device;
    const NetworkInterface = (typeof require !== 'undefined') ? require('./NetworkInterface').NetworkInterface : exports.NetworkInterface;

    class RouteEntry {
        constructor(destinationNetwork, subnetMask, nextHopIp, outgoingInterface, metric, protocol = 'STATIC') {
            this.destinationNetwork = destinationNetwork;
            this.subnetMask = subnetMask;
            this.nextHopIp = nextHopIp;
            this.outgoingInterface = outgoingInterface;
            this.metric = metric;
            this.protocol = protocol;
        }

        matches(targetIp) {
            if (this.destinationNetwork === '0.0.0.0' && this.subnetMask === '0.0.0.0') {
                return true;
            }
            try {
                const target = NetworkInterface.ipToLong(targetIp);
                const net = NetworkInterface.ipToLong(this.destinationNetwork);
                const mask = NetworkInterface.ipToLong(this.subnetMask);
                return (target & mask) === (net & mask);
            } catch (e) {
                return false;
            }
        }

        getPrefixLength() {
            return NetworkInterface.getPrefixLength(this.subnetMask);
        }
    }

    class RouterDevice extends Device {
        constructor(id, name, areaType, x, y) {
            super(id, name, areaType, x, y);
            this.routingTable = [];
            this.natEnabled = false;
            this.natOutsideInterface = null;
            this.firewallEnabled = false;
        }

        addRoute(route) {
            this.routingTable.push(route);
        }

        clearRoutes() {
            this.routingTable = [];
        }

        findBestRoute(destIp) {
            let bestMatch = null;
            let longestPrefix = -1;

            for (const entry of this.routingTable) {
                if (entry.matches(destIp)) {
                    const prefix = entry.getPrefixLength();
                    if (prefix > longestPrefix) {
                        longestPrefix = prefix;
                        bestMatch = entry;
                    } else if (prefix === longestPrefix && bestMatch) {
                        if (entry.metric < bestMatch.metric) {
                            bestMatch = entry;
                        }
                    }
                }
            }
            return bestMatch;
        }

        getDeviceType() {
            return 'L3 Router';
        }
    }

    exports.RouteEntry = RouteEntry;
    exports.RouterDevice = RouterDevice;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
