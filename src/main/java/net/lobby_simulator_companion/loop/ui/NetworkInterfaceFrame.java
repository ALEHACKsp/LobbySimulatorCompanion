package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.Settings;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class NetworkInterfaceFrame extends JFrame {

    private static final String SETTING__NIF_ADDRESS = "network.interface.address";
    private static final Logger logger = LoggerFactory.getLogger(NetworkInterfaceFrame.class);

    private InetAddress localAddr;


    public NetworkInterfaceFrame(Settings settings) throws Exception {
        final JFrame frame = new JFrame("Network Device");
        frame.setFocusableWindowState(true);

        final JLabel ipLab = new JLabel("Select LAN IP obtained from Network Settings:", JLabel.LEFT);
        final JComboBox<String> lanIP = new JComboBox<>();
        final JLabel lanLabel = new JLabel("If your device IP isn't in the dropdown, provide it below.");
        final JTextField lanText = new JTextField(settings.get(SETTING__NIF_ADDRESS, ""));

        ArrayList<InetAddress> inets = new ArrayList<InetAddress>();

        for (PcapNetworkInterface i : Pcaps.findAllDevs()) {
            for (PcapAddress x : i.getAddresses()) {
                InetAddress xAddr = x.getAddress();
                if (xAddr != null && x.getNetmask() != null && !xAddr.toString().equals("/0.0.0.0")) {
                    NetworkInterface inf = NetworkInterface.getByInetAddress(x.getAddress());
                    if (inf != null && inf.isUp() && !inf.isVirtual()) {
                        inets.add(xAddr);
                        lanIP.addItem((lanIP.getItemCount() + 1) + " - " + inf.getDisplayName() + " ::: " + xAddr.getHostAddress());
                        logger.info("Found: {} - {} ::: {}", lanIP.getItemCount(), inf.getDisplayName(), xAddr.getHostAddress());
                        settings.set(SETTING__NIF_ADDRESS, xAddr.getHostAddress().replaceAll("/", ""));
                    }
                }
            }
        }

        if (lanIP.getItemCount() == 0) {
            JOptionPane.showMessageDialog(null, "Unable to locate devices.\nPlease try running the program in Admin Mode.\nIf this does not work, you may need to reboot your computer.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        lanIP.setFocusable(false);
        final JButton start = new JButton("Start");
        start.addActionListener(e -> {
            try {
                if (lanText.getText().length() >= 7 && !lanText.getText().equals("0.0.0.0")) { // 7 is because the minimum field is 0.0.0.0
                    localAddr = InetAddress.getByName(lanText.getText());
                    logger.debug("Using IP from textfield: {}", lanText.getText());
                } else {
                    localAddr = inets.get(lanIP.getSelectedIndex());
                    logger.debug("Using device from dropdown: {}", lanIP.getSelectedItem());
                }
                settings.set(SETTING__NIF_ADDRESS, localAddr.getHostAddress().replaceAll("/", ""));
                frame.setVisible(false);
                frame.dispose();
            } catch (UnknownHostException e1) {
                logger.error("Encountered an invalid address.", e1);
            }
        });

        frame.setLayout(new GridLayout(5, 1));
        frame.add(ipLab);
        frame.add(lanIP);
        frame.add(lanLabel);
        frame.add(lanText);
        frame.add(start);
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (frame.isVisible())
            Thread.sleep(500);
    }
}
