(function(exports) {
    class NetworkInterface {
        constructor(name, macAddress, ipAddress = null, subnetMask = null, ownerDevice = null) {
            this.name = name; // e.g., "eth0", "gi0/1"
            this.macAddress = macAddress;
            this.ipAddress = ipAddress;
            this.subnetMask = subnetMask;
            this.isUp = true;
            this.connectedLink = null;
            this.ownerDevice = ownerDevice;
        }

        static ipToLong(ip) {
            if (!ip) return 0;
            const parts = ip.trim().split('.');
            let res = 0;
            for (let i = 0; i < 4; i++) {
                res = ((res << 8) | (parseInt(parts[i], 10) & 0xFF)) >>> 0;
            }
            return res;
        }

        static longToIp(num) {
            return [
                (num >>> 24) & 0xFF,
                (num >>> 16) & 0xFF,
                (num >>> 8) & 0xFF,
                num & 0xFF
            ].join('.');
        }

        static getNetworkAddress(ip, mask) {
            if (!ip || !mask) return '0.0.0.0';
            const ipNum = NetworkInterface.ipToLong(ip);
            const maskNum = NetworkInterface.ipToLong(mask);
            return NetworkInterface.longToIp((ipNum & maskNum) >>> 0);
        }

        static getPrefixLength(mask) {
            if (!mask || mask === '0.0.0.0') return 0;
            let num = NetworkInterface.ipToLong(mask);
            let count = 0;
            while (num) {
                count += num & 1;
                num >>>= 1;
            }
            return count;
        }

        isInSameSubnet(otherIp) {
            if (!this.ipAddress || !this.subnetMask || !otherIp) return false;
            const thisNet = NetworkInterface.getNetworkAddress(this.ipAddress, this.subnetMask);
            const otherNet = NetworkInterface.getNetworkAddress(otherIp, this.subnetMask);
            return thisNet === otherNet;
        }

        isConfigured() {
            return !!this.ipAddress;
        }

        isActive() {
            return this.isUp && (!this.connectedLink || this.connectedLink.isUp);
        }
    }

    exports.NetworkInterface = NetworkInterface;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
