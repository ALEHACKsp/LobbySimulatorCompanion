package net.lobby_simulator_companion.loop;

import net.lobby_simulator_companion.loop.config.AppProperties;
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

/**
 * @author PsiLupan
 * @author ShadowMoose
 * @author NickyRamone
 */
public class Boot {

    private static Logger log;
    private static MainWindow ui;


    public static void main(String[] args) {
        try {
            configureLogger();
            init();
        } catch (Exception e) {
            log.error("Failed to initialize application: {}", e.getMessage(), e);
            fatalErrorDialog("Failed to initialize application: " + e.getMessage());
            exitApplication(1);
        }
    }

    private static void configureLogger() throws URISyntaxException {
        URI execUri = FileUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path appHome = new File(execUri).toPath().getParent();
        System.setProperty("app.home", appHome.toString());
        log = LoggerFactory.getLogger(Boot.class);
    }

    private static void init() throws Exception {
        log.info("Initializing...");
        Factory.appProperties();
        initUi();

        startServices();
        Factory.pluginLoadUi().loadPlugin();
        SwingUtilities.invokeLater(() -> {
            Factory.mainWindow().start();
            log.info(Factory.appProperties().get("app.name.short") + " is ready.");
        });
    }

    private static void initUi() throws Exception {
        log.info("Starting UI...");
        SwingUtilities.invokeLater(() -> {
            ui = Factory.mainWindow();
            ui.addPropertyChangeListener(MainWindow.PROPERTY_EXIT_REQUEST, evt -> exitApplication(0));
        });
        doSanityCheck();
        setupTray();
    }

    private static void doSanityCheck() {
        boolean performSanityCheck = Factory.settings().getBoolean("loop.feature.sanity_check", true);

        if (performSanityCheck && !Sanity.check()) {
            System.exit(1);
        }
    }

    private static void startServices() throws IOException {
        Factory.loopDataService().start();
        Factory.statsPanel().refreshStatsOnScreen();
        Factory.dbdLogMonitor().start();
    }

    // TODO: separate tray icon
    private static void setupTray() throws AWTException, IOException {
        final AppProperties appProperties = Factory.appProperties();
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


    private static void exitApplication(int status) {
        SystemTray systemTray = SystemTray.getSystemTray();

        for (TrayIcon trayIcon : systemTray.getTrayIcons()) {
            systemTray.remove(trayIcon);
        }
        if (ui != null) {
            ui.close();
        }

        log.info("Terminated UI.");
        System.exit(status);
    }

    private static void fatalErrorDialog(String msg) {
        msg += "\nExiting application.";
        JOptionPane.showMessageDialog(null, msg, "Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}
