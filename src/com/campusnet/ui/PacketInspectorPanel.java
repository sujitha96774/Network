package com.campusnet.ui;

import com.campusnet.model.Packet;
import com.campusnet.model.ProtocolType;
import com.campusnet.simulator.NetworkSimulator;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PacketInspectorPanel extends JPanel {
    private final NetworkSimulator simulator;
    private final JTable packetTable;
    private final DefaultTableModel tableModel;
    private final JTextArea packetDetailsArea;
    private final JComboBox<String> filterCombo;
    private final JTextField searchField;

    private final List<Packet> displayedPackets = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public PacketInspectorPanel(NetworkSimulator simulator) {
        this.simulator = simulator;
        setLayout(new BorderLayout());
        setBackground(Theme.BG_PANEL);

        // Top Header & Filter Bar
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Theme.BG_PANEL);
        topPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel titleLabel = new JLabel("Wireshark-Style Live Packet Sniffer & Protocol Analyzer");
        titleLabel.setFont(Theme.FONT_HEADER);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setForeground(Theme.TEXT_MUTED);
        filterLbl.setFont(Theme.FONT_SMALL);
        filterPanel.add(filterLbl);

        filterCombo = new JComboBox<>(new String[]{"ALL Protocols", "ICMP", "HTTP", "DNS", "TCP", "DROPPED"});
        filterCombo.setFont(Theme.FONT_SMALL);
        filterCombo.addActionListener(e -> refreshTable());
        filterPanel.add(filterCombo);

        searchField = new JTextField(12);
        searchField.setFont(Theme.FONT_SMALL);
        searchField.putClientProperty("JTextField.placeholderText", "Search IP/Payload...");
        searchField.addActionListener(e -> refreshTable());
        filterPanel.add(searchField);

        JButton btnClear = new JButton("Clear Log");
        btnClear.setFont(Theme.FONT_SMALL);
        btnClear.addActionListener(e -> {
            simulator.getPacketHistory().clear();
            refreshTable();
        });
        filterPanel.add(btnClear);

        topPanel.add(filterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Table Model & JTable
        String[] columns = {"No.", "Time", "Source IP", "Destination IP", "Protocol", "Length", "Status", "Info / Summary"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        packetTable = new JTable(tableModel);
        packetTable.setBackground(Theme.BG_DARK);
        packetTable.setForeground(Theme.TEXT_PRIMARY);
        packetTable.setFont(Theme.FONT_MONO);
        packetTable.setRowHeight(22);
        packetTable.setGridColor(Theme.BORDER_COLOR);
        packetTable.getTableHeader().setBackground(Theme.BG_CARD);
        packetTable.getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        packetTable.getTableHeader().setFont(Theme.FONT_HEADER);

        // Custom Column Widths
        packetTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        packetTable.getColumnModel().getColumn(1).setPreferredWidth(90);
        packetTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        packetTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        packetTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        packetTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        packetTable.getColumnModel().getColumn(6).setPreferredWidth(90);
        packetTable.getColumnModel().getColumn(7).setPreferredWidth(350);

        // Color Renderer for Protocol & Status
        packetTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(Theme.BG_CARD);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.BG_DARK : new Color(20, 30, 48));
                    c.setForeground(Theme.TEXT_PRIMARY);

                    if (col == 4 && row < displayedPackets.size()) {
                        Packet p = displayedPackets.get(row);
                        c.setForeground(p.getProtocol().getColor());
                    }
                    if (col == 6 && row < displayedPackets.size()) {
                        Packet p = displayedPackets.get(row);
                        if (p.getStatus() == Packet.Status.DROPPED) {
                            c.setForeground(Theme.ACCENT_RED);
                        } else if (p.getStatus() == Packet.Status.DELIVERED) {
                            c.setForeground(Theme.ACCENT_GREEN);
                        }
                    }
                }
                return c;
            }
        });

        // Selection Listener for packet dissection
        packetTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = packetTable.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < displayedPackets.size()) {
                Packet selectedPacket = displayedPackets.get(selectedRow);
                displayPacketDetails(selectedPacket);
            }
        });

        // Lower Dissection Panel
        packetDetailsArea = new JTextArea();
        packetDetailsArea.setEditable(false);
        packetDetailsArea.setFont(Theme.FONT_MONO);
        packetDetailsArea.setBackground(Theme.BG_DARK);
        packetDetailsArea.setForeground(new Color(226, 232, 240));
        packetDetailsArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(packetTable),
                new JScrollPane(packetDetailsArea));
        splitPane.setDividerLocation(180);
        splitPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR));

        add(splitPane, BorderLayout.CENTER);
    }

    public synchronized void addPacket(Packet packet) {
        SwingUtilities.invokeLater(() -> {
            if (matchesFilter(packet)) {
                displayedPackets.add(packet);
                tableModel.addRow(new Object[]{
                        displayedPackets.size(),
                        timeFormat.format(new Date(packet.getCreationTime())),
                        packet.getSourceIp(),
                        packet.getDestIp(),
                        packet.getProtocol().getCode(),
                        packet.getPacketSize() + " B",
                        packet.getStatus().name(),
                        packet.getPayload()
                });
                // Auto scroll to bottom
                packetTable.scrollRectToVisible(packetTable.getCellRect(packetTable.getRowCount() - 1, 0, true));
            }
        });
    }

    public synchronized void updatePacketStatus(Packet packet) {
        SwingUtilities.invokeLater(() -> {
            int index = displayedPackets.indexOf(packet);
            if (index >= 0) {
                tableModel.setValueAt(packet.getStatus().name(), index, 6);
                if (packet.getStatus() == Packet.Status.DROPPED && packet.getDropReason() != null) {
                    tableModel.setValueAt(packet.getPayload() + " [DROP: " + packet.getDropReason() + "]", index, 7);
                }
            }
        });
    }

    public synchronized void refreshTable() {
        displayedPackets.clear();
        tableModel.setRowCount(0);

        List<Packet> all = simulator.getPacketHistory();
        for (Packet p : all) {
            if (matchesFilter(p)) {
                displayedPackets.add(p);
                tableModel.addRow(new Object[]{
                        displayedPackets.size(),
                        timeFormat.format(new Date(p.getCreationTime())),
                        p.getSourceIp(),
                        p.getDestIp(),
                        p.getProtocol().getCode(),
                        p.getPacketSize() + " B",
                        p.getStatus().name(),
                        p.getPayload() + (p.getDropReason() != null ? " [" + p.getDropReason() + "]" : "")
                });
            }
        }
    }

    private boolean matchesFilter(Packet p) {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter != null && !"ALL Protocols".equals(filter)) {
            if ("DROPPED".equals(filter) && p.getStatus() != Packet.Status.DROPPED) return false;
            if ("ICMP".equals(filter) && p.getProtocol() != ProtocolType.ICMP) return false;
            if ("HTTP".equals(filter) && p.getProtocol() != ProtocolType.HTTP) return false;
            if ("DNS".equals(filter) && p.getProtocol() != ProtocolType.DNS) return false;
            if ("TCP".equals(filter) && (p.getProtocol() != ProtocolType.TCP_SYN && p.getProtocol() != ProtocolType.TCP_ACK)) return false;
        }

        String search = searchField.getText().trim().toLowerCase();
        if (!search.isEmpty()) {
            boolean match = p.getSourceIp().toLowerCase().contains(search) ||
                    p.getDestIp().toLowerCase().contains(search) ||
                    p.getPayload().toLowerCase().contains(search);
            if (!match) return false;
        }

        return true;
    }

    private void displayPacketDetails(Packet p) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================================\n");
        sb.append(String.format("FRAME #%s: %d bytes on wire, Protocol: %s, Status: %s\n",
                p.getId(), p.getPacketSize(), p.getProtocol().getCode(), p.getStatus()));
        sb.append("========================================================================================\n");

        sb.append("▶ LAYER 2 - ETHERNET II (Data Link Layer)\n");
        sb.append(String.format("    Destination MAC: %s\n", p.getDestMac() != null ? p.getDestMac() : "Broadcast / Gateway MAC"));
        sb.append(String.format("    Source MAC:      %s\n", p.getSourceMac() != null ? p.getSourceMac() : "Host Interface MAC"));
        sb.append(String.format("    VLAN ID:         %d\n", p.getVlanId()));
        sb.append("    Type:            IPv4 (0x0800)\n\n");

        sb.append("▶ LAYER 3 - INTERNET PROTOCOL VERSION 4 (IPv4)\n");
        sb.append("    Version:         4\n");
        sb.append("    Header Length:   20 bytes\n");
        sb.append(String.format("    Time to Live:    %d\n", p.getTtl()));
        sb.append(String.format("    Protocol:        %s\n", p.getProtocol().getCode()));
        sb.append(String.format("    Source IP:       %s\n", p.getSourceIp()));
        sb.append(String.format("    Destination IP:  %s\n\n", p.getDestIp()));

        if (p.getSourcePort() > 0 || p.getDestPort() > 0) {
            sb.append("▶ LAYER 4 - TRANSPORT LAYER (TCP / UDP)\n");
            sb.append(String.format("    Source Port:      %d\n", p.getSourcePort()));
            sb.append(String.format("    Destination Port: %d\n", p.getDestPort()));
            sb.append(String.format("    Flags:            SYN=%b, ACK=%b, FIN=%b\n\n", p.isSynFlag(), p.isAckFlag(), p.isFinFlag()));
        }

        sb.append("▶ LAYER 7 - APPLICATION LAYER / PAYLOAD\n");
        sb.append(String.format("    Protocol:        %s (%s)\n", p.getProtocol().getCode(), p.getProtocol().getDescription()));
        if (p.getHttpMethod() != null) {
            sb.append(String.format("    HTTP Method:     %s %s\n", p.getHttpMethod(), p.getHttpUrl()));
        }
        if (p.getDnsQuery() != null) {
            sb.append(String.format("    DNS Query:       %s\n", p.getDnsQuery()));
        }
        if (p.getDnsResolvedIp() != null) {
            sb.append(String.format("    DNS Resolved IP: %s\n", p.getDnsResolvedIp()));
        }
        sb.append(String.format("    Payload Data:\n    \"%s\"\n\n", p.getPayload().replace("\n", "\n    ")));

        sb.append("▶ ROUTE TRAJECTORY & HOPS\n");
        sb.append(String.format("    Path Taken:      %s\n", String.join(" -> ", p.getHopPath())));
        if (p.getDropReason() != null) {
            sb.append(String.format("    ⚠ DROP REASON:   %s\n", p.getDropReason()));
        }

        packetDetailsArea.setText(sb.toString());
        packetDetailsArea.setCaretPosition(0);
    }
}
