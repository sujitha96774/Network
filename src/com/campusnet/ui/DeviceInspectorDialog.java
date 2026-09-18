package com.campusnet.ui;

import com.campusnet.cli.VirtualCLI;
import com.campusnet.model.*;
import com.campusnet.simulator.NetworkSimulator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class DeviceInspectorDialog extends JDialog {
    private final Device device;
    private final NetworkSimulator simulator;
    private final VirtualCLI cli;

    private JTextArea terminalOutput;
    private JTextField terminalInput;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    public DeviceInspectorDialog(Frame parent, Device device, NetworkSimulator simulator) {
        super(parent, "Device Inspector: " + device.getName() + " [" + device.getDeviceType() + "]", true);
        this.device = device;
        this.simulator = simulator;
        this.cli = new VirtualCLI(simulator);

        setSize(780, 560);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        // Header Panel
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel titleLbl = new JLabel(device.getName() + " (" + device.getDeviceType() + ")");
        titleLbl.setFont(Theme.FONT_TITLE);
        titleLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel areaLbl = new JLabel("Area: " + device.getAreaType().getDescription());
        areaLbl.setFont(Theme.FONT_BODY);
        areaLbl.setForeground(device.getAreaType().getColor());

        header.add(titleLbl, BorderLayout.WEST);
        header.add(areaLbl, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(Theme.FONT_BODY);
        tabbedPane.setBackground(Theme.BG_PANEL);
        tabbedPane.setForeground(Theme.TEXT_PRIMARY);

        // Tab 1: Configuration & Interfaces
        tabbedPane.addTab("Interfaces & Config", createInterfacesPanel());

        // Tab 2: Tables (Routing / MAC)
        if (device instanceof RouterDevice) {
            tabbedPane.addTab("Routing Table", createRoutingTablePanel((RouterDevice) device));
        } else if (device instanceof SwitchDevice) {
            tabbedPane.addTab("MAC Address Table", createMacTablePanel((SwitchDevice) device));
        }

        // Tab 3: ARP Cache
        tabbedPane.addTab("ARP Cache", createArpTablePanel());

        // Tab 4: Virtual CLI Terminal
        tabbedPane.addTab("Virtual CLI Terminal", createTerminalPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createInterfacesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Interface", "IP Address", "Subnet Mask", "MAC Address", "Status", "Connected Peer"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (NetworkInterface iface : device.getInterfaces()) {
            String peer = "None";
            if (iface.getConnectedLink() != null) {
                Device other = iface.getConnectedLink().getOtherDevice(device);
                if (other != null) {
                    peer = other.getName() + " (" + iface.getConnectedLink().getAreaType().name() + ")";
                }
            }
            model.addRow(new Object[]{
                    iface.getName(),
                    iface.getIpAddress() != null ? iface.getIpAddress() : "Unassigned",
                    iface.getSubnetMask() != null ? iface.getSubnetMask() : "N/A",
                    iface.getMacAddress(),
                    iface.isUp() ? "UP" : "DOWN",
                    peer
            });
        }

        JTable table = new JTable(model);
        table.setFont(Theme.FONT_MONO);
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setRowHeight(22);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Quick details at bottom
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        infoPanel.setBackground(Theme.BG_CARD);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        infoPanel.add(new JLabel("Primary IP: " + device.getPrimaryIp()));
        infoPanel.add(new JLabel("Device Status: " + (device.isOnline() ? "ONLINE" : "OFFLINE")));
        if (device instanceof HostDevice host) {
            infoPanel.add(new JLabel("Default Gateway: " + (host.getDefaultGateway() != null ? host.getDefaultGateway() : "N/A")));
            infoPanel.add(new JLabel("DNS Server: " + (host.getDnsServerIp() != null ? host.getDnsServerIp() : "N/A")));
        } else {
            infoPanel.add(new JLabel("Description: " + device.getDescription()));
            infoPanel.add(new JLabel("Interfaces Count: " + device.getInterfaces().size()));
        }

        panel.add(infoPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRoutingTablePanel(RouterDevice router) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"Destination Network", "Subnet Mask", "Next Hop / Gateway", "Interface", "Metric", "Protocol"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (RouteEntry re : router.getRoutingTable()) {
            model.addRow(new Object[]{
                    re.getDestinationNetwork(),
                    re.getSubnetMask(),
                    re.getNextHopIp(),
                    re.getOutgoingInterface(),
                    re.getMetric(),
                    re.getProtocol()
            });
        }

        JTable table = new JTable(model);
        table.setFont(Theme.FONT_MONO);
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setRowHeight(22);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMacTablePanel(SwitchDevice sw) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"VLAN", "MAC Address", "Port", "Type"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (MacTableEntry entry : sw.getMacTable().values()) {
            model.addRow(new Object[]{
                    entry.getVlanId(),
                    entry.getMacAddress(),
                    entry.getPortName(),
                    "DYNAMIC"
            });
        }

        JTable table = new JTable(model);
        table.setFont(Theme.FONT_MONO);
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setRowHeight(22);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createArpTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] cols = {"IP Address", "MAC Address", "Interface", "Type"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        for (ArpTableEntry entry : device.getArpTable().values()) {
            model.addRow(new Object[]{
                    entry.getIpAddress(),
                    entry.getMacAddress(),
                    entry.getInterfaceName(),
                    entry.isStatic() ? "STATIC" : "DYNAMIC"
            });
        }

        JTable table = new JTable(model);
        table.setFont(Theme.FONT_MONO);
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setRowHeight(22);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTerminalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_DARK);

        terminalOutput = new JTextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setFont(Theme.FONT_MONO);
        terminalOutput.setBackground(Theme.BG_DARK);
        terminalOutput.setForeground(new Color(34, 197, 94)); // Terminal green
        terminalOutput.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initial Banner
        terminalOutput.append("=======================================================================\n");
        terminalOutput.append(" Welcome to " + device.getName() + " Interactive Terminal\n");
        terminalOutput.append(" Type 'help' to view available diagnostic & routing commands.\n");
        terminalOutput.append("=======================================================================\n\n");

        JScrollPane scrollPane = new JScrollPane(terminalOutput);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Input Line
        JPanel inputBar = new JPanel(new BorderLayout(5, 0));
        inputBar.setBackground(Theme.BG_PANEL);
        inputBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel promptLbl = new JLabel(device.getName() + "# ");
        promptLbl.setFont(Theme.FONT_MONO_BOLD);
        promptLbl.setForeground(Theme.ACCENT_ORANGE);
        inputBar.add(promptLbl, BorderLayout.WEST);

        terminalInput = new JTextField();
        terminalInput.setFont(Theme.FONT_MONO);
        terminalInput.setBackground(Theme.BG_CARD);
        terminalInput.setForeground(Color.WHITE);
        terminalInput.setCaretColor(Color.WHITE);
        terminalInput.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        terminalInput.addActionListener(e -> executeTerminalInput());

        terminalInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (!commandHistory.isEmpty() && historyIndex > 0) {
                        historyIndex--;
                        terminalInput.setText(commandHistory.get(historyIndex));
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (historyIndex < commandHistory.size() - 1) {
                        historyIndex++;
                        terminalInput.setText(commandHistory.get(historyIndex));
                    } else {
                        historyIndex = commandHistory.size();
                        terminalInput.setText("");
                    }
                }
            }
        });

        inputBar.add(terminalInput, BorderLayout.CENTER);
        panel.add(inputBar, BorderLayout.SOUTH);

        return panel;
    }

    private void executeTerminalInput() {
        String input = terminalInput.getText().trim();
        if (input.isEmpty()) return;

        commandHistory.add(input);
        historyIndex = commandHistory.size();
        terminalInput.setText("");

        terminalOutput.append(device.getName() + "# " + input + "\n");

        String result = cli.executeCommand(device, input);
        if ("__CLEAR__".equals(result)) {
            terminalOutput.setText("");
        } else {
            terminalOutput.append(result + "\n\n");
        }

        terminalOutput.setCaretPosition(terminalOutput.getDocument().getLength());
    }
}
