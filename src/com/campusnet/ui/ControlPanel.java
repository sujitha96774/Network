package com.campusnet.ui;

import com.campusnet.model.AreaType;
import com.campusnet.model.Device;
import com.campusnet.model.NetworkLink;
import com.campusnet.model.ProtocolType;
import com.campusnet.protocol.FirewallEngine;
import com.campusnet.simulator.NetworkSimulator;
import com.campusnet.simulator.ScenarioManager;
import com.campusnet.simulator.TrafficGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ControlPanel extends JPanel {
    private final NetworkSimulator simulator;
    private final MainFrame mainFrame;
    private final TrafficGenerator trafficGenerator;

    private JButton btnPlayPause;
    private JComboBox<String> scenarioCombo;
    private JSlider speedSlider;
    private JLabel speedValLbl;

    public ControlPanel(NetworkSimulator simulator, MainFrame mainFrame) {
        this.simulator = simulator;
        this.mainFrame = mainFrame;
        this.trafficGenerator = new TrafficGenerator(simulator);

        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_COLOR));

        // Play/Pause Button
        btnPlayPause = new JButton("|| Pause");
        btnPlayPause.setFont(Theme.FONT_BOLD);
        btnPlayPause.setBackground(Theme.ACCENT_BLUE);
        btnPlayPause.setForeground(Color.WHITE);
        btnPlayPause.addActionListener(e -> togglePlayPause());
        add(btnPlayPause);

        // Step Button
        JButton btnStep = new JButton(">| Step");
        btnStep.setFont(Theme.FONT_BODY);
        btnStep.addActionListener(e -> {
            simulator.step();
            mainFrame.repaint();
        });
        add(btnStep);

        add(new JSeparator(SwingConstants.VERTICAL));

        // Speed Slider
        JLabel speedLbl = new JLabel("Speed:");
        speedLbl.setForeground(Theme.TEXT_MUTED);
        speedLbl.setFont(Theme.FONT_BODY);
        add(speedLbl);

        speedSlider = new JSlider(2, 50, 10); // 0.2x to 5.0x (val / 10)
        speedSlider.setPreferredSize(new Dimension(100, 24));
        speedSlider.setBackground(Theme.BG_PANEL);
        speedSlider.addChangeListener(e -> {
            double speed = speedSlider.getValue() / 10.0;
            simulator.setSimulationSpeed(speed);
            speedValLbl.setText(String.format("%.1fx", speed));
        });
        add(speedSlider);

        speedValLbl = new JLabel("1.0x");
        speedValLbl.setForeground(Theme.TEXT_PRIMARY);
        speedValLbl.setFont(Theme.FONT_MONO);
        add(speedValLbl);

        add(new JSeparator(SwingConstants.VERTICAL));

        // Traffic Generator Wizard Button
        JButton btnWizard = new JButton("[+] Traffic Wizard");
        btnWizard.setFont(Theme.FONT_BOLD);
        btnWizard.setBackground(Theme.ACCENT_GREEN);
        btnWizard.setForeground(Color.WHITE);
        btnWizard.addActionListener(e -> mainFrame.openTrafficWizard());
        add(btnWizard);

        add(new JSeparator(SwingConstants.VERTICAL));

        // Presets & Scenarios Dropdown
        JLabel scenLbl = new JLabel("Presets:");
        scenLbl.setForeground(Theme.TEXT_MUTED);
        scenLbl.setFont(Theme.FONT_BODY);
        add(scenLbl);

        scenarioCombo = new JComboBox<>(new String[]{
                "1. Intra-Department LAN Ping (CS Lab)",
                "2. Inter-Building MAN Access (Web Portal)",
                "3. Remote WAN Branch Communication",
                "4. Cloud Web Access with Border NAT",
                "5. MAN Fiber Failure & SPF Dynamic Reroute",
                "6. DDoS Attack & Firewall Defense"
        });
        scenarioCombo.setFont(Theme.FONT_BODY);
        scenarioCombo.setPreferredSize(new Dimension(280, 26));
        add(scenarioCombo);

        JButton btnRunScenario = new JButton("Run Scenario");
        btnRunScenario.setFont(Theme.FONT_BOLD);
        btnRunScenario.setBackground(Theme.ACCENT_PURPLE);
        btnRunScenario.setForeground(Color.WHITE);
        btnRunScenario.addActionListener(e -> runSelectedScenario());
        add(btnRunScenario);

        add(new JSeparator(SwingConstants.VERTICAL));

        // Reset Button
        JButton btnReset = new JButton("Reset Network");
        btnReset.setFont(Theme.FONT_BODY);
        btnReset.addActionListener(e -> {
            ScenarioManager.buildDefaultCampusTopology(simulator);
            mainFrame.refreshAll();
        });
        add(btnReset);
    }

    private void togglePlayPause() {
        if (simulator.isPaused()) {
            simulator.play();
            btnPlayPause.setText("|| Pause");
            btnPlayPause.setBackground(Theme.ACCENT_BLUE);
        } else {
            simulator.pause();
            btnPlayPause.setText("> Resume");
            btnPlayPause.setBackground(Theme.ACCENT_GREEN);
        }
    }

    private void runSelectedScenario() {
        int idx = scenarioCombo.getSelectedIndex();
        switch (idx) {
            case 0: // Intra-Department LAN Ping
                simulator.log("--- RUNNING SCENARIO 1: Intra-Department LAN Ping (CS-Student-01 -> CS-Student-02) ---");
                trafficGenerator.sendPing("192.168.1.10", "192.168.1.11", 4);
                break;

            case 1: // Inter-Building MAN Web Access
                simulator.log("--- RUNNING SCENARIO 2: Inter-Building MAN Web Access (CS Dept -> Campus Web Portal) ---");
                trafficGenerator.sendDnsQuery("192.168.1.10", "10.0.1.10", "portal.campus.edu");
                CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS).execute(() -> {
                    trafficGenerator.sendHttpRequest("192.168.1.10", "10.0.1.50", "/");
                });
                break;

            case 2: // Remote WAN Branch Communication
                simulator.log("--- RUNNING SCENARIO 3: Remote WAN Branch Communication (CS Lab -> Remote Campus PC) ---");
                trafficGenerator.sendPing("192.168.1.10", "172.16.1.10", 4);
                break;

            case 3: // Cloud Web Access with Border NAT
                simulator.log("--- RUNNING SCENARIO 4: Public Cloud Web Access via Campus Border NAT Gateway ---");
                trafficGenerator.sendHttpRequest("192.168.2.10", "198.51.100.10", "/");
                break;

            case 4: // MAN Fiber Failure & SPF Dynamic Reroute
                simulator.log("--- RUNNING SCENARIO 5: Simulating MAN Primary Fiber Cable Cut & Dynamic Failover ---");
                NetworkLink primaryFiber = null;
                for (NetworkLink link : simulator.getLinks()) {
                    if ("MAN_FIBER_PRIMARY".equalsIgnoreCase(link.getId())) {
                        primaryFiber = link;
                        break;
                    }
                }
                if (primaryFiber != null) {
                    primaryFiber.setUp(false);
                    simulator.recalculateRouting();
                    simulator.log("[ALERT] Primary MAN Fiber CUT! Rerouting traffic via Secondary Ring Gateway Link...");
                    trafficGenerator.sendPing("192.168.1.10", "10.0.1.50", 4);
                }
                break;

            case 5: // DDoS Attack & Firewall Defense
                simulator.log("--- RUNNING SCENARIO 6: DDoS SYN Flood Attack & Firewall Protection Test ---");
                // Block attack range via firewall
                FirewallEngine fw = simulator.getFirewallEngine();
                fw.addRule(new FirewallEngine.Rule(1, FirewallEngine.Action.DENY, "198.51.100.0/24", "10.0.1.50", "ANY", 0, "DDoS Mitigation - Block Suspicious Cloud Net"));
                simulator.log("[FIREWALL] Rule #1 Injected: DENY 198.51.100.0/24 -> 10.0.1.50");
                trafficGenerator.sendDdosBurst("198.51.100.", "10.0.1.50", 15);
                break;
        }
    }
}
