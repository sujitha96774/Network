package com.campusnet.model;

import java.awt.Color;

public enum AreaType {
    LAN("Local Area Network (Department/Building)", new Color(59, 130, 246), "LAN"),
    MAN("Metropolitan Area Network (Campus Backbone)", new Color(16, 185, 129), "MAN"),
    WAN("Wide Area Network (Remote Branch / Internet / Cloud)", new Color(245, 158, 11), "WAN");

    private final String description;
    private final Color color;
    private final String shortName;

    AreaType(String description, Color color, String shortName) {
        this.description = description;
        this.color = color;
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }

    public String getShortName() {
        return shortName;
    }
}
