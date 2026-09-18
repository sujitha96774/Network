package com.campusnet.ui;

import com.campusnet.model.*;
import com.campusnet.simulator.NetworkSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

public class TopologyPanel extends JPanel {
    private final NetworkSimulator simulator;
    private final MainFrame mainFrame;

    private Device selectedDevice = null;
    private Device draggedDevice = null;
    private Point dragOffset = new Point();
    private NetworkLink hoveredLink = null;

    public TopologyPanel(NetworkSimulator simulator, MainFrame mainFrame) {
        this.simulator = simulator;
        this.mainFrame = mainFrame;
        setBackground(Theme.BG_CANVAS);
        setFocusable(true);

        setupMouseListeners();
    }

    private void setupMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Device clicked = findDeviceAt(e.getPoint());
                if (clicked != null) {
                    selectedDevice = clicked;
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        draggedDevice = clicked;
                        dragOffset.x = e.getX() - clicked.getX();
                        dragOffset.y = e.getY() - clicked.getY();
                    }
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        mainFrame.openDeviceInspector(clicked);
                    }
                } else {
                    selectedDevice = null;
                    NetworkLink link = findLinkAt(e.getPoint());
                    if (link != null && SwingUtilities.isRightMouseButton(e)) {
                        showLinkContextMenu(e.getX(), e.getY(), link);
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedDevice != null) {
                    draggedDevice = null;
                    repaint();
                }
                if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
                    Device clicked = findDeviceAt(e.getPoint());
                    if (clicked != null) {
                        showDeviceContextMenu(e.getX(), e.getY(), clicked);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedDevice != null) {
                    draggedDevice.setX(Math.max(40, Math.min(getWidth() - 40, e.getX() - dragOffset.x)));
                    draggedDevice.setY(Math.max(40, Math.min(getHeight() - 40, e.getY() - dragOffset.y)));
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                NetworkLink link = findLinkAt(e.getPoint());
                if (link != hoveredLink) {
                    hoveredLink = link;
                    repaint();
                }
                Device dev = findDeviceAt(e.getPoint());
                if (dev != null) {
                    setToolTipText(String.format("<html><b>%s</b> (%s)<br/>IP: %s<br/>MAC: %s<br/>Area: %s<br/><i>%s</i></html>",
                            dev.getName(), dev.getDeviceType(), dev.getPrimaryIp(), dev.getPrimaryMac(),
                            dev.getAreaType().getShortName(), dev.getDescription()));
                } else if (link != null) {
                    setToolTipText(String.format("<html><b>Link: %s</b> [%s]<br/>Latency: %.1f ms | Bandwidth: %.0f Mbps<br/>Status: %s<br/><i>Right-click to Toggle UP/DOWN</i></html>",
                            link.getId(), link.getAreaType().name(), link.getLatencyMs(), link.getBandwidthMbps(),
                            link.isUp() ? "UP" : "DOWN (FAILED)"));
                } else {
                    setToolTipText(null);
                }
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void showDeviceContextMenu(int x, int y, Device device) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemInspect = new JMenuItem("Inspect Device & Open CLI Terminal");
        itemInspect.addActionListener(ev -> mainFrame.openDeviceInspector(device));
        menu.add(itemInspect);

        menu.addSeparator();

        JMenuItem itemPing = new JMenuItem("Send Ping from this device...");
        itemPing.addActionListener(ev -> mainFrame.openTrafficWizardForDevice(device, "PING"));
        menu.add(itemPing);

        JMenuItem itemHttp = new JMenuItem("Send HTTP Web Request from this device...");
        itemHttp.addActionListener(ev -> mainFrame.openTrafficWizardForDevice(device, "HTTP"));
        menu.add(itemHttp);

        menu.addSeparator();

        JMenuItem itemToggleState = new JMenuItem(device.isOnline() ? "Set Offline (Power Off)" : "Set Online (Power On)");
        itemToggleState.addActionListener(ev -> {
            device.setOnline(!device.isOnline());
            simulator.recalculateRouting();
            repaint();
        });
        menu.add(itemToggleState);

        menu.show(this, x, y);
    }

    private void showLinkContextMenu(int x, int y, NetworkLink link) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemToggle = new JMenuItem(link.isUp() ? "Fail Link (Disconnect Cable)" : "Restore Link (Reconnect Cable)");
        itemToggle.addActionListener(ev -> {
            simulator.toggleLinkState(link);
            repaint();
        });
        menu.add(itemToggle);
        menu.show(this, x, y);
    }

    private Device findDeviceAt(Point p) {
        for (Device dev : simulator.getDevices()) {
            int dx = p.x - dev.getX();
            int dy = p.y - dev.getY();
            if (dx * dx + dy * dy <= 28 * 28) {
                return dev;
            }
        }
        return null;
    }

    private NetworkLink findLinkAt(Point p) {
        for (NetworkLink link : simulator.getLinks()) {
            Device devA = link.getInterfaceA() != null ? link.getInterfaceA().getOwnerDevice() : null;
            Device devB = link.getInterfaceB() != null ? link.getInterfaceB().getOwnerDevice() : null;
            if (devA != null && devB != null) {
                double dist = Line2D.ptSegDist(devA.getX(), devA.getY(), devB.getX(), devB.getY(), p.x, p.y);
                if (dist <= 8.0) {
                    return link;
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // 1. Draw Area Background Group Zones
        drawAreaZones(g2d);

        // 2. Draw Network Links
        drawLinks(g2d);

        // 3. Draw In-Flight Traveling Packets
        drawInFlightPackets(g2d);

        // 4. Draw Device Nodes
        drawDevices(g2d);

        // 5. Draw Legend / Status Overlay
        drawCanvasLegend(g2d);

        g2d.dispose();
    }

    private void drawAreaZones(Graphics2D g2d) {
        // LAN Zone 1: Academic Building A (Left)
        drawStyledZone(g2d, 20, 20, 360, 500, "CAMPUS BUILDING A (Academic & Admin LANs)", AreaType.LAN);

        // LAN Zone 2: Research Building B & Data Center (Right)
        drawStyledZone(g2d, 400, 20, 400, 500, "CAMPUS BUILDING B (Research Lab & Central DC LANs)", AreaType.LAN);

        // MAN Backbone Zone (Center Bridge)
        drawStyledZone(g2d, 240, 220, 320, 240, "CAMPUS METROPOLITAN AREA NETWORK (MAN FIBER RING)", AreaType.MAN);

        // WAN Zone (Bottom / Remote)
        drawStyledZone(g2d, 20, 530, 950, 160, "WIDE AREA NETWORK (WAN - ISP CORE, REMOTE BRANCH & CLOUD)", AreaType.WAN);
    }

    private void drawStyledZone(Graphics2D g2d, int x, int y, int w, int h, String title, AreaType area) {
        Color fill = area == AreaType.LAN ? Theme.AREA_LAN_BG :
                (area == AreaType.MAN ? Theme.AREA_MAN_BG : Theme.AREA_WAN_BG);
        Color border = area.getColor();

        g2d.setColor(fill);
        g2d.fillRoundRect(x, y, w, h, 20, 20);

        g2d.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 120));
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 4}, 0));
        g2d.drawRoundRect(x, y, w, h, 20, 20);

        // Title Badge
        g2d.setFont(Theme.FONT_SMALL);
        FontMetrics fm = g2d.getFontMetrics();
        int titleW = fm.stringWidth(title) + 16;
        g2d.setColor(new Color(border.getRed(), border.getGreen(), border.getBlue(), 220));
        g2d.fillRoundRect(x + 12, y - 9, titleW, 18, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawString(title, x + 20, y + 4);
    }

    private void drawLinks(Graphics2D g2d) {
        for (NetworkLink link : simulator.getLinks()) {
            Device devA = link.getInterfaceA() != null ? link.getInterfaceA().getOwnerDevice() : null;
            Device devB = link.getInterfaceB() != null ? link.getInterfaceB().getOwnerDevice() : null;
            if (devA == null || devB == null) continue;

            int x1 = devA.getX();
            int y1 = devA.getY();
            int x2 = devB.getX();
            int y2 = devB.getY();

            boolean isHovered = (link == hoveredLink);

            if (!link.isUp()) {
                // Link is DOWN (Failure state)
                g2d.setColor(new Color(239, 68, 68, 180));
                g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{8, 6}, 0));
                g2d.drawLine(x1, y1, x2, y2);

                // Draw Red "X" in middle
                int midX = (x1 + x2) / 2;
                int midY = (y1 + y2) / 2;
                g2d.setColor(Theme.ACCENT_RED);
                g2d.setFont(Theme.FONT_MONO_BOLD);
                g2d.drawString("✖ LINK DOWN", midX - 35, midY - 6);
            } else {
                // Link is UP
                Color linkColor;
                float strokeW;
                if (link.getAreaType() == AreaType.MAN) {
                    linkColor = isHovered ? Color.WHITE : Theme.ACCENT_GREEN;
                    strokeW = 3.5f;
                } else if (link.getAreaType() == AreaType.WAN) {
                    linkColor = isHovered ? Color.WHITE : Theme.ACCENT_ORANGE;
                    strokeW = 2.5f;
                } else {
                    linkColor = isHovered ? Color.WHITE : Theme.ACCENT_BLUE;
                    strokeW = 2.0f;
                }

                g2d.setColor(new Color(linkColor.getRed(), linkColor.getGreen(), linkColor.getBlue(), isHovered ? 255 : 190));
                g2d.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(x1, y1, x2, y2);

                // Link latency label
                int midX = (x1 + x2) / 2;
                int midY = (y1 + y2) / 2;
                g2d.setFont(Theme.FONT_SMALL);
                String tag = String.format("%.0fms", link.getLatencyMs());
                g2d.setColor(new Color(15, 23, 42, 200));
                FontMetrics fm = g2d.getFontMetrics();
                int tw = fm.stringWidth(tag) + 6;
                g2d.fillRoundRect(midX - tw / 2, midY - 7, tw, 14, 4, 4);
                g2d.setColor(Theme.TEXT_MUTED);
                g2d.drawString(tag, midX - tw / 2 + 3, midY + 4);
            }
        }
    }

    private void drawInFlightPackets(Graphics2D g2d) {
        List<Packet> packets = simulator.getActiveInFlightPackets();
        for (Packet p : packets) {
            NetworkLink link = p.getCurrentLink();
            if (link == null) continue;

            Device fromDev = p.getCurrentDevice();
            Device toDev = link.getOtherDevice(fromDev);
            if (fromDev == null || toDev == null) continue;

            double prog = p.getProgressOnLink();
            double curX = fromDev.getX() + (toDev.getX() - fromDev.getX()) * prog;
            double curY = fromDev.getY() + (toDev.getY() - fromDev.getY()) * prog;

            // Glowing circle
            Color pColor = p.getProtocol().getColor();
            g2d.setColor(new Color(pColor.getRed(), pColor.getGreen(), pColor.getBlue(), 80));
            g2d.fillOval((int) curX - 12, (int) curY - 12, 24, 24);

            g2d.setColor(pColor);
            g2d.fillOval((int) curX - 7, (int) curY - 7, 14, 14);
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) curX - 7, (int) curY - 7, 14, 14);

            // Packet Badge Tag (e.g. "ICMP", "HTTP", "DNS")
            String tag = p.getProtocol().getCode();
            g2d.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2d.getFontMetrics();
            int tw = fm.stringWidth(tag) + 8;
            g2d.setColor(new Color(15, 23, 42, 220));
            g2d.fillRoundRect((int) curX - tw / 2, (int) curY - 24, tw, 14, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.drawString(tag, (int) curX - tw / 2 + 4, (int) curY - 13);
        }
    }

    private void drawDevices(Graphics2D g2d) {
        for (Device dev : simulator.getDevices()) {
            int cx = dev.getX();
            int cy = dev.getY();
            boolean isSelected = (dev == selectedDevice);

            // Node Circle Base
            int radius = 22;
            Color devColor = getNodeColor(dev);

            if (isSelected) {
                g2d.setColor(new Color(255, 255, 255, 120));
                g2d.fillOval(cx - radius - 6, cy - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
            }

            // Glow
            g2d.setColor(new Color(devColor.getRed(), devColor.getGreen(), devColor.getBlue(), 60));
            g2d.fillOval(cx - radius - 3, cy - radius - 3, (radius + 3) * 2, (radius + 3) * 2);

            // Body
            g2d.setColor(dev.isOnline() ? Theme.BG_PANEL : new Color(75, 85, 99));
            g2d.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Border
            g2d.setColor(dev.isOnline() ? devColor : Color.GRAY);
            g2d.setStroke(new BasicStroke(isSelected ? 3.0f : 2.0f));
            g2d.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

            // Device Icon Symbol
            drawDeviceIcon(g2d, cx, cy, dev);

            // Device Name Label
            g2d.setFont(Theme.FONT_BOLD);
            FontMetrics fm = g2d.getFontMetrics();
            int nw = fm.stringWidth(dev.getName());
            g2d.setColor(Theme.TEXT_PRIMARY);
            g2d.drawString(dev.getName(), cx - nw / 2, cy + radius + 15);

            // IP Address Label
            g2d.setFont(Theme.FONT_SMALL);
            fm = g2d.getFontMetrics();
            String ipStr = dev.getPrimaryIp();
            int iw = fm.stringWidth(ipStr);
            g2d.setColor(Theme.TEXT_MUTED);
            g2d.drawString(ipStr, cx - iw / 2, cy + radius + 27);
        }
    }

    private void drawDeviceIcon(Graphics2D g2d, int cx, int cy, Device dev) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(Theme.FONT_HEADER);
        FontMetrics fm = g2d.getFontMetrics();

        String symbol = "PC";
        if (dev instanceof RouterDevice) {
            symbol = "RTR"; // Router
        } else if (dev instanceof SwitchDevice) {
            symbol = "SW";  // Switch
        } else if (dev instanceof ServerDevice) {
            symbol = "SRV"; // Server
        } else {
            symbol = "PC";  // Host PC
        }

        int sw = fm.stringWidth(symbol);
        g2d.drawString(symbol, cx - sw / 2, cy + 5);
    }

    private Color getNodeColor(Device dev) {
        if (!dev.isOnline()) return Color.GRAY;
        if (dev instanceof RouterDevice) return Theme.ACCENT_ORANGE;
        if (dev instanceof SwitchDevice) return Theme.ACCENT_BLUE;
        if (dev instanceof ServerDevice) return Theme.ACCENT_PURPLE;
        return Theme.ACCENT_GREEN;
    }

    private void drawCanvasLegend(Graphics2D g2d) {
        int x = 30;
        int y = getHeight() - 40;

        g2d.setFont(Theme.FONT_SMALL);
        g2d.setColor(new Color(15, 23, 42, 220));
        g2d.fillRoundRect(x, y - 5, 520, 30, 8, 8);
        g2d.setColor(Theme.BORDER_COLOR);
        g2d.drawRoundRect(x, y - 5, 520, 30, 8, 8);

        int curX = x + 12;
        curX = drawLegendItem(g2d, curX, y + 15, Theme.ACCENT_BLUE, "LAN (Ethernet)");
        curX = drawLegendItem(g2d, curX, y + 15, Theme.ACCENT_GREEN, "MAN (10G Fiber)");
        curX = drawLegendItem(g2d, curX, y + 15, Theme.ACCENT_ORANGE, "WAN (ISP/Cloud)");
        curX = drawLegendItem(g2d, curX, y + 15, Theme.ACCENT_PURPLE, "Server");
        drawLegendItem(g2d, curX, y + 15, Theme.ACCENT_RED, "Link Down / Blocked");
    }

    private int drawLegendItem(Graphics2D g2d, int x, int y, Color c, String text) {
        g2d.setColor(c);
        g2d.fillRect(x, y - 8, 10, 10);
        g2d.setColor(Theme.TEXT_PRIMARY);
        g2d.drawString(text, x + 14, y);
        return x + 14 + g2d.getFontMetrics().stringWidth(text) + 16;
    }
}
