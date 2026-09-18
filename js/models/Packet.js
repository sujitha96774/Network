(function(exports) {
    const ProtocolType = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType : exports.ProtocolType;

    class Packet {
        constructor(sourceIp, destIp, protocol = ProtocolType.ICMP, payload = '') {
            this.id = Math.random().toString(36).substring(2, 9).toUpperCase();
            this.creationTime = Date.now();

            // Layer 2
            this.sourceMac = null;
            this.destMac = null;
            this.vlanId = 1;

            // Layer 3
            this.sourceIp = sourceIp;
            this.destIp = destIp;
            this.ttl = 64;
            this.protocol = protocol;

            // Layer 4
            this.sourcePort = 0;
            this.destPort = 0;
            this.synFlag = false;
            this.ackFlag = false;
            this.finFlag = false;

            // Layer 7
            this.payload = payload;
            this.httpMethod = null;
            this.httpUrl = null;
            this.httpStatusCode = 0;
            this.dnsQuery = null;
            this.dnsResolvedIp = null;
            this.pingSequence = 0;

            // Status & Animation
            this.status = 'IN_TRANSIT'; // IN_TRANSIT, DELIVERED, DROPPED
            this.dropReason = null;
            this.hopPath = [];
            this.currentLink = null;
            this.currentDevice = null;
            this.progressOnLink = 0.0; // 0.0 to 1.0
        }

        static createPing(srcIp, dstIp, seq = 1) {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.ICMP : exports.ProtocolType.ICMP;
            const p = new Packet(srcIp, dstIp, proto, `PING Echo Request seq=${seq}`);
            p.pingSequence = seq;
            return p;
        }

        static createPingReply(srcIp, dstIp, seq = 1) {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.ICMP : exports.ProtocolType.ICMP;
            const p = new Packet(srcIp, dstIp, proto, `PING Echo Reply seq=${seq}`);
            p.pingSequence = seq;
            return p;
        }

        static createHttpRequest(srcIp, dstIp, url = '/') {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.HTTP : exports.ProtocolType.HTTP;
            const p = new Packet(srcIp, dstIp, proto, `GET ${url} HTTP/1.1`);
            p.sourcePort = Math.floor(Math.random() * 50000 + 1024);
            p.destPort = 80;
            p.httpMethod = 'GET';
            p.httpUrl = url;
            return p;
        }

        static createHttpResponse(srcIp, dstIp, statusCode = 200, body = '') {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.HTTP : exports.ProtocolType.HTTP;
            const p = new Packet(srcIp, dstIp, proto, `HTTP/1.1 ${statusCode} OK\n${body}`);
            p.sourcePort = 80;
            p.destPort = 8080;
            p.httpStatusCode = statusCode;
            return p;
        }

        static createDnsQuery(srcIp, dnsServerIp, domain) {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.DNS : exports.ProtocolType.DNS;
            const p = new Packet(srcIp, dnsServerIp, proto, `DNS Standard Query A ${domain}`);
            p.sourcePort = Math.floor(Math.random() * 50000 + 1024);
            p.destPort = 53;
            p.dnsQuery = domain;
            return p;
        }

        static createDnsResponse(dnsServerIp, clientIp, domain, resolvedIp) {
            const proto = (typeof require !== 'undefined') ? require('./ProtocolType').ProtocolType.DNS : exports.ProtocolType.DNS;
            const p = new Packet(dnsServerIp, clientIp, proto, `DNS Query Response: ${domain} -> ${resolvedIp}`);
            p.sourcePort = 53;
            p.dnsQuery = domain;
            p.dnsResolvedIp = resolvedIp;
            return p;
        }

        getPacketSize() {
            return 64 + (this.payload ? this.payload.length : 0);
        }

        decrementTtl() {
            return --this.ttl;
        }

        addHop(deviceName) {
            this.hopPath.push(deviceName);
        }
    }

    exports.Packet = Packet;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
