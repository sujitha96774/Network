(function(exports) {
    const AreaType = {
        LAN: {
            id: 'LAN',
            name: 'Local Area Network',
            description: 'Departmental Subnets & Access Layer (CS, Admin, Research, Data Center)',
            color: '#3b82f6',      // Blue
            bgColor: 'rgba(59, 130, 246, 0.12)',
            borderColor: '#3b82f6',
            shortName: 'LAN'
        },
        MAN: {
            id: 'MAN',
            name: 'Metropolitan Area Network',
            description: 'Campus High-Speed Optical Fiber Ring Backbone (10 Gbps)',
            color: '#10b981',      // Emerald Green
            bgColor: 'rgba(16, 185, 129, 0.12)',
            borderColor: '#10b981',
            shortName: 'MAN'
        },
        WAN: {
            id: 'WAN',
            name: 'Wide Area Network',
            description: 'ISP Core, Public Cloud Datacenter & Remote Branch Campus',
            color: '#f59e0b',      // Amber
            bgColor: 'rgba(245, 158, 11, 0.12)',
            borderColor: '#f59e0b',
            shortName: 'WAN'
        }
    };

    exports.AreaType = AreaType;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
