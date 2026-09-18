(function(exports) {
    const Device = (typeof require !== 'undefined') ? require('./Device').Device : exports.Device;

    class HostDevice extends Device {
        constructor(id, name, areaType, x, y, department = 'General') {
            super(id, name, areaType, x, y);
            this.department = department;
            this.defaultGateway = null;
            this.dnsServerIp = null;
            this.dhcpEnabled = false;
        }

        getDeviceType() {
            return 'Host / PC';
        }
    }

    exports.HostDevice = HostDevice;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
