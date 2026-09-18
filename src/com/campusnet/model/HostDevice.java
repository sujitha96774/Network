package com.campusnet.model;

public class HostDevice extends Device {
    private String defaultGateway;
    private String dnsServerIp;
    private String department;
    private boolean dhcpEnabled = false;

    public HostDevice(String id, String name, AreaType areaType, int x, int y, String department) {
        super(id, name, areaType, x, y);
        this.department = department;
    }

    public String getDefaultGateway() {
        return defaultGateway;
    }

    public void setDefaultGateway(String defaultGateway) {
        this.defaultGateway = defaultGateway;
    }

    public String getDnsServerIp() {
        return dnsServerIp;
    }

    public void setDnsServerIp(String dnsServerIp) {
        this.dnsServerIp = dnsServerIp;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isDhcpEnabled() {
        return dhcpEnabled;
    }

    public void setDhcpEnabled(boolean dhcpEnabled) {
        this.dhcpEnabled = dhcpEnabled;
    }

    @Override
    public String getDeviceType() {
        return "Host / PC";
    }
}
