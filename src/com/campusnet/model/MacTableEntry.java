package com.campusnet.model;

public class MacTableEntry {
    private final String macAddress;
    private final String portName;
    private final int vlanId;
    private long lastSeenTimestamp;

    public MacTableEntry(String macAddress, String portName, int vlanId) {
        this.macAddress = macAddress;
        this.portName = portName;
        this.vlanId = vlanId;
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    public MacTableEntry(String macAddress, String portName) {
        this(macAddress, portName, 1);
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getPortName() {
        return portName;
    }

    public int getVlanId() {
        return vlanId;
    }

    public long getLastSeenTimestamp() {
        return lastSeenTimestamp;
    }

    public void updateTimestamp() {
        this.lastSeenTimestamp = System.currentTimeMillis();
    }
}
