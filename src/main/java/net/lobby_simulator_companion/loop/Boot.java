package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.service.*;
import net.lobby_simulator_companion.loop.ui.MainPanel;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.pcap4j.core.PcapAddress;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;

public class Boot {
    private static Logger logger;
    private static InetAddress localAddr;
    private static MainPanel ui;
    private static Sniffer sniffer;
    private static Settings settings;


    public static void main(String[] args) throws Exception {
//        configureLogger();
//        init();

        SwingUtilities.invokeLater(() -> {
            Factory.getMainPanel();
            Factory.getDebugPanel();
        });

    }

    public static void main2(String[] args) throws Exception {
        configureLogger();
        try {
            init();
        } catch (Exception e) {
            logger.error("Failed to initialize application: {}", e.getMessage(), e);
            fatalErrorDialog("Failed to initialize application: " + e.getMessage());
            exitApplication(1);
        }

        if (settings.getBoolean("debug")) {
            return;
        }

        try {
            sniffer = new Sniffer(localAddr, new SnifferListener() {
                @Override
                public void notifyNewConnection(Connection connection) {
                    ui.connect();
                }

                @Override
                public void notifyDisconnect() {
                    ui.disconnect();
                }

                @Override
                public void handleException(Exception e) {
                    logger.error("Fatal error while sniffing packets.", e);
                    fatalErrorDialog("A fatal error occurred while processing connections.\nPlease, send us the log files.");
                }
            });
        } catch (InvalidNetworkInterfaceException e) {
            fatalErrorDialog("The device you selected doesn't seem to exist. Double-check the IP you entered.");
        }

        // start sniffer thread
        Thread thread = new Thread(sniffer);
        thread.setDaemon(true);
        thread.start();
    }

    private static void configureLogger() throws URISyntaxException {
        URI execUri = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path appHome = new File(execUri).toPath().getParent();
        System.setProperty("app.home", appHome.toString());
        logger = LoggerFactory.getLogger(Boot.class);
    }


    private static void init() throws Exception {
        logger.info("Initializing...");
        Factory.init();
        settings = Factory.getSettings();
        System.setProperty("jna.nosys", "true");
        if (!Sanity.check()) {
            System.exit(1);
        }
        setupTray();

        logger.info("Setting up network interface...");
        getLocalAddr();

        logger.info("Starting UI...");
        ui = Factory.getMainPanel();

        logger.info("Starting log monitor...");
        DbdLogMonitor logMonitor = Factory.getDbdLogMonitor();
        logMonitor.addObserver(ui);
        Thread thread = new Thread(logMonitor);
        thread.setDaemon(true);
        thread.start();

        logger.info("Initialization finished.");

        if (settings.getBoolean("debug")) {
            Factory.getDebugPanel();
        }
    }


    public static void setupTray() throws AWTException, IOException {
        final AppProperties appProperties = Factory.getAppProperties();
        final SystemTray tray = SystemTray.getSystemTray();
        final PopupMenu popup = new PopupMenu();
        final MenuItem info = new MenuItem();
        final MenuItem exit = new MenuItem();

        BufferedImage trayIconImage = ImageIO.read(FileUtil.localResource("loop_logo.png"));
        int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
        TrayIcon trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip(appProperties.get("app.name"));

        info.addActionListener(e -> {
            String message = ""
                    + appProperties.get("app.name.short") + " is a tool to provide Dead By Daylight players more information about the lobby hosts.\n\n"
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

            String title = appProperties.get("app.name") + " " + appProperties.get("app.version");
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });

        exit.addActionListener(e -> {
            exitApplication(0);
        });
        info.setLabel("Help");
        exit.setLabel("Exit");
        popup.add(info);
        popup.add(exit);
        tray.add(trayIcon);
    }

    public static void exitApplication(int status) {
        SystemTray systemTray = SystemTray.getSystemTray();

        for (TrayIcon trayIcon : systemTray.getTrayIcons()) {
            systemTray.remove(trayIcon);
        }
        if (ui != null) {
            ui.close();
        }

        logger.info("Terminated UI.");
        sniffer.close();

        System.exit(status);
    }


    // TODO: This should be moved into its own component class
    public static void getLocalAddr() throws InterruptedException, PcapNativeException, UnknownHostException, SocketException,
            ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {

        if (settings.getBoolean("autoload")) {
            localAddr = InetAddress.getByName(settings.get("addr"));
            return;
        }

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        final JFrame frame = new JFrame("Network Device");
        frame.setFocusableWindowState(true);

        final JLabel ipLab = new JLabel("Select LAN IP obtained from Network Settings:", JLabel.LEFT);
        final JComboBox<String> lanIP = new JComboBox<>();
        final JLabel lanLabel = new JLabel("If your device IP isn't in the dropdown, provide it below.");
        final JTextField lanText = new JTextField(settings.get("addr", ""));

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
                        settings.set("addr", xAddr.getHostAddress().replaceAll("/", ""));
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
                settings.set("addr", localAddr.getHostAddress().replaceAll("/", ""));
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


    private static void fatalErrorDialog(String msg) {
        msg += "\nExiting application.";
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}
