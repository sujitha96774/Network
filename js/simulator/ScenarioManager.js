(function(exports) {
    const AreaType = (typeof require !== 'undefined') ? require('../models/AreaType').AreaType : exports.AreaType;
    const HostDevice = (typeof require !== 'undefined') ? require('../models/HostDevice').HostDevice : exports.HostDevice;
    const SwitchDevice = (typeof require !== 'undefined') ? require('../models/SwitchDevice').SwitchDevice : exports.SwitchDevice;
    const RouterDevice = (typeof require !== 'undefined') ? require('../models/RouterDevice').RouterDevice : exports.RouterDevice;
    const ServerDevice = (typeof require !== 'undefined') ? require('../models/ServerDevice').ServerDevice : exports.ServerDevice;
    const NetworkInterface = (typeof require !== 'undefined') ? require('../models/NetworkInterface').NetworkInterface : exports.NetworkInterface;
    const NetworkLink = (typeof require !== 'undefined') ? require('../models/NetworkLink').NetworkLink : exports.NetworkLink;
    const FirewallRule = (typeof require !== 'undefined') ? require('../protocols/FirewallEngine').FirewallRule : exports.FirewallRule;

    class ScenarioManager {
        static buildDefaultCampusTopology(simulator) {
            simulator.reset();
            simulator.devices = [];
            simulator.links = [];

            // ==========================================
            // 1. LAN 1: COMPUTER SCIENCE DEPARTMENT
            // ==========================================
            const swCs = new SwitchDevice('SW_CS', 'SW-CS-Dept', AreaType.LAN, 120, 160);
            swCs.description = 'CS Dept Access Switch (VLAN 10)';
            simulator.addDevice(swCs);

            const pcCs1 = new HostDevice('PC_CS_01', 'CS-Student-01', AreaType.LAN, 60, 60, 'Computer Science');
            pcCs1.description = 'CS Student Lab Workstation';
            const ifCs1 = new NetworkInterface('eth0', '00:1A:2B:3C:4D:01', '192.168.1.10', '255.255.255.0', pcCs1);
            pcCs1.addInterface(ifCs1);
            pcCs1.defaultGateway = '192.168.1.1';
            pcCs1.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcCs1);

            const pcCs2 = new HostDevice('PC_CS_02', 'CS-Student-02', AreaType.LAN, 180, 60, 'Computer Science');
            pcCs2.description = 'CS AI Research Terminal';
            const ifCs2 = new NetworkInterface('eth0', '00:1A:2B:3C:4D:02', '192.168.1.11', '255.255.255.0', pcCs2);
            pcCs2.addInterface(ifCs2);
            pcCs2.defaultGateway = '192.168.1.1';
            pcCs2.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcCs2);

            const swCsPort1 = new NetworkInterface('fa0/1', '00:11:22:33:44:01', null, null, swCs);
            swCs.addInterface(swCsPort1);
            simulator.addLink(new NetworkLink('LINK_CS1', ifCs1, swCsPort1, AreaType.LAN, 1000, 1, 0));

            const swCsPort2 = new NetworkInterface('fa0/2', '00:11:22:33:44:02', null, null, swCs);
            swCs.addInterface(swCsPort2);
            simulator.addLink(new NetworkLink('LINK_CS2', ifCs2, swCsPort2, AreaType.LAN, 1000, 1, 0));

            // ==========================================
            // 2. LAN 2: ADMIN & FINANCE DEPARTMENT
            // ==========================================
            const swAdmin = new SwitchDevice('SW_ADMIN', 'SW-Admin-Dept', AreaType.LAN, 120, 360);
            swAdmin.description = 'Admin & Finance Switch (VLAN 20)';
            simulator.addDevice(swAdmin);

            const pcAdm1 = new HostDevice('PC_ADM_01', 'Admin-Finance', AreaType.LAN, 60, 460, 'Administration');
            pcAdm1.description = 'Campus Payroll & Finance PC';
            const ifAdm1 = new NetworkInterface('eth0', '00:2A:3B:4C:5D:01', '192.168.2.10', '255.255.255.0', pcAdm1);
            pcAdm1.addInterface(ifAdm1);
            pcAdm1.defaultGateway = '192.168.2.1';
            pcAdm1.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcAdm1);

            const pcAdm2 = new HostDevice('PC_ADM_02', 'Admin-Registrar', AreaType.LAN, 180, 460, 'Administration');
            pcAdm2.description = 'Student Admissions & Records PC';
            const ifAdm2 = new NetworkInterface('eth0', '00:2A:3B:4C:5D:02', '192.168.2.11', '255.255.255.0', pcAdm2);
            pcAdm2.addInterface(ifAdm2);
            pcAdm2.defaultGateway = '192.168.2.1';
            pcAdm2.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcAdm2);

            const swAdmPort1 = new NetworkInterface('fa0/1', '00:22:33:44:55:01', null, null, swAdmin);
            swAdmin.addInterface(swAdmPort1);
            simulator.addLink(new NetworkLink('LINK_ADM1', ifAdm1, swAdmPort1, AreaType.LAN, 1000, 1, 0));

            const swAdmPort2 = new NetworkInterface('fa0/2', '00:22:33:44:55:02', null, null, swAdmin);
            swAdmin.addInterface(swAdmPort2);
            simulator.addLink(new NetworkLink('LINK_ADM2', ifAdm2, swAdmPort2, AreaType.LAN, 1000, 1, 0));

            // ==========================================
            // 3. MAIN CAMPUS BUILDING A CORE ROUTER
            // ==========================================
            const rtrBldgA = new RouterDevice('RTR_BLDG_A', 'RTR-Building-A', AreaType.MAN, 280, 260);
            rtrBldgA.description = 'Main Campus Academic Building Core Router';

            const rtrA_ifCs = new NetworkInterface('gi0/0', '00:AA:BB:CC:DD:01', '192.168.1.1', '255.255.255.0', rtrBldgA);
            rtrBldgA.addInterface(rtrA_ifCs);
            const swCsUplink = new NetworkInterface('gi0/1', '00:11:22:33:44:03', null, null, swCs);
            swCs.addInterface(swCsUplink);
            simulator.addLink(new NetworkLink('LINK_CS_RTR', swCsUplink, rtrA_ifCs, AreaType.LAN, 1000, 1, 0));

            const rtrA_ifAdm = new NetworkInterface('gi0/1', '00:AA:BB:CC:DD:02', '192.168.2.1', '255.255.255.0', rtrBldgA);
            rtrBldgA.addInterface(rtrA_ifAdm);
            const swAdmUplink = new NetworkInterface('gi0/1', '00:22:33:44:55:03', null, null, swAdmin);
            swAdmin.addInterface(swAdmUplink);
            simulator.addLink(new NetworkLink('LINK_ADM_RTR', swAdmUplink, rtrA_ifAdm, AreaType.LAN, 1000, 1, 0));

            simulator.addDevice(rtrBldgA);

            // ==========================================
            // 4. LAN 3: RESEARCH LAB (BUILDING B)
            // ==========================================
            const swRes = new SwitchDevice('SW_RES', 'SW-Research-Lab', AreaType.LAN, 640, 160);
            swRes.description = 'Engineering & Nanotech Research Switch';
            simulator.addDevice(swRes);

            const pcRes1 = new HostDevice('PC_RES_01', 'Research-WS-01', AreaType.LAN, 570, 60, 'Research');
            const ifRes1 = new NetworkInterface('eth0', '00:3A:4B:5C:6D:01', '192.168.3.10', '255.255.255.0', pcRes1);
            pcRes1.addInterface(ifRes1);
            pcRes1.defaultGateway = '192.168.3.1';
            pcRes1.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcRes1);

            const pcRes2 = new HostDevice('PC_RES_02', 'Research-WS-02', AreaType.LAN, 710, 60, 'Research');
            const ifRes2 = new NetworkInterface('eth0', '00:3A:4B:5C:6D:02', '192.168.3.11', '255.255.255.0', pcRes2);
            pcRes2.addInterface(ifRes2);
            pcRes2.defaultGateway = '192.168.3.1';
            pcRes2.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcRes2);

            const swResPort1 = new NetworkInterface('fa0/1', '00:33:44:55:66:01', null, null, swRes);
            swRes.addInterface(swResPort1);
            simulator.addLink(new NetworkLink('LINK_RES1', ifRes1, swResPort1, AreaType.LAN, 1000, 1, 0));

            const swResPort2 = new NetworkInterface('fa0/2', '00:33:44:55:66:02', null, null, swRes);
            swRes.addInterface(swResPort2);
            simulator.addLink(new NetworkLink('LINK_RES2', ifRes2, swResPort2, AreaType.LAN, 1000, 1, 0));

            // ==========================================
            // 5. LAN 4: CENTRAL DATA CENTER
            // ==========================================
            const swDc = new SwitchDevice('SW_DC', 'SW-DataCenter', AreaType.LAN, 640, 360);
            swDc.description = 'Campus Core Data Center High-Speed Switch';
            simulator.addDevice(swDc);

            const srvWeb = new ServerDevice('SRV_WEB', 'Campus-Portal-Web', AreaType.LAN, 570, 460, 'Data Center');
            srvWeb.description = 'University Web Portal & LMS Server (HTTP port 80)';
            srvWeb.httpServiceRunning = true;
            const ifWeb = new NetworkInterface('eth0', '00:4A:5B:6C:7D:01', '10.0.1.50', '255.255.255.0', srvWeb);
            srvWeb.addInterface(ifWeb);
            srvWeb.defaultGateway = '10.0.1.1';
            srvWeb.dnsServerIp = '10.0.1.10';
            simulator.addDevice(srvWeb);

            const srvDns = new ServerDevice('SRV_DNS', 'Campus-DNS-Server', AreaType.LAN, 710, 460, 'Data Center');
            srvDns.description = 'Central Campus DNS Server (Port 53)';
            srvDns.dnsServiceRunning = true;
            const ifDns = new NetworkInterface('eth0', '00:4A:5B:6C:7D:02', '10.0.1.10', '255.255.255.0', srvDns);
            srvDns.addInterface(ifDns);
            srvDns.defaultGateway = '10.0.1.1';
            simulator.addDevice(srvDns);

            const swDcPort1 = new NetworkInterface('gi0/1', '00:44:55:66:77:01', null, null, swDc);
            swDc.addInterface(swDcPort1);
            simulator.addLink(new NetworkLink('LINK_DC1', ifWeb, swDcPort1, AreaType.LAN, 10000, 0.5, 0));

            const swDcPort2 = new NetworkInterface('gi0/2', '00:44:55:66:77:02', null, null, swDc);
            swDc.addInterface(swDcPort2);
            simulator.addLink(new NetworkLink('LINK_DC2', ifDns, swDcPort2, AreaType.LAN, 10000, 0.5, 0));

            // ==========================================
            // 6. BUILDING B DISTRIBUTION ROUTER
            // ==========================================
            const rtrBldgB = new RouterDevice('RTR_BLDG_B', 'RTR-Building-B', AreaType.MAN, 480, 260);
            rtrBldgB.description = 'Research Annex & Data Center Distribution Router';

            const rtrB_ifRes = new NetworkInterface('gi0/0', '00:BB:CC:DD:EE:01', '192.168.3.1', '255.255.255.0', rtrBldgB);
            rtrBldgB.addInterface(rtrB_ifRes);
            const swResUplink = new NetworkInterface('gi0/1', '00:33:44:55:66:03', null, null, swRes);
            swRes.addInterface(swResUplink);
            simulator.addLink(new NetworkLink('LINK_RES_RTR', swResUplink, rtrB_ifRes, AreaType.LAN, 1000, 1, 0));

            const rtrB_ifDc = new NetworkInterface('gi0/1', '00:BB:CC:DD:EE:02', '10.0.1.1', '255.255.255.0', rtrBldgB);
            rtrBldgB.addInterface(rtrB_ifDc);
            const swDcUplink = new NetworkInterface('gi0/10', '00:44:55:66:77:03', null, null, swDc);
            swDc.addInterface(swDcUplink);
            simulator.addLink(new NetworkLink('LINK_DC_RTR', swDcUplink, rtrB_ifDc, AreaType.LAN, 10000, 0.5, 0));

            simulator.addDevice(rtrBldgB);

            // ==========================================
            // 7. MAN (CAMPUS METROPOLITAN FIBER RING)
            // ==========================================
            const rtrA_manPri = new NetworkInterface('fiber0/1', '00:AA:BB:CC:DD:03', '10.100.1.1', '255.255.255.252', rtrBldgA);
            rtrBldgA.addInterface(rtrA_manPri);
            const rtrB_manPri = new NetworkInterface('fiber0/1', '00:BB:CC:DD:EE:03', '10.100.1.2', '255.255.255.252', rtrBldgB);
            rtrBldgB.addInterface(rtrB_manPri);
            simulator.addLink(new NetworkLink('MAN_FIBER_PRIMARY', rtrA_manPri, rtrB_manPri, AreaType.MAN, 10000, 2, 0));

            const rtrGateway = new RouterDevice('RTR_GATEWAY', 'RTR-Campus-Gateway', AreaType.MAN, 380, 420);
            rtrGateway.description = 'Campus Border Gateway (NAT & Firewall Inspection)';
            rtrGateway.natEnabled = true;
            rtrGateway.firewallEnabled = true;
            simulator.addDevice(rtrGateway);

            // Setup Firewall Rules
            const fw = simulator.firewallEngine;
            fw.clearRules();
            fw.addRule(new FirewallRule(10, 'PERMIT', 'any', 'any', 'DNS', 53, 'Allow DNS Queries'));
            fw.addRule(new FirewallRule(20, 'PERMIT', 'any', 'any', 'HTTP', 80, 'Allow Web Browsing (Port 80)'));
            fw.addRule(new FirewallRule(30, 'PERMIT', 'any', 'any', 'ICMP', 0, 'Allow Ping / Diagnostics'));
            fw.addRule(new FirewallRule(40, 'PERMIT', 'any', 'any', 'TCP', 0, 'Allow General TCP Outbound'));

            const rtrA_gw = new NetworkInterface('fiber0/2', '00:AA:BB:CC:DD:04', '10.100.2.1', '255.255.255.252', rtrBldgA);
            rtrBldgA.addInterface(rtrA_gw);
            const gw_ifA = new NetworkInterface('fiber0/1', '00:EE:FF:11:22:01', '10.100.2.2', '255.255.255.252', rtrGateway);
            rtrGateway.addInterface(gw_ifA);
            simulator.addLink(new NetworkLink('MAN_FIBER_RING_1', rtrA_gw, gw_ifA, AreaType.MAN, 10000, 2.5, 0));

            const rtrB_gw = new NetworkInterface('fiber0/2', '00:BB:CC:DD:EE:04', '10.100.3.1', '255.255.255.252', rtrBldgB);
            rtrBldgB.addInterface(rtrB_gw);
            const gw_ifB = new NetworkInterface('fiber0/2', '00:EE:FF:11:22:02', '10.100.3.2', '255.255.255.252', rtrGateway);
            rtrGateway.addInterface(gw_ifB);
            simulator.addLink(new NetworkLink('MAN_FIBER_RING_2', rtrB_gw, gw_ifB, AreaType.MAN, 10000, 2.5, 0));

            // ==========================================
            // 8. WAN (WIDE AREA NETWORK / ISP / CLOUD)
            // ==========================================
            const ispCore = new RouterDevice('ISP_CORE', 'ISP-Core-Router', AreaType.WAN, 380, 580);
            ispCore.description = 'Global Internet Service Provider Backbone Gateway';
            simulator.addDevice(ispCore);

            const gw_wan = new NetworkInterface('wan0', '00:EE:FF:11:22:03', '203.0.113.2', '255.255.255.252', rtrGateway);
            rtrGateway.addInterface(gw_wan);
            rtrGateway.natOutsideInterface = 'wan0';
            const isp_campusIf = new NetworkInterface('gi0/0', '00:99:88:77:66:01', '203.0.113.1', '255.255.255.252', ispCore);
            ispCore.addInterface(isp_campusIf);
            simulator.addLink(new NetworkLink('WAN_CAMPUS_ISP', gw_wan, isp_campusIf, AreaType.WAN, 500, 35, 0.005));

            const cloudServer = new ServerDevice('SRV_CLOUD', 'Cloud-Public-Web', AreaType.WAN, 160, 580, 'Global Cloud');
            cloudServer.description = 'Remote Cloud Hosting Facility (198.51.100.10)';
            cloudServer.httpServiceRunning = true;
            cloudServer.addWebPage('/', '<html><body><h1>University Global Cloud Portal</h1><p>Hosted in Remote Region Datacenter</p></body></html>');
            const cloudIf = new NetworkInterface('eth0', '00:88:77:66:55:01', '198.51.100.10', '255.255.255.0', cloudServer);
            cloudServer.addInterface(cloudIf);
            cloudServer.defaultGateway = '198.51.100.1';
            simulator.addDevice(cloudServer);

            const isp_cloudIf = new NetworkInterface('gi0/1', '00:99:88:77:66:02', '198.51.100.1', '255.255.255.0', ispCore);
            ispCore.addInterface(isp_cloudIf);
            simulator.addLink(new NetworkLink('WAN_ISP_CLOUD', cloudIf, isp_cloudIf, AreaType.WAN, 1000, 40, 0));

            const rtrRemote = new RouterDevice('RTR_REMOTE', 'RTR-Remote-Branch', AreaType.WAN, 600, 580);
            rtrRemote.description = 'Remote Satellite Campus Branch Router';
            const rtrRemote_wan = new NetworkInterface('wan0', '00:77:66:55:44:01', '192.0.2.2', '255.255.255.252', rtrRemote);
            rtrRemote.addInterface(rtrRemote_wan);
            const isp_branchIf = new NetworkInterface('gi0/2', '00:99:88:77:66:03', '192.0.2.1', '255.255.255.252', ispCore);
            ispCore.addInterface(isp_branchIf);
            simulator.addLink(new NetworkLink('WAN_ISP_BRANCH', rtrRemote_wan, isp_branchIf, AreaType.WAN, 100, 65, 0.01));
            simulator.addDevice(rtrRemote);

            const swRemote = new SwitchDevice('SW_REMOTE', 'SW-Remote-Branch', AreaType.WAN, 740, 580);
            simulator.addDevice(swRemote);
            const rtrRemote_lan = new NetworkInterface('gi0/0', '00:77:66:55:44:02', '172.16.1.1', '255.255.255.0', rtrRemote);
            rtrRemote.addInterface(rtrRemote_lan);
            const swRemote_uplink = new NetworkInterface('gi0/1', '00:66:55:44:33:01', null, null, swRemote);
            swRemote.addInterface(swRemote_uplink);
            simulator.addLink(new NetworkLink('LINK_REMOTE_LAN', swRemote_uplink, rtrRemote_lan, AreaType.LAN, 1000, 1, 0));

            const pcRemote = new HostDevice('PC_REMOTE_01', 'Remote-Campus-PC', AreaType.WAN, 880, 580, 'Remote Branch');
            pcRemote.description = 'Remote Satellite Branch Student PC';
            const ifRemotePc = new NetworkInterface('eth0', '00:55:44:33:22:01', '172.16.1.10', '255.255.255.0', pcRemote);
            pcRemote.addInterface(ifRemotePc);
            pcRemote.defaultGateway = '172.16.1.1';
            pcRemote.dnsServerIp = '10.0.1.10';
            simulator.addDevice(pcRemote);

            const swRemote_port = new NetworkInterface('fa0/1', '00:66:55:44:33:02', null, null, swRemote);
            swRemote.addInterface(swRemote_port);
            simulator.addLink(new NetworkLink('LINK_REMOTE_PC', ifRemotePc, swRemote_port, AreaType.LAN, 1000, 1, 0));

            simulator.recalculateRouting();
        }
    }

    exports.ScenarioManager = ScenarioManager;
})(typeof module !== 'undefined' && module.exports ? module.exports : (window.CampusNet = window.CampusNet || {}));
