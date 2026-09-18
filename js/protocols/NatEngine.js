(function(exports) {
    class NatSession {
        constructor(insideLocalIp, insideLocalPort, insideGlobalIp, insideGlobalPort, outsideGlobalIp, outsideGlobalPort) {
            this.insideLocalIp = insideLocalIp;
            this.insideLocalPort = insideLocalPort;
            this.insideGlobalIp = insideGlobalIp;
            this.insideGlobalPort = insideGlobalPort;
            this.outsideGlobalIp = outsideGlobalIp;
            this.outsideGlobalPort = outsideGlobalPort;
            this.createdTime = Date.now();
        }
    }

    class NatEngine {
        constructor() {
            this.outboundSessions = new Map(); // insideLocalIp:port -> NatSession
            this.inboundSessions = new Map();  // insideGlobalPort -> NatSession
            this.nextPort = 40000;
        }

        applyOutboundNat(packet, publicWanIp) {
            const key = `${packet.sourceIp}:${packet.sourcePort}`;
            let session = this.outboundSessions.get(key);

            if (!session) {
                const port = this.nextPort++;
                if (this.nextPort > 65000) this.nextPort = 40000;

                session = new NatSession(
                    packet.sourceIp, packet.sourcePort,
                    publicWanIp, port,
                    packet.destIp, packet.destPort
                );
                this.outboundSessions.set(key, session);
                this.inboundSessions.set(port, session);
            }

            packet.sourceIp = session.insideGlobalIp;
            packet.sourcePort = session.insideGlobalPort;
            return true;
        }

        applyInboundNat(packet) {
            const session = this.inboundSessions.get(packet.destPort);
            if (session) {
                packet.destIp = session.insideLocalIp;
                packet.destPort = session.insideLocalPort;
                return true;
            }
            return false;
        }

        clearSessions() {
            this.outboundSessions.clear();
            this.inboundSessions.clear();
        }
    }

    exports.NatSession = NatSession;
    exports.NatEngine = NatEngine;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
