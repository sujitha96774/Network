package com.campusnet.model;

import java.awt.Color;

public enum ProtocolType {
    ICMP("ICMP", "Ping / Echo Request & Reply", new Color(59, 130, 246)),
    TCP_SYN("TCP-SYN", "TCP Connection Initiation", new Color(16, 185, 129)),
    TCP_ACK("TCP-ACK", "TCP Acknowledgment", new Color(52, 211, 153)),
    HTTP("HTTP", "Web Request / Response (Port 80)", new Color(245, 158, 11)),
    HTTPS("HTTPS", "Secure Web (Port 443)", new Color(217, 119, 6)),
    DNS("DNS", "Domain Name Query / Response (Port 53)", new Color(168, 85, 247)),
    DHCP("DHCP", "IP Address Configuration", new Color(236, 72, 153)),
    FTP("FTP", "File Transfer Protocol (Port 21)", new Color(14, 165, 233)),
    ARP("ARP", "Address Resolution Protocol (Broadcast)", new Color(234, 179, 8)),
    DROPPED("DROP", "Packet Dropped / Blocked by Firewall", new Color(239, 68, 68));

    private final String code;
    private final String description;
    private final Color color;

    ProtocolType(String code, String description, Color color) {
        this.code = code;
        this.description = description;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }
}
