package com.campusnet.model;

public class NetworkInterface {
    private final String name; // e.g., "eth0", "gi0/1"
    private final String macAddress;
    private String ipAddress;
    private String subnetMask;
    private boolean isUp;
    private NetworkLink connectedLink;
    private Device ownerDevice;

    public NetworkInterface(String name, String macAddress, String ipAddress, String subnetMask, Device ownerDevice) {
        this.name = name;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.subnetMask = subnetMask;
        this.isUp = true;
        this.ownerDevice = ownerDevice;
    }

    public NetworkInterface(String name, String macAddress, Device ownerDevice) {
        this(name, macAddress, null, null, ownerDevice);
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSubnetMask() {
        return subnetMask;
    }

    public void setSubnetMask(String subnetMask) {
        this.subnetMask = subnetMask;
    }

    public boolean isUp() {
        return isUp && (connectedLink == null || connectedLink.isUp());
    }

    public void setUp(boolean up) {
        isUp = up;
    }

    public NetworkLink getConnectedLink() {
        return connectedLink;
    }

    public void setConnectedLink(NetworkLink connectedLink) {
        this.connectedLink = connectedLink;
    }

    public Device getOwnerDevice() {
        return ownerDevice;
    }

    public void setOwnerDevice(Device ownerDevice) {
        this.ownerDevice = ownerDevice;
    }

    public boolean isConfigured() {
        return ipAddress != null && !ipAddress.isEmpty();
    }

    public boolean isInSameSubnet(String otherIp) {
        if (ipAddress == null || subnetMask == null || otherIp == null) {
            return false;
        }
        try {
            long thisIpVal = ipToLong(ipAddress);
            long maskVal = ipToLong(subnetMask);
            long otherIpVal = ipToLong(otherIp);
            return (thisIpVal & maskVal) == (otherIpVal & maskVal);
        } catch (Exception e) {
            return false;
        }
    }

    public static long ipToLong(String ip) {
        String[] parts = ip.trim().split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(parts[i]) & 0xFF);
        }
        return result;
    }

    public static String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }

    @Override
    public String toString() {
        return name + " (" + (ipAddress != null ? ipAddress : "No IP") + ", MAC: " + macAddress + ")";
    }
}
