(function(exports) {
    class Device {
        constructor(id, name, areaType, x = 0, y = 0) {
            this.id = id;
            this.name = name;
            this.areaType = areaType;
            this.x = x;
            this.y = y;
            this.interfaces = [];
            this.arpTable = new Map(); // ip -> { ip, mac, interfaceName, isStatic, timestamp }
            this.isOnline = true;
            this.description = '';
        }

        addInterface(iface) {
            iface.ownerDevice = this;
            this.interfaces.push(iface);
        }

        getInterfaceByName(name) {
            return this.interfaces.find(i => i.name.toLowerCase() === name.toLowerCase()) || null;
        }

        getInterfaceByIp(ip) {
            return this.interfaces.find(i => i.ipAddress === ip) || null;
        }

        getPrimaryInterface() {
            return this.interfaces.length > 0 ? this.interfaces[0] : null;
        }

        getPrimaryIp() {
            for (const iface of this.interfaces) {
                if (iface.ipAddress) return iface.ipAddress;
            }
            return 'Unassigned';
        }

        getPrimaryMac() {
            for (const iface of this.interfaces) {
                if (iface.macAddress) return iface.macAddress;
            }
            return '00:00:00:00:00:00';
        }

        updateArpTable(ip, mac, ifaceName) {
            this.arpTable.set(ip, {
                ip, mac, interfaceName: ifaceName, isStatic: false, timestamp: Date.now()
            });
        }

        lookupArp(ip) {
            const entry = this.arpTable.get(ip);
            return entry ? entry.mac : null;
        }

        hasIp(ip) {
            return this.interfaces.some(i => i.ipAddress === ip);
        }

        getDeviceType() {
            return 'Generic Device';
        }
    }

    exports.Device = Device;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
