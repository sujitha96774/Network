package com.campusnet.ui;

import com.campusnet.model.Device;
import com.campusnet.model.ProtocolType;
import com.campusnet.simulator.NetworkSimulator;
import com.campusnet.simulator.TrafficGenerator;

import javax.swing.*;
import java.awt.*;

public class TrafficWizardDialog extends JDialog {
    private final NetworkSimulator simulator;
    private final TrafficGenerator trafficGenerator;

    private JComboBox<Device> srcCombo;
    private JComboBox<Device> dstCombo;
    private JComboBox<String> trafficTypeCombo;
    private JTextField payloadField;
    private JSpinner countSpinner;

    public TrafficWizardDialog(Frame parent, NetworkSimulator simulator, Device initialSrc, String initialType) {
        super(parent, "Traffic Generator Wizard - Multi-Area Simulator", true);
        this.simulator = simulator;
        this.trafficGenerator = new TrafficGenerator(simulator);

        setSize(520, 380);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout(10, 10));

        // Title Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_PANEL);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel title = new JLabel("Network Packet & Traffic Generator Wizard");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Form Body
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG_DARK);
        form.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // 1. Source Device
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel srcLbl = new JLabel("Source Device:");
        srcLbl.setForeground(Theme.TEXT_PRIMARY);
        form.add(srcLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        srcCombo = new JComboBox<>(simulator.getDevices().toArray(new Device[0]));
        if (initialSrc != null) srcCombo.setSelectedItem(initialSrc);
        form.add(srcCombo, gbc);

        // 2. Destination Device
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel dstLbl = new JLabel("Destination Device / Server:");
        dstLbl.setForeground(Theme.TEXT_PRIMARY);
        form.add(dstLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        dstCombo = new JComboBox<>(simulator.getDevices().toArray(new Device[0]));
        if (dstCombo.getItemCount() > 1) dstCombo.setSelectedIndex(1);
        form.add(dstCombo, gbc);

        // 3. Traffic Type
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel typeLbl = new JLabel("Protocol / Traffic Type:");
        typeLbl.setForeground(Theme.TEXT_PRIMARY);
        form.add(typeLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        trafficTypeCombo = new JComboBox<>(new String[]{
                "ICMP Ping Echo (Network Diagnostics)",
                "HTTP Web Request (GET Portal / Webpage)",
                "DNS Domain Resolution Query",
                "Continuous TCP Data Stream (Video/FTP)",
                "DDoS SYN Flood Attack Burst"
        });
        if ("HTTP".equalsIgnoreCase(initialType)) trafficTypeCombo.setSelectedIndex(1);
        form.add(trafficTypeCombo, gbc);

        // 4. Custom Parameter / Domain / URL
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel paramLbl = new JLabel("Payload / URL / Domain:");
        paramLbl.setForeground(Theme.TEXT_PRIMARY);
        form.add(paramLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        payloadField = new JTextField("portal.campus.edu");
        form.add(payloadField, gbc);

        // 5. Packet Count / Rate
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel countLbl = new JLabel("Packet Count / Duration:");
        countLbl.setForeground(Theme.TEXT_PRIMARY);
        form.add(countLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        countSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 100, 1));
        form.add(countSpinner, gbc);

        add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.setBackground(Theme.BG_PANEL);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel);

        JButton btnSend = new JButton("Transmit Traffic");
        btnSend.setFont(Theme.FONT_BOLD);
        btnSend.setBackground(Theme.ACCENT_GREEN);
        btnSend.setForeground(Color.WHITE);
        btnSend.addActionListener(e -> executeTraffic());
        btnPanel.add(btnSend);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void executeTraffic() {
        Device src = (Device) srcCombo.getSelectedItem();
        Device dst = (Device) dstCombo.getSelectedItem();
        int typeIdx = trafficTypeCombo.getSelectedIndex();
        int count = (Integer) countSpinner.getValue();
        String payload = payloadField.getText().trim();

        if (src == null || dst == null) {
            JOptionPane.showMessageDialog(this, "Please select both source and destination devices.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String srcIp = src.getPrimaryIp();
        String dstIp = dst.getPrimaryIp();

        switch (typeIdx) {
            case 0: // ICMP Ping
                trafficGenerator.sendPing(srcIp, dstIp, count);
                break;
            case 1: // HTTP Web Request
                trafficGenerator.sendHttpRequest(srcIp, dstIp, payload.isEmpty() ? "/" : payload);
                break;
            case 2: // DNS Resolution
                trafficGenerator.sendDnsQuery(srcIp, dstIp, payload.isEmpty() ? "portal.campus.edu" : payload);
                break;
            case 3: // Continuous TCP Stream
                trafficGenerator.startContinuousTraffic(srcIp, dstIp, ProtocolType.TCP_SYN, 5, Math.min(count, 30));
                break;
            case 4: // DDoS Attack Burst
                trafficGenerator.sendDdosBurst("198.51.100.", dstIp, count * 5);
                break;
        }

        dispose();
    }
}
