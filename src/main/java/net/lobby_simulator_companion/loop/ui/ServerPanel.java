package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.Server;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 *
 * @author Nicky Ramone
 */
public class ServerPanel extends JPanel {

    private static final Font font = ResourceFactory.getRobotoFont();

    private Window window;
    private JPanel summaryBar;
    private JPanel detailsPanel;
    private JLabel detailsCollapseButton;
    private JLabel countryValueLabel;
    private JLabel regionValueLabel;
    private JLabel cityValueLabel;
    private JLabel serverIdValueLabel;


    public ServerPanel(Window window) {
        this.window = window;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0, 0, 250));
        summaryBar = createSummaryBar();
        detailsPanel = createDetailsPanel();

        add(summaryBar);
        add(detailsPanel);
    }


    private JPanel createSummaryBar() {
        JPanel container = new JPanel();
        container.setPreferredSize(new Dimension(200, 25));
        container.setMinimumSize(new Dimension(300, 25));
        container.setBackground(Colors.STATUS_BAR_BACKGROUND);
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));

        Border border = new EmptyBorder(5, 5, 5, 5);

        JLabel serverLabel = new JLabel("Server:");
        serverLabel.setBorder(border);
        serverLabel.setForeground(Colors.STATUS_BAR_FOREGROUND);
        serverLabel.setFont(font);

        JLabel summaryLabel = new JLabel("N/A");
        summaryLabel.setBorder(border);
        summaryLabel.setFont(font);

        detailsCollapseButton = new JLabel();
        detailsCollapseButton.setIcon(ResourceFactory.getCollapseIcon());

        detailsCollapseButton.setBorder(border);
        detailsCollapseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                detailsPanel.setVisible(!detailsPanel.isVisible());
            }
        });

        container.add(serverLabel);
        container.add(summaryLabel);
        container.add(Box.createHorizontalGlue());
        container.add(detailsCollapseButton);

        return container;
    }

    private JPanel createDetailsPanel() {
        JPanel container = new JPanel();
        container.setBackground(Colors.INFO_PANEL_BACKGROUND);
        container.setLayout(new GridLayout(4, 2, 10, 10));
        container.setMaximumSize(new Dimension(500, 100));

        JLabel countryLabel = new JLabel("Country:", JLabel.RIGHT);
        countryLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        countryLabel.setFont(font);
        countryValueLabel = new JLabel("dummy_country");
        countryValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        countryValueLabel.setFont(font);

        JLabel regionLabel = new JLabel("Region:", JLabel.RIGHT);
        regionLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        regionLabel.setFont(font);
        regionValueLabel = new JLabel("dummy_region");
        regionValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        regionValueLabel.setFont(font);

        JLabel cityLabel = new JLabel("City:", JLabel.RIGHT);
        cityLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        cityLabel.setFont(font);
        cityValueLabel = new JLabel("dummy_city");
        cityValueLabel.setForeground(Colors.INFO_PANEL_VALUE_FOREGOUND);
        cityValueLabel.setFont(font);

        JLabel serverIdLabel = new JLabel("Encountered server #:", JLabel.RIGHT);
        serverIdLabel.setForeground(Colors.INFO_PANEL_NAME_FOREGROUND);
        serverIdLabel.setFont(font);
        serverIdValueLabel = new JLabel("5");
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
                window.pack();
                super.componentShown(e);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                detailsCollapseButton.setIcon(ResourceFactory.getExpandIcon());
                window.pack();
                super.componentHidden(e);
            }
        });

        return container;
    }

    public void clearServer() {
        updateServer(null);
    }

    public void updateServer(Server server) {
        if (server == null) {
            server = new Server();
        }

        countryValueLabel.setText(server.getCountry());
        regionValueLabel.setText(server.getRegion());
        cityValueLabel.setText(server.getCity());
        serverIdValueLabel.setText(server.getDiscoveryNumber() != null ? String.valueOf(server.getDiscoveryNumber()) : "");
    }

}
