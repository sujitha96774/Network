(function(exports) {
    const ProtocolType = {
        ICMP: { code: 'ICMP', name: 'ICMP Ping / Echo', color: '#3b82f6', description: 'Network Control & Echo Diagnostics' },
        TCP_SYN: { code: 'TCP-SYN', name: 'TCP SYN', color: '#10b981', description: 'TCP Handshake Initiation' },
        TCP_ACK: { code: 'TCP-ACK', name: 'TCP ACK', color: '#34d399', description: 'TCP Acknowledgment' },
        HTTP: { code: 'HTTP', name: 'HTTP Web Request/Reply', color: '#f59e0b', description: 'Hypertext Transfer Protocol (Port 80)' },
        HTTPS: { code: 'HTTPS', name: 'HTTPS Secure Web', color: '#d97706', description: 'Encrypted Web Traffic (Port 443)' },
        DNS: { code: 'DNS', name: 'DNS Query/Response', color: '#a855f7', description: 'Domain Name System (Port 53)' },
        DHCP: { code: 'DHCP', name: 'DHCP IP Lease', color: '#ec4899', description: 'Dynamic Host Configuration Protocol' },
        FTP: { code: 'FTP', name: 'FTP File Transfer', color: '#0ea5e9', description: 'File Transfer Protocol (Port 21)' },
        ARP: { code: 'ARP', name: 'ARP Resolution', color: '#eab308', description: 'Address Resolution Protocol Broadcast' },
        DROPPED: { code: 'DROP', name: 'Dropped / Filtered', color: '#ef4444', description: 'Packet Dropped or Blocked by Firewall' }
    };

    exports.ProtocolType = ProtocolType;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
