package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.config.AppProperties;
import net.lobby_simulator_companion.loop.config.Settings;
import net.lobby_simulator_companion.loop.domain.Server;
import net.lobby_simulator_companion.loop.repository.ServerDao;
import net.lobby_simulator_companion.loop.ui.common.CollapsablePanel;
import net.lobby_simulator_companion.loop.ui.common.Colors;
import net.lobby_simulator_companion.loop.ui.common.ComponentUtils;
import net.lobby_simulator_companion.loop.ui.common.NameValueInfoPanel;
import net.lobby_simulator_companion.loop.ui.common.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

import static net.lobby_simulator_companion.loop.ui.common.ResourceFactory.Icon;

/**
 * @author NickyRamone
 */
public class ServerPanel extends JPanel {

    public static final String EVENT_STRUCTURE_CHANGED = "structure_changed";
    private static final Font font = ResourceFactory.getRobotoFont();
    private static final Logger logger = LoggerFactory.getLogger(ServerPanel.class);

    private enum InfoType {
        COUNTRY("Country"),
        REGION("Region"),
        CITY("City"),
        PROVIDER("Provider");

        String description;

        InfoType(String description) {
            this.description = description;
        }
    }

    private final Settings settings;
    private final AppProperties appProperties;
    private final ServerDao serverDao;

    private JLabel summaryLabel;
    private JLabel geoLocationLabel;
    private NameValueInfoPanel<InfoType> detailsPanel;
    private Server server;


    public ServerPanel(Settings settings, AppProperties appProperties, ServerDao serverDao) {
        this.settings = settings;
        this.appProperties = appProperties;
        this.serverDao = serverDao;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        detailsPanel = createDetailsPanel();

        JPanel collapsablePanel = new CollapsablePanel(
                createTitleBar(),
                detailsPanel,
                settings, "ui.panel.server.collapsed");
        collapsablePanel.addPropertyChangeListener(evt -> firePropertyChange(EVENT_STRUCTURE_CHANGED, null, null));
        add(collapsablePanel);
    }


    private JPanel createTitleBar() {
        JLabel serverLabel = new JLabel("Server:");
        serverLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        serverLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        serverLabel.setFont(font);

        summaryLabel = new JLabel();
        summaryLabel.setBorder(ComponentUtils.DEFAULT_BORDER);
        summaryLabel.setFont(font);

        geoLocationLabel = ComponentUtils.createButtonLabel(
                null,
                "Click to visit this location in Google Maps on your browser.",
                Icon.GEO_LOCATION,
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        if (server != null) {
                            try {
                                // we need to provide US locale so that the formatter uses "." as a decimal separator
                                String profileUrl = String.format(appProperties.get("google.maps.geolocation.url_template"),
                                        server.getLatitude(), server.getLongitude(), Locale.US);
                                Desktop.getDesktop().browse(new URL(profileUrl).toURI());
                            } catch (IOException e1) {
                                logger.error("Failed to open browser at Google Maps.");
                            } catch (URISyntaxException e1) {
                                logger.error("Attempted to use an invalid URL for Google Maps.");
                            }
                        }
                    }
                });
        geoLocationLabel.setVisible(false);

        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.add(serverLabel);
        container.add(summaryLabel);
        container.add(geoLocationLabel);
        container.add(Box.createHorizontalGlue());

        return container;
    }

    private NameValueInfoPanel createDetailsPanel() {
        NameValueInfoPanel.Builder<InfoType> builder = new NameValueInfoPanel.Builder<>();
        for (InfoType infoType : InfoType.values()) {
            builder.addField(infoType, infoType.description + ":");
        }

        return builder.build();
    }

    public void updateServerIpAddress(String ipAddress) {
        new Thread(() -> {
            try {
                server = serverDao.getByIpAddress(ipAddress);
                SwingUtilities.invokeLater(() -> updateServerPanel(this.server));

            } catch (IOException e) {
                logger.error("Failed to retrieve server information.", e);
            }
        }).start();
    }

    public void updateServerPanel(Server server) {
        clearServer();
        this.server = server;
        summaryLabel.setText(String.format("%s, %s", server.getCity(), server.getCountry()));
        if (server.getLatitude() != null && server.getLongitude() != null) {
            geoLocationLabel.setVisible(true);
        }

        setServerValue(InfoType.COUNTRY, server.getCountry());
        setServerValue(InfoType.REGION, server.getRegion());
        setServerValue(InfoType.CITY, server.getCity());
        setServerValue(InfoType.PROVIDER, server.getIsp());
    }

    public void clearServer() {
        server = null;
        summaryLabel.setText(null);
        geoLocationLabel.setVisible(false);
        setServerValue(InfoType.COUNTRY, null);
        setServerValue(InfoType.REGION, null);
        setServerValue(InfoType.CITY, null);
        setServerValue(InfoType.PROVIDER, null);
    }

    private void setServerValue(InfoType type, String value) {
        detailsPanel.get(type).setText(value);
    }

}
