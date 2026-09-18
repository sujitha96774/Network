package com.campusnet;

import com.campusnet.cli.VirtualCLI;
import com.campusnet.model.Device;
import com.campusnet.simulator.NetworkSimulator;
import com.campusnet.simulator.ScenarioManager;
import com.campusnet.ui.MainFrame;

import javax.swing.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean cliMode = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                cliMode = true;
                break;
            }
        }

        NetworkSimulator simulator = new NetworkSimulator();
        ScenarioManager.buildDefaultCampusTopology(simulator);

        if (cliMode) {
            runCliInteractive(simulator);
        } else {
            runGui(simulator);
        }
    }

    private static void runGui(NetworkSimulator simulator) {
        // Set Look and Feel to modern system or FlatLaf style
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(simulator);
            frame.setVisible(true);
        });
    }

    private static void runCliInteractive(NetworkSimulator simulator) {
        VirtualCLI cli = new VirtualCLI(simulator);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=========================================================================");
        System.out.println(" Multi-Area Campus Network Simulator (LAN, MAN, WAN) - Headless CLI Mode");
        System.out.println("=========================================================================");
        System.out.println("Active Devices in Campus Network:");
        for (Device d : simulator.getDevices()) {
            System.out.printf("  * %-20s [%-10s] IP: %-16s Area: %s\n",
                    d.getName(), d.getDeviceType(), d.getPrimaryIp(), d.getAreaType().name());
        }
        System.out.println("-------------------------------------------------------------------------");

        Device currentDevice = simulator.getDevices().get(1); // Default to CS-Student-01
        System.out.println("Current active device: " + currentDevice.getName() + " (" + currentDevice.getPrimaryIp() + ")");
        System.out.println("Type 'help' for commands, 'switch <device_name>' to change device, 'exit' to quit.\n");

        while (true) {
            System.out.print(currentDevice.getName() + "# ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                break;
            }
            if (line.toLowerCase().startsWith("switch ")) {
                String devName = line.substring(7).trim();
                Device found = simulator.findDeviceByName(devName);
                if (found != null) {
                    currentDevice = found;
                    System.out.println("Switched to device: " + currentDevice.getName() + " [" + currentDevice.getPrimaryIp() + "]");
                } else {
                    System.out.println("Device '" + devName + "' not found.");
                }
                continue;
            }

            String output = cli.executeCommand(currentDevice, line);
            if ("__CLEAR__".equals(output)) {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            } else {
                System.out.println(output);
            }
        }

        simulator.shutdown();
        System.out.println("Simulator exited.");
    }
}
