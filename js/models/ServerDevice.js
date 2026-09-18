(function(exports) {
    const HostDevice = (typeof require !== 'undefined') ? require('./HostDevice').HostDevice : exports.HostDevice;

    class ServerDevice extends HostDevice {
        constructor(id, name, areaType, x, y, department = 'Data Center') {
            super(id, name, areaType, x, y, department);
            this.httpServiceRunning = true;
            this.dnsServiceRunning = false;
            this.dhcpServiceRunning = false;
            this.hostedWebPages = new Map();
            this.dnsRecords = new Map();

            this.hostedWebPages.set('/', '<html><head><title>Campus Portal</title></head><body><h1>Welcome to University Campus Network</h1><p>Active Services: Web, DNS, Library Portal, Student ERP</p></body></html>');
        }

        addWebPage(path, html) {
            this.hostedWebPages.set(path, html);
        }

        getWebPage(path) {
            return this.hostedWebPages.get(path) || '<html><body><h1>404 Not Found</h1></body></html>';
        }

        addDnsRecord(domain, ip) {
            this.dnsRecords.set(domain.toLowerCase(), ip);
        }

        resolveDns(domain) {
            if (!domain) return null;
            return this.dnsRecords.get(domain.toLowerCase()) || null;
        }

        getDeviceType() {
            return 'Server';
        }
    }

    exports.ServerDevice = ServerDevice;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
