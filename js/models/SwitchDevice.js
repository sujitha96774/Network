(function(exports) {
    const Device = (typeof require !== 'undefined') ? require('./Device').Device : exports.Device;

    class SwitchDevice extends Device {
        constructor(id, name, areaType, x, y) {
            super(id, name, areaType, x, y);
            this.macTable = new Map(); // mac -> { mac, portName, vlanId, timestamp }
            this.portVlanMap = new Map();
        }

        learnMac(mac, portName, vlanId = 1) {
            if (!mac || mac === 'FF:FF:FF:FF:FF:FF') return;
            this.macTable.set(mac.toUpperCase(), {
                mac: mac.toUpperCase(),
                portName,
                vlanId,
                timestamp: Date.now()
            });
        }

        lookupPort(mac) {
            if (!mac) return null;
            const entry = this.macTable.get(mac.toUpperCase());
            return entry ? entry.portName : null;
        }

        clearMacTable() {
            this.macTable.clear();
        }

        getDeviceType() {
            return 'L2 Switch';
        }
    }

    exports.SwitchDevice = SwitchDevice;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
