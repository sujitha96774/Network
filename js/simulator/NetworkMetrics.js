(function(exports) {
    class NetworkMetrics {
        constructor() {
            this.totalPacketsSent = 0;
            this.totalPacketsDelivered = 0;
            this.totalPacketsDropped = 0;
            this.totalBytesTransferred = 0;
            this.protocolCounts = new Map();
            this.latencySamples = [];
            this.throughputHistory = new Array(30).fill(0);
            this.latencyHistory = new Array(30).fill(0);
            this.lastThroughputBytes = 0;
        }

        recordPacketSent(protocol, bytes = 64) {
            this.totalPacketsSent++;
            this.totalBytesTransferred += bytes;
            const code = protocol ? protocol.code : 'OTHER';
            this.protocolCounts.set(code, (this.protocolCounts.get(code) || 0) + 1);
        }

        recordPacketDelivered(latencyMs) {
            this.totalPacketsDelivered++;
            this.latencySamples.push(latencyMs);
            if (this.latencySamples.length > 500) {
                this.latencySamples.shift();
            }
        }

        recordPacketDropped() {
            this.totalPacketsDropped++;
        }

        tickSample() {
            const currentBytes = this.totalBytesTransferred;
            const diffBytes = currentBytes - this.lastThroughputBytes;
            this.lastThroughputBytes = currentBytes;
            const kbps = Math.round((diffBytes * 8) / 1000);

            this.throughputHistory.push(kbps);
            if (this.throughputHistory.length > 50) {
                this.throughputHistory.shift();
            }

            const avgLat = this.getAverageLatency();
            this.latencyHistory.push(avgLat);
            if (this.latencyHistory.length > 50) {
                this.latencyHistory.shift();
            }
        }

        getDeliveryRate() {
            if (this.totalPacketsSent === 0) return 100.0;
            return (this.totalPacketsDelivered * 100.0) / this.totalPacketsSent;
        }

        getAverageLatency() {
            if (this.latencySamples.length === 0) return 0.0;
            const sum = this.latencySamples.reduce((a, b) => a + b, 0);
            return sum / this.latencySamples.length;
        }

        reset() {
            this.totalPacketsSent = 0;
            this.totalPacketsDelivered = 0;
            this.totalPacketsDropped = 0;
            this.totalBytesTransferred = 0;
            this.protocolCounts.clear();
            this.latencySamples = [];
            this.throughputHistory = new Array(30).fill(0);
            this.latencyHistory = new Array(30).fill(0);
            this.lastThroughputBytes = 0;
        }
    }

    exports.NetworkMetrics = NetworkMetrics;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
