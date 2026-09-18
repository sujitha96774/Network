package com.campusnet.simulator;

import com.campusnet.model.Packet;

public interface SimulationListener {
    void onPacketCreated(Packet packet);
    void onPacketMoved(Packet packet, String fromDevice, String toDevice, double progress);
    void onPacketDelivered(Packet packet);
    void onPacketDropped(Packet packet, String reason);
    void onTopologyChanged();
    void onStatsUpdated(NetworkMetrics metrics);
    void onLogMessage(String message);
}
