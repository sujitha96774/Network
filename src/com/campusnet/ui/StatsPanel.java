package com.campusnet.ui;

import com.campusnet.model.ProtocolType;
import com.campusnet.simulator.NetworkMetrics;
import com.campusnet.simulator.NetworkSimulator;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class StatsPanel extends JPanel {
    private final NetworkSimulator simulator;

    private JLabel lblSent;
    private JLabel lblDelivered;
    private JLabel lblDropped;
    private JLabel lblRate;
    private JLabel lblLatency;
    private JLabel lblBytes;

    private final ThroughputGraphPanel graphPanel = new ThroughputGraphPanel();

    public StatsPanel(NetworkSimulator simulator) {
        this.simulator = simulator;
        setLayout(new BorderLayout(5, 5));
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Top Metrics Cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 6, 8, 0));
        cardsPanel.setOpaque(false);

        cardsPanel.add(createCard("Packets Sent", lblSent = new JLabel("0"), Theme.ACCENT_BLUE));
        cardsPanel.add(createCard("Delivered", lblDelivered = new JLabel("0"), Theme.ACCENT_GREEN));
        cardsPanel.add(createCard("Dropped", lblDropped = new JLabel("0"), Theme.ACCENT_RED));
        cardsPanel.add(createCard("Delivery Rate", lblRate = new JLabel("100.0%"), Theme.ACCENT_PURPLE));
        cardsPanel.add(createCard("Avg Latency", lblLatency = new JLabel("0.0 ms"), Theme.ACCENT_ORANGE));
        cardsPanel.add(createCard("Transferred", lblBytes = new JLabel("0 KB"), Theme.TEXT_PRIMARY));

        add(cardsPanel, BorderLayout.NORTH);

        // Center Live Chart
        add(graphPanel, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, JLabel valLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(Theme.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 100), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_SMALL);
        titleLbl.setForeground(Theme.TEXT_MUTED);

        valLabel.setFont(Theme.FONT_BOLD);
        valLabel.setForeground(accent);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valLabel, BorderLayout.CENTER);
        return card;
    }

    public void updateStats(NetworkMetrics metrics) {
        SwingUtilities.invokeLater(() -> {
            lblSent.setText(String.valueOf(metrics.getTotalPacketsSent()));
            lblDelivered.setText(String.valueOf(metrics.getTotalPacketsDelivered()));
            lblDropped.setText(String.valueOf(metrics.getTotalPacketsDropped()));
            lblRate.setText(String.format("%.1f%%", metrics.getDeliveryRate()));
            lblLatency.setText(String.format("%.1f ms", metrics.getAverageLatency()));
            lblBytes.setText(String.format("%.1f KB", metrics.getTotalBytesTransferred() / 1024.0));

            graphPanel.repaint();
        });
    }

    private class ThroughputGraphPanel extends JPanel {
        public ThroughputGraphPanel() {
            setBackground(Theme.BG_DARK);
            setPreferredSize(new Dimension(100, 65));
            setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            NetworkMetrics m = simulator.getMetrics();
            List<Long> history = m.getThroughputHistory();
            if (history.isEmpty()) {
                g2d.dispose();
                return;
            }

            int w = getWidth();
            int h = getHeight();

            // Find Max Value for scaling
            long maxVal = 10;
            for (Long val : history) {
                if (val > maxVal) maxVal = val;
            }

            // Draw Grid Lines
            g2d.setColor(new Color(51, 65, 85, 100));
            g2d.drawLine(0, h / 2, w, h / 2);
            g2d.drawLine(0, h / 4, w, h / 4);
            g2d.drawLine(0, 3 * h / 4, w, 3 * h / 4);

            // Draw Graph Line
            g2d.setColor(Theme.ACCENT_GREEN);
            g2d.setStroke(new BasicStroke(2.0f));

            int n = history.size();
            int[] xPoints = new int[n];
            int[] yPoints = new int[n];

            for (int i = 0; i < n; i++) {
                xPoints[i] = (i * w) / (n - 1);
                double norm = (double) history.get(i) / maxVal;
                yPoints[i] = h - 6 - (int) (norm * (h - 14));
            }

            for (int i = 0; i < n - 1; i++) {
                g2d.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            }

            // Labels
            g2d.setFont(Theme.FONT_SMALL);
            g2d.setColor(Theme.TEXT_MUTED);
            g2d.drawString(String.format("Live Real-Time Throughput [Peak: %d Kbps]", maxVal), 8, 14);

            g2d.dispose();
        }
    }
}
