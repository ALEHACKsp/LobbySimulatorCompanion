package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Server;
import net.lobby_simulator_companion.loop.repository.ServerDao;
import net.lobby_simulator_companion.loop.service.LoopDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author NickyRamone
 */
public class ServerPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";
    private static final Font font = ResourceFactory.getRobotoFont();
    private static final Logger logger = LoggerFactory.getLogger(ServerPanel.class);

    private final Settings settings;
    private final AppProperties appProperties;
    private final LoopDataService dataService;
    private final ServerDao serverDao;

    private JPanel titleBar;
    private JLabel summaryLabel;
    private JLabel geoLocationLabel;
    private JPanel detailsPanel;
    private JLabel detailsCollapseButton;
    private JLabel countryValueLabel;
    private JLabel regionValueLabel;
    private JLabel cityValueLabel;
    private JLabel serverIdValueLabel;

    private Server server;


    public ServerPanel(Settings settings, AppProperties appProperties, LoopDataService dataService, ServerDao serverDao) {
        this.settings = settings;
        this.appProperties = appProperties;
        this.dataService = dataService;
        this.serverDao = serverDao;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0, 0, 250));
        titleBar = createTitleBar();
        detailsPanel = createDetailsPanel();

        add(titleBar);
        add(detailsPanel);
    }


    private JPanel createTitleBar() {

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel serverLabel = new JLabel("Server:");
        serverLabel.setBorder(border);
        serverLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        serverLabel.setFont(font);

        summaryLabel = new JLabel();
        summaryLabel.setBorder(border);
        summaryLabel.setFont(font);

        geoLocationLabel = new JLabel();
        geoLocationLabel.setVisible(false);
        geoLocationLabel.setBorder(border);
        geoLocationLabel.setIcon(ResourceFactory.getGeoLocationIcon());
        geoLocationLabel.setToolTipText("Click to visit this location in Google Maps on your browser.");
        geoLocationLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (server != null) {
                    try {
                        String profileUrl = String.format(appProperties.get("google.maps.geolocation.url_template"),
                                server.getLatitude(), server.getLongitude());
                        Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                    } catch (IOException e1) {
                        logger.error("Failed to open browser at Google Maps.");
                    } catch (URISyntaxException e1) {
                        logger.error("Attempted to use an invalid URL for Google Maps.");
                    }
                }
            }
        });


        detailsCollapseButton = new JLabel();
        detailsCollapseButton.setIcon(ResourceFactory.getCollapseIcon());

        detailsCollapseButton.setBorder(border);
        detailsCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailsPanel.setVisible(!detailsPanel.isVisible());
            }
        });

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(serverLabel);
        container.add(summaryLabel);
        container.add(geoLocationLabel);
        container.add(Box.createHorizontalGlue());
        container.add(detailsCollapseButton);

        return container;
    }

    private JPanel createDetailsPanel() {
        JPanel container = new JPanel();
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new GridLayout(0, 2, 10, 5));
        container.setMaximumSize(new Dimension(500, 100));

        JLabel countryLabel = new JLabel("Country:", JLabel.RIGHT);
        countryLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        countryLabel.setFont(font);
        countryValueLabel = new JLabel();
        countryValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        countryValueLabel.setFont(font);

        JLabel regionLabel = new JLabel("Region:", JLabel.RIGHT);
        regionLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        regionLabel.setFont(font);
        regionValueLabel = new JLabel();
        regionValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        regionValueLabel.setFont(font);

        JLabel cityLabel = new JLabel("City:", JLabel.RIGHT);
        cityLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        cityLabel.setFont(font);
        cityValueLabel = new JLabel();
        cityValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        cityValueLabel.setFont(font);

        JLabel serverIdLabel = new JLabel("Encountered server #:", JLabel.RIGHT);
        serverIdLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        serverIdLabel.setFont(font);
        serverIdValueLabel = new JLabel();
        serverIdValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        serverIdValueLabel.setFont(font);

        container.add(countryLabel);
        container.add(countryValueLabel);
        container.add(regionLabel);
        container.add(regionValueLabel);
        container.add(cityLabel);
        container.add(cityValueLabel);
        container.add(serverIdLabel);
        container.add(serverIdValueLabel);
        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                detailsCollapseButton.setIcon(ResourceFactory.getCollapseIcon());
                super.componentShown(e);
                settings.set("ui.panel.server.collapsed", false);
                firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                detailsCollapseButton.setIcon(ResourceFactory.getExpandIcon());
                super.componentHidden(e);
                settings.set("ui.panel.server.collapsed", true);
                firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null);
            }
        });
        container.setVisible(!settings.getBoolean("ui.panel.server.collapsed"));

        return container;
    }

    public void updateServerIpAddress(String ipAddress) {
        new Thread(() -> {
            try {
                server = dataService.getServerByIpAddress(ipAddress);
                if (server == null) {
                    // it's not cached
                    server = serverDao.getByIpAddress(ipAddress);
                    dataService.addServer(server);
                }

                SwingUtilities.invokeLater(() -> updateServerPanel(this.server));

            } catch (IOException e) {
                logger.error("Failed to retrieve server information.", e);
            }
        }).start();
    }

    public void updateServerPanel(Server server) {
        clearServer();
        this.server = server;
        summaryLabel.setText(String.format("%s, %s [#%d]", server.getCity(), server.getCountry(), server.getDiscoveryNumber()));
        if (server.getLatitude() != null && server.getLongitude() != null) {
            geoLocationLabel.setVisible(true);
        }
        countryValueLabel.setText(server.getCountry());
        regionValueLabel.setText(server.getRegion());
        cityValueLabel.setText(server.getCity());
        serverIdValueLabel.setText(server.getDiscoveryNumber() != null ?
                String.valueOf(server.getDiscoveryNumber()) : null);
    }

    public void clearServer() {
        server = null;
        summaryLabel.setText(null);
        geoLocationLabel.setVisible(false);
        countryValueLabel.setText(null);
        regionValueLabel.setText(null);
        cityValueLabel.setText(null);
        serverIdValueLabel.setText(null);
    }


}
