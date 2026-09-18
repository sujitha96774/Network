package com.campusnet.ui;

import com.campusnet.model.Device;
import com.campusnet.model.Packet;
import com.campusnet.simulator.*;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainFrame extends JFrame implements SimulationListener {
    private final NetworkSimulator simulator;

    private TopologyPanel topologyPanel;
    private PacketInspectorPanel packetInspectorPanel;
    private StatsPanel statsPanel;
    private ControlPanel controlPanel;
    private JTextArea logConsoleArea;

    private final SimpleDateFormat logTimeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public MainFrame(NetworkSimulator simulator) {
        super("Multi-Area Campus Network Simulator (LAN, MAN, WAN) - University Network Architecture");
        this.simulator = simulator;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 850);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        simulator.addListener(this);

        initUI();
    }

    private void initUI() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Theme.BG_DARK);

        // 1. Top Control Toolbar
        controlPanel = new ControlPanel(simulator, this);
        getContentPane().add(controlPanel, BorderLayout.NORTH);

        // 2. Center Work Area (Split: Topology Canvas + Bottom Inspector/Logs)
        topologyPanel = new TopologyPanel(simulator, this);
        packetInspectorPanel = new PacketInspectorPanel(simulator);

        // Bottom Log Console
        logConsoleArea = new JTextArea();
        logConsoleArea.setEditable(false);
        logConsoleArea.setFont(Theme.FONT_MONO);
        logConsoleArea.setBackground(Theme.BG_DARK);
        logConsoleArea.setForeground(new Color(203, 213, 225));
        logConsoleArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JTabbedPane bottomTabs = new JTabbedPane();
        bottomTabs.setFont(Theme.FONT_BODY);
        bottomTabs.setBackground(Theme.BG_PANEL);
        bottomTabs.setForeground(Theme.TEXT_PRIMARY);

        bottomTabs.addTab("Packet Inspector (Wireshark-Style)", packetInspectorPanel);
        bottomTabs.addTab("Live Simulation Event Log", new JScrollPane(logConsoleArea));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topologyPanel, bottomTabs);
        mainSplit.setDividerLocation(460);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setBorder(null);

        getContentPane().add(mainSplit, BorderLayout.CENTER);

        // 3. Bottom Stats & Throughput Dashboard
        statsPanel = new StatsPanel(simulator);
        getContentPane().add(statsPanel, BorderLayout.SOUTH);
    }

    public void openDeviceInspector(Device device) {
        DeviceInspectorDialog dialog = new DeviceInspectorDialog(this, device, simulator);
        dialog.setVisible(true);
    }

    public void openTrafficWizard() {
        TrafficWizardDialog dialog = new TrafficWizardDialog(this, simulator, null, "PING");
        dialog.setVisible(true);
    }

    public void openTrafficWizardForDevice(Device device, String initialType) {
        TrafficWizardDialog dialog = new TrafficWizardDialog(this, simulator, device, initialType);
        dialog.setVisible(true);
    }

    public void refreshAll() {
        topologyPanel.repaint();
        packetInspectorPanel.refreshTable();
        statsPanel.updateStats(simulator.getMetrics());
    }

    // SimulationListener Callbacks
    @Override
    public void onPacketCreated(Packet packet) {
        packetInspectorPanel.addPacket(packet);
        topologyPanel.repaint();
    }

    @Override
    public void onPacketMoved(Packet packet, String fromDevice, String toDevice, double progress) {
        topologyPanel.repaint();
    }

    @Override
    public void onPacketDelivered(Packet packet) {
        packetInspectorPanel.updatePacketStatus(packet);
        topologyPanel.repaint();
    }

    @Override
    public void onPacketDropped(Packet packet, String reason) {
        packetInspectorPanel.updatePacketStatus(packet);
        topologyPanel.repaint();
    }

    @Override
    public void onTopologyChanged() {
        topologyPanel.repaint();
    }

    @Override
    public void onStatsUpdated(NetworkMetrics metrics) {
        statsPanel.updateStats(metrics);
    }

    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (logConsoleArea != null) {
                String timestamp = logTimeFormat.format(new Date());
                logConsoleArea.append(String.format("[%s] %s\n", timestamp, message));
                logConsoleArea.setCaretPosition(logConsoleArea.getDocument().getLength());
            }
        });
    }
}
