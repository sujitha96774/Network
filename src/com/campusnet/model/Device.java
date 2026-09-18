package com.campusnet.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Device {
    protected final String id;
    protected String name;
    protected AreaType areaType;
    protected final List<NetworkInterface> interfaces = new ArrayList<>();
    protected final Map<String, ArpTableEntry> arpTable = new HashMap<>();
    
    // UI Visual Coordinates
    protected int x;
    protected int y;
    protected boolean isOnline = true;
    protected String description = "";

    public Device(String id, String name, AreaType areaType, int x, int y) {
        this.id = id;
        this.name = name;
        this.areaType = areaType;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    public void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    public List<NetworkInterface> getInterfaces() {
        return interfaces;
    }

    public void addInterface(NetworkInterface iface) {
        iface.setOwnerDevice(this);
        interfaces.add(iface);
    }

    public NetworkInterface getInterfaceByName(String name) {
        for (NetworkInterface iface : interfaces) {
            if (iface.getName().equalsIgnoreCase(name)) {
                return iface;
            }
        }
        return null;
    }

    public NetworkInterface getInterfaceByIp(String ip) {
        for (NetworkInterface iface : interfaces) {
            if (ip.equals(iface.getIpAddress())) {
                return iface;
            }
        }
        return null;
    }

    public NetworkInterface getPrimaryInterface() {
        return interfaces.isEmpty() ? null : interfaces.get(0);
    }

    public String getPrimaryIp() {
        for (NetworkInterface iface : interfaces) {
            if (iface.getIpAddress() != null) {
                return iface.getIpAddress();
            }
        }
        return "Unassigned";
    }

    public String getPrimaryMac() {
        for (NetworkInterface iface : interfaces) {
            if (iface.getMacAddress() != null) {
                return iface.getMacAddress();
            }
        }
        return "00:00:00:00:00:00";
    }

    public Map<String, ArpTableEntry> getArpTable() {
        return arpTable;
    }

    public void updateArpTable(String ip, String mac, String ifaceName) {
        arpTable.put(ip, new ArpTableEntry(ip, mac, ifaceName));
    }

    public String lookupArp(String ip) {
        ArpTableEntry entry = arpTable.get(ip);
        return entry != null ? entry.getMacAddress() : null;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public abstract String getDeviceType();

    public boolean hasIp(String ip) {
        for (NetworkInterface iface : interfaces) {
            if (ip.equals(iface.getIpAddress())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s - %s)", name, getDeviceType(), getPrimaryIp());
    }
}
