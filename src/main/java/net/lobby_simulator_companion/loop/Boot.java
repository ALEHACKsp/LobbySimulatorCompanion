package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Connection;
import net.lobby_simulator_companion.loop.service.ConnectionManager;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.InvalidNetworkInterfaceException;
import net.lobby_simulator_companion.loop.service.P2pConnectionManager;
import net.lobby_simulator_companion.loop.service.SnifferListener;
import net.lobby_simulator_companion.loop.ui.MainWindow;
import net.lobby_simulator_companion.loop.ui.NetworkInterfaceFrame;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapNativeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Boot {
    private static final String SETTING__NIF_AUTOLOAD = "network.interface.autoload";
    private static final String SETTING__NIF_ADDRESS = "network.interface.address";

    private static Logger logger;
    private static MainWindow ui;
    private static DbdLogMonitor logMonitor;
    private static ConnectionManager connectionManager;
    private static Settings settings;
    private static AppProperties appProperties;


    public static void main2(String[] args) throws Exception {
        SwingUtilities.invokeLater(() -> {
            Factory.getMainWindow();
            Factory.getDebugPanel();
        });
    }

    public static void main(String[] args) throws Exception {
        configureLogger();
        try {
            init();
        } catch (Exception e) {
            logger.error("Failed to initialize application: {}", e.getMessage(), e);
            fatalErrorDialog("Failed to initialize application: " + e.getMessage());
            exitApplication(1);
        }
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
        appProperties = Factory.getAppProperties();

        System.setProperty("jna.nosys", "true");
        if (!Sanity.check()) {
            System.exit(1);
        }
        setupTray();

        logger.info("Setting up network interface...");
        InetAddress localAddr;
        if (settings.getBoolean(SETTING__NIF_AUTOLOAD, false)) {
            localAddr = InetAddress.getByName(settings.get(SETTING__NIF_ADDRESS));
        } else {
            NetworkInterfaceFrame nifFrame = new NetworkInterfaceFrame(settings);
            localAddr = nifFrame.getLocalAddr();
        }

        initLogMonitor();
        initConnectionManager(localAddr);
        initUi();
    }


    private static void initLogMonitor() {
        logger.info("Starting log monitor...");
        logMonitor = Factory.getDbdLogMonitor();
        Thread thread = new Thread(logMonitor);
        thread.setDaemon(true);
        thread.start();
    }

    private static void initConnectionManager(InetAddress localAddr) throws PcapNativeException, NotOpenException {
        if (!appProperties.getBoolean("debug")) {
            logger.info("Starting net traffic sniffer...");
            try {
                connectionManager = new P2pConnectionManager(localAddr, new SnifferListener() {
                    @Override
                    public void notifyNewConnection(Connection connection) {
                        SwingUtilities.invokeLater(() -> ui.connect());
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

            // start connection manager thread
            Thread thread = new Thread(() -> connectionManager.start());
            thread.setDaemon(true);
            thread.start();
        }
    }

    private static void initUi() {
        logger.info("Starting UI...");
        SwingUtilities.invokeLater(() -> {
            if (appProperties.getBoolean("debug")) {
                Factory.getDebugPanel();
            }
            ui = Factory.getMainWindow();
            ui.addPropertyChangeListener(ui.PROPERTY_EXIT_REQUEST, evt -> {
                exitApplication(0);
            });
            logMonitor.addObserver(ui);
            logger.info(Factory.getAppProperties().get("app.name.short") + " is ready.");
        });
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
        connectionManager.close();

        System.exit(status);
    }

    private static void fatalErrorDialog(String msg) {
        msg += "\nExiting application.";
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}
