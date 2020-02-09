package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Killer;
import net.lobby_simulator_companion.loop.domain.RealmMap;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.ui.MainWindow;
import net.lobby_simulator_companion.loop.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class Boot {
    private static Logger logger;
    private static MainWindow ui;
    private static DbdLogMonitor logMonitor;
    private static Settings settings;
    private static AppProperties appProperties;


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
        boolean performSanityCheck = Factory.getSettings().getBoolean("loop.feature.sanity_check", true);

        if (performSanityCheck && !Sanity.check()) {
            System.exit(1);
        }
        setupTray();

        initLogMonitor();
        initUi();
        logger.info("Supported game killers: {}", Killer.values().length - 1);
        logger.info("Supported game maps: {}", RealmMap.values().length - 1);
    }


    private static void initLogMonitor() {
        logger.info("Starting log monitor...");
        logMonitor = Factory.getDbdLogMonitor();
        Thread thread = new Thread(logMonitor);
        thread.setDaemon(true);
        thread.start();
    }

    private static void initUi() {
        logger.info("Starting UI...");
        SwingUtilities.invokeLater(() -> {
            if (appProperties.getBoolean("debug")) {
                Factory.getDebugPanel();
            }
            ui = Factory.getMainWindow();
            ui.addPropertyChangeListener(MainWindow.PROPERTY_EXIT_REQUEST, evt -> exitApplication(0));
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
                    + "Credits:\n"
                    + "=======\n"
                    + "Author: NickyRamone\n"
                    + "Based on the MLGA project, by PsiLupan & ShadowMoose";

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
        System.exit(status);
    }

    private static void fatalErrorDialog(String msg) {
        msg += "\nExiting application.";
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}
