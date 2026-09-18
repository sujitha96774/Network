(function(exports) {
    class NetworkLink {
        constructor(id, interfaceA, interfaceB, areaType, bandwidthMbps = 1000, latencyMs = 1, packetLossRate = 0) {
            this.id = id;
            this.interfaceA = interfaceA;
            this.interfaceB = interfaceB;
            this.areaType = areaType; // LAN, MAN, WAN
            this.bandwidthMbps = bandwidthMbps;
            this.latencyMs = latencyMs;
            this.packetLossRate = packetLossRate;
            this.isUp = true;

            if (interfaceA) interfaceA.connectedLink = this;
            if (interfaceB) interfaceB.connectedLink = this;
        }

        getOtherInterface(iface) {
            if (iface === this.interfaceA) return this.interfaceB;
            if (iface === this.interfaceB) return this.interfaceA;
            return null;
        }

        getOtherDevice(dev) {
            if (this.interfaceA && this.interfaceA.ownerDevice === dev) {
                return this.interfaceB ? this.interfaceB.ownerDevice : null;
            }
            if (this.interfaceB && this.interfaceB.ownerDevice === dev) {
                return this.interfaceA ? this.interfaceA.ownerDevice : null;
            }
            return null;
        }

        getRoutingCost() {
            if (!this.isUp) return Infinity;
            return this.latencyMs + (1000.0 / Math.max(this.bandwidthMbps, 1.0));
        }
    }

    exports.NetworkLink = NetworkLink;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
