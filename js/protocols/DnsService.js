(function(exports) {
    class DnsService {
        constructor() {
            this.records = new Map();
            this.records.set('portal.campus.edu', '10.0.1.50');
            this.records.set('library.campus.edu', '10.0.1.51');
            this.records.set('dns.campus.edu', '10.0.1.10');
            this.records.set('cloud.remote.com', '198.51.100.10');
            this.records.set('google.com', '8.8.8.8');
        }

        addRecord(domain, ip) {
            this.records.set(domain.toLowerCase(), ip);
        }

        resolve(domain) {
            if (!domain) return null;
            return this.records.get(domain.toLowerCase()) || null;
        }
    }

    class DhcpService {
        constructor(subnetPrefix, subnetMask, defaultGateway, dnsServer) {
            this.subnetPrefix = subnetPrefix; // e.g. "192.168.1."
            this.subnetMask = subnetMask;
            this.defaultGateway = defaultGateway;
            this.dnsServer = dnsServer;
            this.nextHostId = 10;
            this.leasedIps = new Map(); // mac -> ip
        }

        leaseIp(mac) {
            if (this.leasedIps.has(mac)) {
                return {
                    ip: this.leasedIps.get(mac),
                    mask: this.subnetMask,
                    gateway: this.defaultGateway,
                    dns: this.dnsServer
                };
            }
            const allocatedIp = this.subnetPrefix + (this.nextHostId++);
            this.leasedIps.set(mac, allocatedIp);
            return {
                ip: allocatedIp,
                mask: this.subnetMask,
                gateway: this.defaultGateway,
                dns: this.dnsServer
            };
        }
    }

    exports.DnsService = DnsService;
    exports.DhcpService = DhcpService;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
