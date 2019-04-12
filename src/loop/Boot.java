package loop;

import loop.io.FileUtil;
import loop.io.Settings;
import loop.ui.Overlay;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

public class Boot {
    private static final Logger logger = LoggerFactory.getLogger(Boot.class);
    public static PcapNetworkInterface nif = null;
    public static HashMap<Inet4Address, Timestamp> active = new HashMap<>();

    private static InetAddress localAddr;
    private static PcapHandle handle = null;
    private static Overlay ui;
    private static boolean running = true;

    public static void main(String[] args) throws UnsupportedLookAndFeelException, AWTException, ClassNotFoundException, InterruptedException,
            InstantiationException, IllegalAccessException, IOException, PcapNativeException, NotOpenException {
        System.setProperty("jna.nosys", "true");
        if (!Sanity.check()) {
            System.exit(1);
        }
        Settings.init();
        Settings.set("autoload", Settings.get("autoload", "0")); //"autoload" is an ini-only toggle for advanced users.
        setupTray();

        getLocalAddr();
        nif = Pcaps.getDevByAddress(localAddr);
        if (nif == null) {
            JOptionPane.showMessageDialog(null,
                    "The device you selected doesn't seem to exist. Double-check the IP you entered.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        final int snapLen = 65536;
        final PromiscuousMode mode = PromiscuousMode.NONPROMISCUOUS;
        final int timeout = 0;
        handle = nif.openLive(snapLen, mode, timeout);

        // Berkley Packet Filter (BPF): http://biot.com/capstats/bpf.html
        handle.setFilter("udp && less 150", BpfProgram.BpfCompileMode.OPTIMIZE);

        ui = new Overlay();

        if (Settings.SIMULATE_TRAFFIC) {
            simulateTraffic();
            return;
        }

        while (running) {

            final Packet packet = handle.getNextPacket();

            if (packet != null) {
                final IpV4Packet ippacket = packet.get(IpV4Packet.class);

                if (ippacket != null) {
                    final UdpPacket udppack = ippacket.get(UdpPacket.class);

                    if (udppack != null && udppack.getPayload() != null) {
                        final Inet4Address srcAddr = ippacket.getHeader().getSrcAddr();
                        final Inet4Address dstAddr = ippacket.getHeader().getDstAddr();
                        final int payloadLen = udppack.getPayload().getRawData().length;

                        //Packets are STUN related: 56 is request, 68 is response
                        if (active.containsKey(srcAddr) && !srcAddr.equals(localAddr)) {
                            // it's a response from a peer to the local address
                            if (active.get(srcAddr) != null && payloadLen == 68 && dstAddr.equals(localAddr)) {
                                int ping = (int) (handle.getTimestamp().getTime() - active.get(srcAddr).getTime());
                                ui.setPing(ippacket.getHeader().getSrcAddr(), ping);
                                active.put(srcAddr, null); //No longer expect ping
                            }
                        } else {
                            if (payloadLen == 56 && srcAddr.equals(localAddr)) {
                                // it's a request from the local address to a peer
                                // we will store the peer address
                                Inet4Address peerAddresss = ippacket.getHeader().getDstAddr();
                                active.put(peerAddresss, handle.getTimestamp());
                            }
                        }
                    }
                }
            }
        }
    }


    private static void simulateTraffic() throws UnknownHostException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long endTime1 = startTime + 350000;
        long endTime2 = startTime + 500;

        while (running) {
            long currentTime = System.currentTimeMillis();

            if (currentTime <= endTime1) {
                ui.setPing((Inet4Address) Inet4Address.getByName("1.2.3.4"), (int) (Math.random() * 300));
            }

            if (currentTime <= endTime2) {
                ui.setPing((Inet4Address) Inet4Address.getByName("1.2.3.5"), (int) (Math.random() * 300));
            }

            Thread.sleep(2000);
        }
    }


    public static void setupTray() throws AWTException {
        final SystemTray tray = SystemTray.getSystemTray();
        final PopupMenu popup = new PopupMenu();
        final MenuItem info = new MenuItem();
        final MenuItem exit = new MenuItem();
        final TrayIcon trayIcon = new TrayIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Constants.APP_SHORT_NAME, popup);
        try {
            InputStream is = FileUtil.localResource("icon.png");
            trayIcon.setImage(ImageIO.read(is));
            is.close();
        } catch (IOException e1) {
            logger.error("Failed to load application icon.", e1);
        }

        info.addActionListener(e -> {
            String message = ""
                    + Constants.APP_SHORT_NAME + " is a tool to provide Dead By Daylight players more information about the lobby hosts.\n\n"
                    + "Features:\n"
                    + "======\n"
                    + "- Ping display:\n"
                    + "      The ping against the lobby/match host will be displayed on the overlay.\n\n"
                    + "- Rate user hosting the lobby/match:\n"
                    + "      As soon as the host name is detected, hold Shift and click on the name.\n"
                    + "      With every click, you will cycle between thumbs down, thumbs up and unrated.\n\n"
                    + "- Attach a description to the lobby/match host:\n"
                    + "      As soon as the host name is detected, right-click on the overlay to add/edit a description.\n\n"
                    + "- Visit the host's Steam profile:\n"
                    + "      As soon as the host name is detected, hold Shift and click on the Steam icon.\n"
                    + "      It will attempt to open the default browser on the host's Steam profile page.\n\n"
                    + "- Re-position the overlay:\n"
                    + "    Double-click to lock/unlock the overlay for dragging.\n"
                    + "\n"
                    + "Credits:\n"
                    + "=====\n"
                    + "Author of this fork: NickyRamone\n"
                    + "Original version and core: MLGA project, by PsiLupan & ShadowMoose";
            JOptionPane.showMessageDialog(null, message, Constants.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
        });

        exit.addActionListener(e -> {
            running = false;
            tray.remove(trayIcon);
            ui.close();
            logger.info("Terminated UI.");
            logger.info("Cleaning up system resources. Could take a while...");
            handle.close();
            logger.info("Killed handle.");
            System.exit(0);
        });
        info.setLabel("Help");
        exit.setLabel("Exit");
        popup.add(info);
        popup.add(exit);
        tray.add(trayIcon);
    }

    public static void getLocalAddr() throws InterruptedException, PcapNativeException, UnknownHostException, SocketException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        if (Settings.getDouble("autoload", 0) == 1) {
            localAddr = InetAddress.getByName(Settings.get("addr", ""));
            return;
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final JFrame frame = new JFrame("Network Device");
        frame.setFocusableWindowState(true);

        final JLabel ipLab = new JLabel("Select LAN IP obtained from Network Settings:", JLabel.LEFT);
        final JComboBox<String> lanIP = new JComboBox<String>();
        final JLabel lanLabel = new JLabel("If your device IP isn't in the dropdown, provide it below.");
        final JTextField lanText = new JTextField(Settings.get("addr", ""));

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
                        Settings.set("addr", xAddr.getHostAddress().replaceAll("/", ""));

                        if (Settings.AUTOSELECT_NETWORK_INTERFACE) {
                            localAddr = xAddr;
                            return;
                        }
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
                Settings.set("addr", localAddr.getHostAddress().replaceAll("/", ""));
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
            Thread.sleep(10);
    }


}
