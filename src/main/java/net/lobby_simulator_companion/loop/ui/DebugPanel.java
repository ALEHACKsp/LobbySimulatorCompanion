package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.domain.Server;
import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import net.lobby_simulator_companion.loop.service.PlayerDto;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

/**
 * A panel for debugging purposes.
 * Simulated connections and host user discovery.
 *
 * @author NickyRamone
 */
public class DebugPanel extends JPanel {

    private Set<Integer> connections = new HashSet<>();

    private MainWindow mainPanel;
    private DbdLogMonitor logMonitor;
    private JFrame frame;


    public DebugPanel(MainWindow mainPanel, DbdLogMonitor logMonitor) throws Exception {
        this.mainPanel = mainPanel;
        this.logMonitor = logMonitor;
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        setOpaque(true);
        frame = new JFrame("Debug Panel");

        /** Since this frame will be always on top, having the focus state in true
         * will make other windows unclickable so we need to set it to false. */
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.WHITE);
        frame.setLayout(new GridLayout(0, 1, 10, 10));
        frame.setAlwaysOnTop(true);
        addComponents();
        frame.pack();
        frame.setLocation(800, 300);
        frame.setVisible(true);
    }

    private void addComponents() {
        JButton button = new JButton("Reset");
        button.addActionListener(e -> reset());
        frame.add(button);

        JPanel connectionContainer = new JPanel();
        connectionContainer.setLayout(new BoxLayout(connectionContainer, BoxLayout.X_AXIS));
        button = new JButton("Connect");
        button.addActionListener(e -> mainPanel.connectToMatch("1.2.3.4"));
        connectionContainer.add(button);

        button = new JButton("Disconnect");
        button.addActionListener(e -> mainPanel.disconnectFromMatch());
        connectionContainer.add(button);

        frame.add(connectionContainer);


        button = new JButton("update server");
        button.addActionListener(e -> mainPanel.updateServer(generateRandomServer()));
        frame.add(button);

        JPanel userContainer = new JPanel();
        userContainer.setLayout(new GridLayout(0, 1, 10, 10));
        button = new JButton("Detect killer A");
        button.addActionListener(e -> simulateKillerUserUpdate());
        userContainer.add(button);

        button = new JButton("Detect killer B");
        button.addActionListener(e -> simulateKillerUserUpdate());
        userContainer.add(button);
        frame.add(userContainer);

        button = new JButton("Detect killer character A");
        button.addActionListener(e -> simulateKillerCharUpdate());
        userContainer.add(button);
        frame.add(button);
    }

    private void simulateKillerCharUpdate() {
        String character = "Hillbilly";
        DbdLogMonitor.Event event = new DbdLogMonitor.Event(DbdLogMonitor.Event.Type.KILLER_CHARACTER, character);
        mainPanel.update(logMonitor, event);
    }

    private Server generateRandomServer() {
        Server server = new Server("0.0.0.0");
        server.setCountry("some country");
        server.setRegion("some region");
        server.setCity("some city");
        server.setDiscoveryNumber(10);

        return server;
    }

    private void reset() {
        connections.clear();
    }

    private void simulateKillerUserUpdate() {
        PlayerDto playerDto = new PlayerDto("76561198961125794", "ab-cd-ef");
        DbdLogMonitor.Event event = new DbdLogMonitor.Event(DbdLogMonitor.Event.Type.KILLER_PLAYER, playerDto);
        mainPanel.update(logMonitor, event);
    }

}
