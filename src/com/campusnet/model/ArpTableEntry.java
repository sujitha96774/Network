package com.campusnet.model;

public class ArpTableEntry {
    private final String ipAddress;
    private final String macAddress;
    private final String interfaceName;
    private final boolean isStatic;
    private long timestamp;

    public ArpTableEntry(String ipAddress, String macAddress, String interfaceName, boolean isStatic) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.interfaceName = interfaceName;
        this.isStatic = isStatic;
        this.timestamp = System.currentTimeMillis();
    }

    public ArpTableEntry(String ipAddress, String macAddress, String interfaceName) {
        this(ipAddress, macAddress, interfaceName, false);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void refresh() {
        this.timestamp = System.currentTimeMillis();
    }
}
