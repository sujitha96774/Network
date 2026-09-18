package com.campusnet.simulator;

import com.campusnet.model.*;
import com.campusnet.protocol.FirewallEngine;

public class ScenarioManager {

    public static void buildDefaultCampusTopology(NetworkSimulator simulator) {
        simulator.reset();
        simulator.getDevices().clear();
        simulator.getLinks().clear();

        // ==========================================
        // 1. LAN 1: COMPUTER SCIENCE DEPARTMENT
        // ==========================================
        SwitchDevice swCs = new SwitchDevice("SW_CS", "SW-CS-Dept", AreaType.LAN, 120, 160);
        swCs.setDescription("CS Dept Access Switch (VLAN 10)");
        simulator.addDevice(swCs);

        HostDevice pcCs1 = new HostDevice("PC_CS_01", "CS-Student-01", AreaType.LAN, 50, 60, "Computer Science");
        pcCs1.setDescription("CS Student Lab Workstation");
        NetworkInterface ifCs1 = new NetworkInterface("eth0", "00:1A:2B:3C:4D:01", "192.168.1.10", "255.255.255.0", pcCs1);
        pcCs1.addInterface(ifCs1);
        pcCs1.setDefaultGateway("192.168.1.1");
        pcCs1.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcCs1);

        HostDevice pcCs2 = new HostDevice("PC_CS_02", "CS-Student-02", AreaType.LAN, 190, 60, "Computer Science");
        pcCs2.setDescription("CS AI Research Terminal");
        NetworkInterface ifCs2 = new NetworkInterface("eth0", "00:1A:2B:3C:4D:02", "192.168.1.11", "255.255.255.0", pcCs2);
        pcCs2.addInterface(ifCs2);
        pcCs2.setDefaultGateway("192.168.1.1");
        pcCs2.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcCs2);

        // Connect CS Hosts to Switch
        NetworkInterface swCsPort1 = new NetworkInterface("fa0/1", "00:11:22:33:44:01", swCs);
        swCs.addInterface(swCsPort1);
        simulator.addLink(new NetworkLink("LINK_CS1", ifCs1, swCsPort1, AreaType.LAN, 1000.0, 1.0, 0.0));

        NetworkInterface swCsPort2 = new NetworkInterface("fa0/2", "00:11:22:33:44:02", swCs);
        swCs.addInterface(swCsPort2);
        simulator.addLink(new NetworkLink("LINK_CS2", ifCs2, swCsPort2, AreaType.LAN, 1000.0, 1.0, 0.0));

        // ==========================================
        // 2. LAN 2: ADMIN & FINANCE DEPARTMENT
        // ==========================================
        SwitchDevice swAdmin = new SwitchDevice("SW_ADMIN", "SW-Admin-Dept", AreaType.LAN, 120, 360);
        swAdmin.setDescription("Admin & Finance Switch (VLAN 20)");
        simulator.addDevice(swAdmin);

        HostDevice pcAdmin1 = new HostDevice("PC_ADM_01", "Admin-Finance", AreaType.LAN, 50, 460, "Administration");
        pcAdmin1.setDescription("Campus Payroll & Finance PC");
        NetworkInterface ifAdm1 = new NetworkInterface("eth0", "00:2A:3B:4C:5D:01", "192.168.2.10", "255.255.255.0", pcAdmin1);
        pcAdmin1.addInterface(ifAdm1);
        pcAdmin1.setDefaultGateway("192.168.2.1");
        pcAdmin1.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcAdmin1);

        HostDevice pcAdmin2 = new HostDevice("PC_ADM_02", "Admin-Registrar", AreaType.LAN, 190, 460, "Administration");
        pcAdmin2.setDescription("Student Admissions & Records PC");
        NetworkInterface ifAdm2 = new NetworkInterface("eth0", "00:2A:3B:4C:5D:02", "192.168.2.11", "255.255.255.0", pcAdmin2);
        pcAdmin2.addInterface(ifAdm2);
        pcAdmin2.setDefaultGateway("192.168.2.1");
        pcAdmin2.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcAdmin2);

        // Connect Admin Hosts to Switch
        NetworkInterface swAdmPort1 = new NetworkInterface("fa0/1", "00:22:33:44:55:01", swAdmin);
        swAdmin.addInterface(swAdmPort1);
        simulator.addLink(new NetworkLink("LINK_ADM1", ifAdm1, swAdmPort1, AreaType.LAN, 1000.0, 1.0, 0.0));

        NetworkInterface swAdmPort2 = new NetworkInterface("fa0/2", "00:22:33:44:55:02", swAdmin);
        swAdmin.addInterface(swAdmPort2);
        simulator.addLink(new NetworkLink("LINK_ADM2", ifAdm2, swAdmPort2, AreaType.LAN, 1000.0, 1.0, 0.0));

        // ==========================================
        // 3. MAIN CAMPUS BUILDING A CORE ROUTER
        // ==========================================
        RouterDevice rtrBldgA = new RouterDevice("RTR_BLDG_A", "RTR-Building-A", AreaType.MAN, 280, 260);
        rtrBldgA.setDescription("Main Campus Academic Building Core Router");
        
        NetworkInterface rtrA_ifCs = new NetworkInterface("gi0/0", "00:AA:BB:CC:DD:01", "192.168.1.1", "255.255.255.0", rtrBldgA);
        rtrBldgA.addInterface(rtrA_ifCs);
        NetworkInterface swCsUplink = new NetworkInterface("gi0/1", "00:11:22:33:44:03", swCs);
        swCs.addInterface(swCsUplink);
        simulator.addLink(new NetworkLink("LINK_CS_RTR", swCsUplink, rtrA_ifCs, AreaType.LAN, 1000.0, 1.0, 0.0));

        NetworkInterface rtrA_ifAdm = new NetworkInterface("gi0/1", "00:AA:BB:CC:DD:02", "192.168.2.1", "255.255.255.0", rtrBldgA);
        rtrBldgA.addInterface(rtrA_ifAdm);
        NetworkInterface swAdmUplink = new NetworkInterface("gi0/1", "00:22:33:44:55:03", swAdmin);
        swAdmin.addInterface(swAdmUplink);
        simulator.addLink(new NetworkLink("LINK_ADM_RTR", swAdmUplink, rtrA_ifAdm, AreaType.LAN, 1000.0, 1.0, 0.0));

        simulator.addDevice(rtrBldgA);

        // ==========================================
        // 4. LAN 3: RESEARCH & LABS (BUILDING B)
        // ==========================================
        SwitchDevice swRes = new SwitchDevice("SW_RES", "SW-Research-Lab", AreaType.LAN, 640, 160);
        swRes.setDescription("Engineering & Nanotech Research Switch");
        simulator.addDevice(swRes);

        HostDevice pcRes1 = new HostDevice("PC_RES_01", "Research-WS-01", AreaType.LAN, 570, 60, "Research");
        NetworkInterface ifRes1 = new NetworkInterface("eth0", "00:3A:4B:5C:6D:01", "192.168.3.10", "255.255.255.0", pcRes1);
        pcRes1.addInterface(ifRes1);
        pcRes1.setDefaultGateway("192.168.3.1");
        pcRes1.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcRes1);

        HostDevice pcRes2 = new HostDevice("PC_RES_02", "Research-WS-02", AreaType.LAN, 710, 60, "Research");
        NetworkInterface ifRes2 = new NetworkInterface("eth0", "00:3A:4B:5C:6D:02", "192.168.3.11", "255.255.255.0", pcRes2);
        pcRes2.addInterface(ifRes2);
        pcRes2.setDefaultGateway("192.168.3.1");
        pcRes2.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcRes2);

        NetworkInterface swResPort1 = new NetworkInterface("fa0/1", "00:33:44:55:66:01", swRes);
        swRes.addInterface(swResPort1);
        simulator.addLink(new NetworkLink("LINK_RES1", ifRes1, swResPort1, AreaType.LAN, 1000.0, 1.0, 0.0));

        NetworkInterface swResPort2 = new NetworkInterface("fa0/2", "00:33:44:55:66:02", swRes);
        swRes.addInterface(swResPort2);
        simulator.addLink(new NetworkLink("LINK_RES2", ifRes2, swResPort2, AreaType.LAN, 1000.0, 1.0, 0.0));

        // ==========================================
        // 5. LAN 4: CAMPUS CENTRAL DATA CENTER
        // ==========================================
        SwitchDevice swDc = new SwitchDevice("SW_DC", "SW-DataCenter", AreaType.LAN, 640, 360);
        swDc.setDescription("Campus Core Data Center High-Speed Switch");
        simulator.addDevice(swDc);

        ServerDevice srvWeb = new ServerDevice("SRV_WEB", "Campus-Portal-Web", AreaType.LAN, 570, 460, "Data Center");
        srvWeb.setDescription("University Web Portal & LMS Server (HTTP port 80)");
        srvWeb.setHttpServiceRunning(true);
        NetworkInterface ifWeb = new NetworkInterface("eth0", "00:4A:5B:6C:7D:01", "10.0.1.50", "255.255.255.0", srvWeb);
        srvWeb.addInterface(ifWeb);
        srvWeb.setDefaultGateway("10.0.1.1");
        srvWeb.setDnsServerIp("10.0.1.10");
        simulator.addDevice(srvWeb);

        ServerDevice srvDns = new ServerDevice("SRV_DNS", "Campus-DNS-Server", AreaType.LAN, 710, 460, "Data Center");
        srvDns.setDescription("Central Campus DNS Server (Port 53)");
        srvDns.setDnsServiceRunning(true);
        NetworkInterface ifDns = new NetworkInterface("eth0", "00:4A:5B:6C:7D:02", "10.0.1.10", "255.255.255.0", srvDns);
        srvDns.addInterface(ifDns);
        srvDns.setDefaultGateway("10.0.1.1");
        simulator.addDevice(srvDns);

        NetworkInterface swDcPort1 = new NetworkInterface("gi0/1", "00:44:55:66:77:01", swDc);
        swDc.addInterface(swDcPort1);
        simulator.addLink(new NetworkLink("LINK_DC1", ifWeb, swDcPort1, AreaType.LAN, 10000.0, 0.5, 0.0));

        NetworkInterface swDcPort2 = new NetworkInterface("gi0/2", "00:44:55:66:77:02", swDc);
        swDc.addInterface(swDcPort2);
        simulator.addLink(new NetworkLink("LINK_DC2", ifDns, swDcPort2, AreaType.LAN, 10000.0, 0.5, 0.0));

        // ==========================================
        // 6. CAMPUS BUILDING B CORE ROUTER
        // ==========================================
        RouterDevice rtrBldgB = new RouterDevice("RTR_BLDG_B", "RTR-Building-B", AreaType.MAN, 480, 260);
        rtrBldgB.setDescription("Research Annex & Data Center Distribution Router");

        NetworkInterface rtrB_ifRes = new NetworkInterface("gi0/0", "00:BB:CC:DD:EE:01", "192.168.3.1", "255.255.255.0", rtrBldgB);
        rtrBldgB.addInterface(rtrB_ifRes);
        NetworkInterface swResUplink = new NetworkInterface("gi0/1", "00:33:44:55:66:03", swRes);
        swRes.addInterface(swResUplink);
        simulator.addLink(new NetworkLink("LINK_RES_RTR", swResUplink, rtrB_ifRes, AreaType.LAN, 1000.0, 1.0, 0.0));

        NetworkInterface rtrB_ifDc = new NetworkInterface("gi0/1", "00:BB:CC:DD:EE:02", "10.0.1.1", "255.255.255.0", rtrBldgB);
        rtrBldgB.addInterface(rtrB_ifDc);
        NetworkInterface swDcUplink = new NetworkInterface("gi0/10", "00:44:55:66:77:03", swDc);
        swDc.addInterface(swDcUplink);
        simulator.addLink(new NetworkLink("LINK_DC_RTR", swDcUplink, rtrB_ifDc, AreaType.LAN, 10000.0, 0.5, 0.0));

        simulator.addDevice(rtrBldgB);

        // ==========================================
        // 7. MAN (CAMPUS METROPOLITAN FIBER RING)
        // ==========================================
        // Primary 10 Gbps Fiber Link between Bldg A and Bldg B (MAN)
        NetworkInterface rtrA_manPri = new NetworkInterface("fiber0/1", "00:AA:BB:CC:DD:03", "10.100.1.1", "255.255.255.252", rtrBldgA);
        rtrBldgA.addInterface(rtrA_manPri);
        NetworkInterface rtrB_manPri = new NetworkInterface("fiber0/1", "00:BB:CC:DD:EE:03", "10.100.1.2", "255.255.255.252", rtrBldgB);
        rtrBldgB.addInterface(rtrB_manPri);
        simulator.addLink(new NetworkLink("MAN_FIBER_PRIMARY", rtrA_manPri, rtrB_manPri, AreaType.MAN, 10000.0, 2.0, 0.0));

        // Campus Border / Gateway Router
        RouterDevice rtrGateway = new RouterDevice("RTR_GATEWAY", "RTR-Campus-Gateway", AreaType.MAN, 380, 420);
        rtrGateway.setDescription("Campus Border Gateway (NAT & Firewall Inspection)");
        rtrGateway.setNatEnabled(true);
        rtrGateway.setFirewallEnabled(true);
        simulator.addDevice(rtrGateway);

        // Setup Firewall Rules on Campus Gateway
        FirewallEngine fw = simulator.getFirewallEngine();
        fw.clearRules();
        fw.addRule(new FirewallEngine.Rule(10, FirewallEngine.Action.PERMIT, "any", "any", "DNS", 53, "Allow DNS Queries"));
        fw.addRule(new FirewallEngine.Rule(20, FirewallEngine.Action.PERMIT, "any", "any", "HTTP", 80, "Allow Web Browsing (Port 80)"));
        fw.addRule(new FirewallEngine.Rule(30, FirewallEngine.Action.PERMIT, "any", "any", "ICMP", 0, "Allow Ping / Diagnostics"));
        fw.addRule(new FirewallEngine.Rule(40, FirewallEngine.Action.PERMIT, "any", "any", "TCP", 0, "Allow General TCP Outbound"));

        // MAN Link Bldg A <-> Gateway
        NetworkInterface rtrA_gw = new NetworkInterface("fiber0/2", "00:AA:BB:CC:DD:04", "10.100.2.1", "255.255.255.252", rtrBldgA);
        rtrBldgA.addInterface(rtrA_gw);
        NetworkInterface gw_ifA = new NetworkInterface("fiber0/1", "00:EE:FF:11:22:01", "10.100.2.2", "255.255.255.252", rtrGateway);
        rtrGateway.addInterface(gw_ifA);
        simulator.addLink(new NetworkLink("MAN_FIBER_RING_1", rtrA_gw, gw_ifA, AreaType.MAN, 10000.0, 2.5, 0.0));

        // MAN Link Bldg B <-> Gateway
        NetworkInterface rtrB_gw = new NetworkInterface("fiber0/2", "00:BB:CC:DD:EE:04", "10.100.3.1", "255.255.255.252", rtrBldgB);
        rtrBldgB.addInterface(rtrB_gw);
        NetworkInterface gw_ifB = new NetworkInterface("fiber0/2", "00:EE:FF:11:22:02", "10.100.3.2", "255.255.255.252", rtrGateway);
        rtrGateway.addInterface(gw_ifB);
        simulator.addLink(new NetworkLink("MAN_FIBER_RING_2", rtrB_gw, gw_ifB, AreaType.MAN, 10000.0, 2.5, 0.0));

        // ==========================================
        // 8. WAN (WIDE AREA NETWORK / ISP / CLOUD)
        // ==========================================
        RouterDevice ispCore = new RouterDevice("ISP_CORE", "ISP-Core-Router", AreaType.WAN, 380, 580);
        ispCore.setDescription("Global Internet Service Provider Backbone Gateway");
        simulator.addDevice(ispCore);

        // WAN Link: Campus Gateway <-> ISP Core
        NetworkInterface gw_wan = new NetworkInterface("wan0", "00:EE:FF:11:22:03", "203.0.113.2", "255.255.255.252", rtrGateway);
        rtrGateway.addInterface(gw_wan);
        rtrGateway.setNatOutsideInterface("wan0");
        NetworkInterface isp_campusIf = new NetworkInterface("gi0/0", "00:99:88:77:66:01", "203.0.113.1", "255.255.255.252", ispCore);
        ispCore.addInterface(isp_campusIf);
        simulator.addLink(new NetworkLink("WAN_CAMPUS_ISP", gw_wan, isp_campusIf, AreaType.WAN, 500.0, 35.0, 0.005));

        // Remote Cloud Web Server (e.g. AWS / University Cloud Archive)
        ServerDevice cloudServer = new ServerDevice("SRV_CLOUD", "Cloud-Public-Web", AreaType.WAN, 160, 580, "Global Cloud");
        cloudServer.setDescription("Remote Cloud Hosting Facility (198.51.100.10)");
        cloudServer.setHttpServiceRunning(true);
        cloudServer.addWebPage("/", "<html><body><h1>University Global Cloud Portal</h1><p>Hosted in Remote Region Datacenter</p></body></html>");
        NetworkInterface cloudIf = new NetworkInterface("eth0", "00:88:77:66:55:01", "198.51.100.10", "255.255.255.0", cloudServer);
        cloudServer.addInterface(cloudIf);
        cloudServer.setDefaultGateway("198.51.100.1");
        simulator.addDevice(cloudServer);

        NetworkInterface isp_cloudIf = new NetworkInterface("gi0/1", "00:99:88:77:66:02", "198.51.100.1", "255.255.255.0", ispCore);
        ispCore.addInterface(isp_cloudIf);
        simulator.addLink(new NetworkLink("WAN_ISP_CLOUD", cloudIf, isp_cloudIf, AreaType.WAN, 1000.0, 40.0, 0.0));

        // Remote Satellite Branch Campus (City B)
        RouterDevice rtrRemote = new RouterDevice("RTR_REMOTE", "RTR-Remote-Branch", AreaType.WAN, 600, 580);
        rtrRemote.setDescription("Remote Satellite Campus Branch Router");
        NetworkInterface rtrRemote_wan = new NetworkInterface("wan0", "00:77:66:55:44:01", "192.0.2.2", "255.255.255.252", rtrRemote);
        rtrRemote.addInterface(rtrRemote_wan);
        NetworkInterface isp_branchIf = new NetworkInterface("gi0/2", "00:99:88:77:66:03", "192.0.2.1", "255.255.255.252", ispCore);
        ispCore.addInterface(isp_branchIf);
        simulator.addLink(new NetworkLink("WAN_ISP_BRANCH", rtrRemote_wan, isp_branchIf, AreaType.WAN, 100.0, 65.0, 0.01));
        simulator.addDevice(rtrRemote);

        SwitchDevice swRemote = new SwitchDevice("SW_REMOTE", "SW-Remote-Branch", AreaType.WAN, 740, 580);
        simulator.addDevice(swRemote);
        NetworkInterface rtrRemote_lan = new NetworkInterface("gi0/0", "00:77:66:55:44:02", "172.16.1.1", "255.255.255.0", rtrRemote);
        rtrRemote.addInterface(rtrRemote_lan);
        NetworkInterface swRemote_uplink = new NetworkInterface("gi0/1", "00:66:55:44:33:01", swRemote);
        swRemote.addInterface(swRemote_uplink);
        simulator.addLink(new NetworkLink("LINK_REMOTE_LAN", swRemote_uplink, rtrRemote_lan, AreaType.LAN, 1000.0, 1.0, 0.0));

        HostDevice pcRemote = new HostDevice("PC_REMOTE_01", "Remote-Campus-PC", AreaType.WAN, 880, 580, "Remote Branch");
        pcRemote.setDescription("Remote Satellite Branch Student PC");
        NetworkInterface ifRemotePc = new NetworkInterface("eth0", "00:55:44:33:22:01", "172.16.1.10", "255.255.255.0", pcRemote);
        pcRemote.addInterface(ifRemotePc);
        pcRemote.setDefaultGateway("172.16.1.1");
        pcRemote.setDnsServerIp("10.0.1.10");
        simulator.addDevice(pcRemote);

        NetworkInterface swRemote_port = new NetworkInterface("fa0/1", "00:66:55:44:33:02", swRemote);
        swRemote.addInterface(swRemote_port);
        simulator.addLink(new NetworkLink("LINK_REMOTE_PC", ifRemotePc, swRemote_port, AreaType.LAN, 1000.0, 1.0, 0.0));

        // Recompute Dijkstra SPF Dynamic Routing across all routers
        simulator.recalculateRouting();
    }
}
