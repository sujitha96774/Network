package com.campusnet.simulator;

import com.campusnet.model.Device;
import com.campusnet.model.NetworkInterface;
import com.campusnet.model.NetworkLink;
import com.campusnet.model.Packet;

public class SimulationEvent implements Comparable<SimulationEvent> {
    public enum EventType {
        PACKET_SEND,
        PACKET_ARRIVE_LINK,
        PACKET_ARRIVE_DEVICE,
        PACKET_PROCESSED,
        PACKET_DROPPED,
        LINK_STATE_CHANGE
    }

    private final long timestamp; // Simulation time in ms
    private final EventType type;
    private final Packet packet;
    private final Device currentDevice;
    private final NetworkInterface currentInterface;
    private final NetworkLink currentLink;
    private final String description;

    public SimulationEvent(long timestamp, EventType type, Packet packet,
                           Device currentDevice, NetworkInterface currentInterface,
                           NetworkLink currentLink, String description) {
        this.timestamp = timestamp;
        this.type = type;
        this.packet = packet;
        this.currentDevice = currentDevice;
        this.currentInterface = currentInterface;
        this.currentLink = currentLink;
        this.description = description;
    }

    public long getTimestamp() { return timestamp; }
    public EventType getType() { return type; }
    public Packet getPacket() { return packet; }
    public Device getCurrentDevice() { return currentDevice; }
    public NetworkInterface getCurrentInterface() { return currentInterface; }
    public NetworkLink getCurrentLink() { return currentLink; }
    public String getDescription() { return description; }

    @Override
    public int compareTo(SimulationEvent o) {
        return Long.compare(this.timestamp, o.timestamp);
    }
}
