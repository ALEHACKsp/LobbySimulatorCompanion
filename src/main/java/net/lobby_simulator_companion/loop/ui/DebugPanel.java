package net.lobby_simulator_companion.loop.ui;

import net.lobby_simulator_companion.loop.service.DbdLogMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A panel for debugging purposes.
 * Simulated connections and host user discovery.
 *
 * @author NickyRamone
 */
public class DebugPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ServerPanel.class);
    private JFrame frame;
    private FileWriter logWriter;


    public DebugPanel(DbdLogMonitor logMonitor) throws Exception {
        this.logWriter = new FileWriter(logMonitor.getLogFile());
        logger.debug("Monitoring log file: {}", logMonitor.getLogFile());
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        setOpaque(true);
        frame = new JFrame("Debug Panel");

        /** Since this frame will be always on top, having the focus state in true
         * will make other windows unclickable so we need to set it to false. */
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(Color.WHITE);
        frame.setAlwaysOnTop(true);
        frame.pack();
        frame.setLocation(800, 300);
        frame.setSize(new Dimension(400, 300));
        frame.setVisible(true);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        addComponents(contentPanel);

        frame.setContentPane(contentPanel);
    }

    private void addComponents(JPanel container) {
        JButton button;

        JPanel connPanel = new JPanel();
        connPanel.setLayout(new BoxLayout(connPanel, BoxLayout.X_AXIS));
        container.add(connPanel);
        button = new JButton("Search Match");
        button.addActionListener(e -> simulateMatchSearch());
        connPanel.add(button);
        button = new JButton("Cancel Match Search");
        button.addActionListener(e -> simulateMatchSearchCancel());
        connPanel.add(button);

        JPanel lobbyPanel = new JPanel();
        lobbyPanel.setLayout(new BoxLayout(lobbyPanel, BoxLayout.X_AXIS));
        container.add(lobbyPanel);
        button = new JButton("Leave Lobby");
        button.addActionListener(e -> simulateLobbyLeave());
        lobbyPanel.add(button);

        JPanel matchPanel = new JPanel();
        matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.X_AXIS));
        container.add(matchPanel);
        button = new JButton("Generate Map");
        button.addActionListener(e -> simulateMapGeneration());
        matchPanel.add(button);
        button = new JButton("Start Match");
        button.addActionListener(e -> simulateMatchStart());
        matchPanel.add(button);
        button = new JButton("End Match");
        button.addActionListener(e -> simulateMatchEnd());
        matchPanel.add(button);

        JPanel serverPanel = new JPanel();
        serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
        container.add(serverPanel);
        button = new JButton("Connect to Server");
        button.addActionListener(e -> simulateServerConnect());
        serverPanel.add(button);
        button = new JButton("Disconnect from Server");
        button.addActionListener(e -> simulateServerDisconnect());
        serverPanel.add(button);

        JPanel killerPanel = new JPanel();
        killerPanel.setLayout(new BoxLayout(killerPanel, BoxLayout.X_AXIS));
        container.add(killerPanel);
        button = new JButton("Detect Killer Player A");
        button.addActionListener(e -> simulateNewKillerPlayer(1));
        killerPanel.add(button);
        button = new JButton("Detect Killer Player B");
        button.addActionListener(e -> simulateNewKillerPlayer(2));
        killerPanel.add(button);
    }

    private void simulateMatchSearch() {
        writeLog("--- REQUEST: [POST https://latest.live.dbd.bhvronline.com/api/v1/queue] ---");
    }

    private void simulateMatchSearchCancel() {
        writeLog("--- RESPONSE: code 200, request [POST https://latest.live.dbd.bhvronline.com/api/v1/queue/cancel] ---");
    }

    private void simulateServerConnect() {
        writeLog("--- Browse: 35.159.49.240:7779//Game/Maps/OfflineLobby?UseDedicatedServer?GameType=0?... ---");
    }

    private void simulateServerDisconnect() {
        writeLog("--- FOnlineAsyncTaskMirrorsDestroyMatch ---");
    }

    private void simulateLobbyLeave() {
        simulateMatchSearchCancel();
    }

    private void simulateMapGeneration() {
        writeLog("--- ProceduralLevelGeneration: InitLevel: Theme: Hospital Map: Hos_Treatment ---");
    }

    private void simulateMatchStart() {
        writeLog("--- //Game/Maps/ProceduralLevel ---");
    }

    private void simulateMatchEnd() {
        writeLog("--- PUT https://latest.live.dbd.bhvronline.com/api/v1/softWallet/put/analytics ---");
    }

    private void simulateNewKillerPlayer(int id) {
        if (id == 1) {
            writeLog("--- Mirrors: [FOnlineSessionMirrors::AddSessionPlayer] Session:GameSession PlayerId:ab-cd-ef-1|76561198961125794 ---");
        } else if (id == 2) {
            writeLog("--- Mirrors: [FOnlineSessionMirrors::AddSessionPlayer] Session:GameSession PlayerId:ab-cd-ef-2|76561198977148626 ---");
        }
        writeLog("--- LogCustomization: --> CA_123 ---");
    }


    private void writeLog(String s) {
        try {
            logWriter.write(s + "\n");
            logWriter.flush();
        } catch (IOException e) {
            logger.error("Failed to write to log file", e);
        }
    }

}
