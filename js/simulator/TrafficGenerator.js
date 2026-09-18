(function(exports) {
    const Packet = (typeof require !== 'undefined') ? require('../models/Packet').Packet : exports.Packet;
    const ProtocolType = (typeof require !== 'undefined') ? require('../models/ProtocolType').ProtocolType : exports.ProtocolType;

    class TrafficGenerator {
        constructor(simulator) {
            this.simulator = simulator;
        }

        sendPing(srcIp, dstIp, count = 4) {
            for (let i = 1; i <= count; i++) {
                const seq = i;
                setTimeout(() => {
                    const p = Packet.createPing(srcIp, dstIp, seq);
                    this.simulator.sendPacket(p);
                }, (i - 1) * 800);
            }
        }

        sendHttpRequest(srcIp, dstIp, url = '/') {
            const syn = new Packet(srcIp, dstIp, ProtocolType.TCP_SYN, 'TCP SYN [Port 80]');
            syn.sourcePort = Math.floor(Math.random() * 50000 + 1024);
            syn.destPort = 80;
            syn.synFlag = true;
            this.simulator.sendPacket(syn);

            setTimeout(() => {
                const http = Packet.createHttpRequest(srcIp, dstIp, url);
                this.simulator.sendPacket(http);
            }, 400);
        }

        sendDnsQuery(srcIp, dnsServerIp, domain) {
            const dns = Packet.createDnsQuery(srcIp, dnsServerIp, domain);
            this.simulator.sendPacket(dns);
        }

        startContinuousTraffic(srcIp, dstIp, protocol = ProtocolType.TCP_SYN, ratePerSec = 5, durationSec = 10) {
            const delayMs = Math.max(50, Math.floor(1000 / ratePerSec));
            const endTime = Date.now() + (durationSec * 1000);
            let seq = 1;

            const interval = setInterval(() => {
                if (Date.now() >= endTime) {
                    clearInterval(interval);
                    return;
                }
                const p = new Packet(srcIp, dstIp, protocol, `Data Stream Payload #${seq++}`);
                p.sourcePort = 12345;
                p.destPort = 80;
                this.simulator.sendPacket(p);
            }, delayMs);
        }

        sendDdosBurst(srcIpPrefix, targetIp, packetCount = 15) {
            let i = 1;
            const interval = setInterval(() => {
                if (i > packetCount) {
                    clearInterval(interval);
                    return;
                }
                const spoofedSrc = srcIpPrefix + Math.floor(Math.random() * 250 + 1);
                const flood = new Packet(spoofedSrc, targetIp, ProtocolType.TCP_SYN, `SYN FLOOD Attack Frame #${i++}`);
                flood.sourcePort = Math.floor(Math.random() * 60000 + 1024);
                flood.destPort = 80;
                this.simulator.sendPacket(flood);
            }, 30);
        }
    }

    exports.TrafficGenerator = TrafficGenerator;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
