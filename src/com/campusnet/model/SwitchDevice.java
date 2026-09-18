package com.campusnet.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SwitchDevice extends Device {
    private final Map<String, MacTableEntry> macTable = new HashMap<>();
    private final Map<String, Integer> portVlanMap = new HashMap<>();

    public SwitchDevice(String id, String name, AreaType areaType, int x, int y) {
        super(id, name, areaType, x, y);
    }

    public Map<String, MacTableEntry> getMacTable() {
        return macTable;
    }

    public void learnMac(String mac, String portName, int vlanId) {
        if (mac == null || mac.isEmpty() || "FF:FF:FF:FF:FF:FF".equalsIgnoreCase(mac)) {
            return;
        }
        macTable.put(mac.toUpperCase(), new MacTableEntry(mac.toUpperCase(), portName, vlanId));
    }

    public String lookupPort(String mac) {
        if (mac == null) return null;
        MacTableEntry entry = macTable.get(mac.toUpperCase());
        return entry != null ? entry.getPortName() : null;
    }

    public void clearMacTable() {
        macTable.clear();
    }

    public void setPortVlan(String portName, int vlanId) {
        portVlanMap.put(portName, vlanId);
    }

    public int getPortVlan(String portName) {
        return portVlanMap.getOrDefault(portName, 1);
    }

    @Override
    public String getDeviceType() {
        return "L2 Switch";
    }
}
