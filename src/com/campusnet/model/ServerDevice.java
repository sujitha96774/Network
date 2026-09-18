package com.campusnet.model;

import java.util.HashMap;
import java.util.Map;

public class ServerDevice extends HostDevice {
    private boolean httpServiceRunning = true;
    private boolean dnsServiceRunning = false;
    private boolean dhcpServiceRunning = false;
    private final Map<String, String> hostedWebPages = new HashMap<>();
    private final Map<String, String> dnsRecords = new HashMap<>(); // domain -> IP

    public ServerDevice(String id, String name, AreaType areaType, int x, int y, String department) {
        super(id, name, areaType, x, y, department);
        hostedWebPages.put("/", "<html><head><title>Campus Portal</title></head><body><h1>Welcome to University Campus Network</h1><p>Active Services: Web, DNS, Library Portal, Student ERP</p></body></html>");
    }

    public boolean isHttpServiceRunning() {
        return httpServiceRunning;
    }

    public void setHttpServiceRunning(boolean httpServiceRunning) {
        this.httpServiceRunning = httpServiceRunning;
    }

    public boolean isDnsServiceRunning() {
        return dnsServiceRunning;
    }

    public void setDnsServiceRunning(boolean dnsServiceRunning) {
        this.dnsServiceRunning = dnsServiceRunning;
    }

    public boolean isDhcpServiceRunning() {
        return dhcpServiceRunning;
    }

    public void setDhcpServiceRunning(boolean dhcpServiceRunning) {
        this.dhcpServiceRunning = dhcpServiceRunning;
    }

    public void addWebPage(String path, String htmlContent) {
        hostedWebPages.put(path, htmlContent);
    }

    public String getWebPage(String path) {
        return hostedWebPages.getOrDefault(path, "<html><body><h1>404 Not Found</h1></body></html>");
    }

    public void addDnsRecord(String domain, String ip) {
        dnsRecords.put(domain.toLowerCase(), ip);
    }

    public String resolveDns(String domain) {
        return dnsRecords.get(domain.toLowerCase());
    }

    public Map<String, String> getDnsRecords() {
        return dnsRecords;
    }

    @Override
    public String getDeviceType() {
        return "Server";
    }
}
