package com.campusnet.model;

import java.util.ArrayList;
import java.util.List;

public class RouterDevice extends Device {
    private final List<RouteEntry> routingTable = new ArrayList<>();
    private boolean natEnabled = false;
    private boolean firewallEnabled = false;
    private String natOutsideInterface = null;

    public RouterDevice(String id, String name, AreaType areaType, int x, int y) {
        super(id, name, areaType, x, y);
    }

    public List<RouteEntry> getRoutingTable() {
        return routingTable;
    }

    public void addRoute(RouteEntry route) {
        routingTable.add(route);
    }

    public void clearRoutes() {
        routingTable.clear();
    }

    public RouteEntry findBestRoute(String destIp) {
        RouteEntry bestMatch = null;
        int longestPrefix = -1;

        for (RouteEntry entry : routingTable) {
            if (entry.matches(destIp)) {
                int prefix = entry.getPrefixLength();
                if (prefix > longestPrefix) {
                    longestPrefix = prefix;
                    bestMatch = entry;
                } else if (prefix == longestPrefix && bestMatch != null) {
                    // Compare metrics if same prefix length
                    if (entry.getMetric() < bestMatch.getMetric()) {
                        bestMatch = entry;
                    }
                }
            }
        }
        return bestMatch;
    }

    public boolean isNatEnabled() {
        return natEnabled;
    }

    public void setNatEnabled(boolean natEnabled) {
        this.natEnabled = natEnabled;
    }

    public String getNatOutsideInterface() {
        return natOutsideInterface;
    }

    public void setNatOutsideInterface(String natOutsideInterface) {
        this.natOutsideInterface = natOutsideInterface;
    }

    public boolean isFirewallEnabled() {
        return firewallEnabled;
    }

    public void setFirewallEnabled(boolean firewallEnabled) {
        this.firewallEnabled = firewallEnabled;
    }

    @Override
    public String getDeviceType() {
        return "L3 Router";
    }
}
